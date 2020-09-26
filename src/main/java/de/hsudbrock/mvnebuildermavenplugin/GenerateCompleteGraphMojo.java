package de.hsudbrock.mvnebuildermavenplugin;

import static de.hsudbrock.mvnebuildermavenplugin.graph.DependencyType.UNKNOWN_DEP;
import static freemarker.template.Configuration.VERSION_2_3_30;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.aether.util.graph.transformer.ConflictResolver.CONFIG_PROP_VERBOSE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.guava.MutableValueGraphAdapter;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import de.hsudbrock.mvnebuildermavenplugin.graph.BuildDependencyGraphVisitor;
import de.hsudbrock.mvnebuildermavenplugin.graph.DependencyGraphEdge;
import de.hsudbrock.mvnebuildermavenplugin.graph.DependencyGraphNode;
import de.hsudbrock.mvnebuildermavenplugin.graph.DependencyType;
import de.hsudbrock.mvnebuildermavenplugin.graph.EdgeTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "generate-complete-graph", defaultPhase = LifecyclePhase.NONE, requiresProject = false, requiresDirectInvocation = true)
public class GenerateCompleteGraphMojo extends AbstractMojo {

	@Parameter(property = "groupId", required = true)
	private String groupId;

	@Parameter(property = "artifactId", required = true)
	private String artifactId;

	@Parameter(property = "version", required = true)
	private String version;

	@Parameter(property = "type", defaultValue = "jar")
	private String type;

	@Parameter(property = "classifier", defaultValue = "")
	private String classifier;

	@Parameter(property = "profiles")
	private List<String> profiles;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Parameter(property = "outputFileName", defaultValue = "graph.dot")
	private String outputFileName;

	@Component
	private ProjectBuilder projectBuilder;

	@Component
	private ProjectDependenciesResolver dependenciesResolver;

	@Component
	private LifecycleExecutor lifecycleExecutor;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph = ValueGraphBuilder.directed()
					.allowsSelfLoops(true).build();
			graph.addNode(new DependencyGraphNode(
					getProject(this.groupId, this.artifactId, this.version, "compile", this.type, this.classifier), "jar"));

			boolean keepGoing = true;
			while (keepGoing) {
				// Get nodes that still need to be analyzed
				Set<DependencyGraphNode> remainingNodes = graph.nodes().stream()
						.filter(node -> !node.isAsProject() && !node.isOptional() && !node.getType().equals("pom"))
						.collect(toSet());

				if (remainingNodes.isEmpty()) {
					keepGoing = false;
				} else {
					for (DependencyGraphNode node : remainingNodes) {
						MavenProject nodeProject = getProject(node);
						if (nodeProject != null) {
							buildDependencyGraph(nodeProject, graph);
						} else {
							node.setAsProject(true);
							node.setProjectFailure(true);
						}
					}
				}
			}
			
			removeCycles(graph);

			writeGraph(graph);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new MojoFailureException("something went terribly wrong", t);
		}
	}

	private void removeCycles(MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) {
		MutableValueGraphAdapter<DependencyGraphNode, DependencyGraphEdge> graphAdapter = new MutableValueGraphAdapter<DependencyGraphNode, DependencyGraphEdge>(graph, new DependencyGraphEdge(UNKNOWN_DEP), edge -> 0.0);
		TarjanSimpleCycles<DependencyGraphNode, EndpointPair<DependencyGraphNode>> tarjanSimpleCycles = new TarjanSimpleCycles<>(graphAdapter);
		List<List<DependencyGraphNode>> findSimpleCycles = tarjanSimpleCycles.findSimpleCycles();
		for (List<DependencyGraphNode> cycle: findSimpleCycles) {
			System.out.println("Cycle:");
			System.out.println(cycle.stream().map(node -> node.toString()).collect(Collectors.joining(" - ")));
			System.out.println("------");
		}
	}

	public MavenProject getProject(String groupId, String artifactId, String version, String scope, String type,
			String classifier) {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
				this.session.getProjectBuildingRequest());
		buildingRequest.setRepositorySession(this.session.getRepositorySession());
		buildingRequest.setProject(null);
		buildingRequest.setResolveDependencies(true);
		buildingRequest.setActiveProfileIds(this.profiles);

		try {
			DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, version, scope, type, classifier,
					new DefaultArtifactHandler());
			return this.projectBuilder.build(artifact, buildingRequest).getProject();
		} catch (Throwable t) {
			getLog().warn("Could not create project for " + groupId + ":" + artifactId + ":" + version + "; skipping");
			return null;
		}

	}

	private Set<String> projectGavs = new HashSet<>();

	public void buildDependencyGraph(MavenProject project,
			MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) throws MojoExecutionException {

		getLog().info("Working on project: " + getGav(project));
		if (projectGavs.contains(getGav(project))) {
			getLog().error("Oh no, I ran into a cycle for " + getGav(project));
			throw new RuntimeException("Cycle");
		}
		projectGavs.add(getGav(project));

		// Add root node
		DependencyGraphNode rootNode = new DependencyGraphNode(project, "jar");
		Optional<DependencyGraphNode> existingNode = graph.nodes().stream()
				.filter(it -> it.equals(new DependencyGraphNode(project, "jar"))).findFirst();
		if (existingNode.isPresent()) {
			rootNode = existingNode.get();
		} else {
			graph.addNode(rootNode);
		}

		rootNode.setAsProject(true);

		// Add all parent poms
		addParentsFor(project, graph, rootNode);

		// Add "normal" dependencies (i.e., not build or reporting)
		// No need to pass in root node, as it is found again
		addDependenciesFor(project, graph);

		// Add "build plugin" dependencies
		addBuildPluginDependenciesFor(project, graph, rootNode);

		// Add "report plugin" dependencies
		// Skip this for now, not super-essential for ebuild for now
		// addReportDependenciesFor(project, graph, rootNode);
	}

	private String getGav(MavenProject project) {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
	}

	private MavenProject getProject(DependencyGraphNode node) {
		return getProject(node.getGroupId(), node.getArtifactId(), node.getVersion(), "compile", node.getType(),
				node.getClassifier());
	}

	private void addBuildPluginDependenciesFor(MavenProject project,
			MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph, DependencyGraphNode parentNode) {
		List<Plugin> buildPlugins = project.getBuildPlugins();

		MavenExecutionPlan executionPlan = null;
		try {
			executionPlan = lifecycleExecutor.calculateExecutionPlan(session, "package");
		} catch (PluginNotFoundException | PluginResolutionException | PluginDescriptorParsingException
				| MojoNotFoundException | NoPluginFoundForPrefixException | InvalidPluginDescriptorException
				| PluginManagerException | LifecyclePhaseNotFoundException | LifecycleNotFoundException
				| PluginVersionResolutionException e) {
			getLog().warn("Could not calculate execution plan for " + getGav(project) + "; skipping");
			return;
		}

		for (Plugin plugin : buildPlugins) {
			if (isRelevant(plugin, executionPlan)) {
				DependencyGraphNode pluginNode = new DependencyGraphNode(plugin);
				graph.addNode(pluginNode);
				graph.putEdgeValue(parentNode, pluginNode, new DependencyGraphEdge(DependencyType.PLUGIN_DEP));
			}
		}
	}

	private boolean isRelevant(Plugin plugin, MavenExecutionPlan executionPlan) {
		List<String> executions = getExecutions(plugin, executionPlan);
		return (executions.contains("process-resources") || executions.contains("compile")
				|| executions.contains("package"));
	}

	private List<String> getExecutions(Plugin plugin, MavenExecutionPlan executionPlan) {
		return executionPlan.getMojoExecutions().stream().filter(execution -> execution.getPlugin().equals(plugin))
				.map(execution -> execution.getLifecyclePhase()).collect(toList());
	}

	private void addParentsFor(MavenProject project, MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph,
			DependencyGraphNode node) {
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			DependencyGraphNode parentNode = new DependencyGraphNode(parentProject, "pom");
			graph.addNode(parentNode);
			graph.putEdgeValue(node, parentNode, new DependencyGraphEdge(DependencyType.PARENT));

			addParentsFor(parentProject, graph, node);
		}
	}

	private void addDependenciesFor(MavenProject project,
			MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) throws MojoExecutionException {
		DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
		request.setResolutionFilter(
				(node, parents) -> node.getDependency() == null || !node.getDependency().isOptional());
		request.setMavenProject(project);
		request.setRepositorySession(getVerboseRepositorySession(project));

		DependencyResolutionResult result = null;
		try {
			result = this.dependenciesResolver.resolve(request);
		} catch (Throwable t) {
			getLog().warn("Could not resolve dependencies for " + project.getGroupId() + ":" + project.getArtifactId()
					+ ":" + project.getVersion() + "; skipping");
		}

		if (result != null) {
			org.eclipse.aether.graph.DependencyNode root = result.getDependencyGraph();
			BuildDependencyGraphVisitor visitor = new BuildDependencyGraphVisitor(graph);
			root.accept(visitor);
		}
	}

	private static RepositorySystemSession getVerboseRepositorySession(MavenProject project) {
		RepositorySystemSession repositorySession = project.getProjectBuildingRequest().getRepositorySession();
		DefaultRepositorySystemSession verboseRepositorySession = new DefaultRepositorySystemSession(repositorySession);
		verboseRepositorySession.setConfigProperty(CONFIG_PROP_VERBOSE, "true");
		verboseRepositorySession.setReadOnly();
		repositorySession = verboseRepositorySession;
		return repositorySession;
	}

	private void writeGraph(MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph)
			throws MojoExecutionException {
		Configuration freemarkerConfig = createFreemarkerConfig();

		Template graphTemplate;
		try {
			graphTemplate = freemarkerConfig.getTemplate("depgraph.ftl");
		} catch (IOException e) {
			throw new MojoExecutionException("Could not load template depgraph.ftl", e);
		}

		Path targetFile = Paths.get(System.getProperty("user.dir"), outputFileName);

		try (Writer out = new FileWriter(targetFile.toFile())) {
			graphTemplate.process(createGraphModel(graph), out);
		} catch (TemplateException | IOException e) {
			throw new MojoExecutionException("Could not render the graph file", e);
		}
	}

	private Map<String, Object> createGraphModel(MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) {
		Map<String, Object> model = new HashMap<>();
		model.put("nodes", graph.nodes());
		model.put("edges", graph.edges().stream().map(it -> createEdgeTemplateModel(it, graph)).collect(toSet()));
		return model;
	}

	private EdgeTemplateModel createEdgeTemplateModel(EndpointPair<DependencyGraphNode> endpointPair,
			MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) {
		return new EdgeTemplateModel(endpointPair.nodeU(), endpointPair.nodeV(), graph.edgeValue(endpointPair));
	}

	private Configuration createFreemarkerConfig() {
		Configuration config = new Configuration(VERSION_2_3_30);
		config.setDefaultEncoding("UTF-8");
		config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		config.setClassForTemplateLoading(this.getClass(), "/");
		return config;
	}

}
