package de.hsudbrock.mvnebuildermavenplugin;

import static freemarker.template.Configuration.VERSION_2_3_30;
import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "generate-binary-ebuild-for-artifact", defaultPhase = LifecyclePhase.NONE, requiresProject = false, requiresDirectInvocation = true)
public class GenerateBinaryEbuildForArtifactMojo extends AbstractMojo {

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Parameter(property = "profiles")
	private List<String> profiles;

	@Parameter(property = "groupId", required = true)
	private String groupId;

	@Parameter(property = "artifactId", required = true)
	private String artifactId;

	@Parameter(property = "version", required = true)
	private String version;

	@Parameter(property = "classifier", defaultValue = "")
	private String classifier;

	@Parameter(defaultValue = "https://repo1.maven.org/maven2")
	private String mavenRepoBase;

	@Parameter(property = "pomCategory", defaultValue = "dev-java")
	private String pomCategory;

	@Parameter(property = "repoTarget", defaultValue = "${project.build.directory}/portage-repo")
	private String repoTarget;

	@Parameter(property = "maintainerEmail", defaultValue = "gentoo@hsudbrock.de")
	private String maintainerEmail;

	@Component
	private ProjectBuilder projectBuilder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		MavenProject project = getProject();

		Configuration freemarkerConfig = createFreemarkerConfig();

		Template binEbuildTemplate;
		Template metadataTemplate;
		try {
			binEbuildTemplate = freemarkerConfig.getTemplate("bin-ebuild.ftl");
			metadataTemplate = freemarkerConfig.getTemplate("metadata.xml.ftl");
		} catch (IOException e) {
			throw new MojoFailureException("Could not load template for bin ebuilds", e);
		}

		Path targetFolder = Paths.get(repoTarget, pomCategory);
		try {
			Files.createDirectories(targetFolder);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create target folder", e);
		}

		Path ebuildFolder = targetFolder.resolve(getFolderName(project));
		try {
			Files.createDirectories(ebuildFolder);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create the folder" + ebuildFolder, e);
		}

		Path ebuildFile = ebuildFolder.resolve(getEbuildName(project) + ".ebuild");
		try (Writer out = new FileWriter(ebuildFile.toFile())) {
			binEbuildTemplate.process(createEbuildModel(project), out);
		} catch (TemplateException | IOException | LicenseNotFoundException e) {
			throw new MojoFailureException("Could not render the bin ebuild", e);
		}

		Path metadataFile = ebuildFolder.resolve("metadata.xml");
		try (Writer out = new FileWriter(metadataFile.toFile())) {
			metadataTemplate.process(createMetadataModel(project), out);
		} catch (TemplateException | IOException | LicenseNotFoundException e) {
			throw new MojoFailureException(
					"Could not render the metadata file for parent project " + project.toString(), e);
		}
	}

	private String getFolderName(MavenProject project) {
		return project.getArtifactId() + "-bin";
	}

	private String getEbuildName(MavenProject project) {
		return project.getArtifactId() + "-bin-" + project.getVersion();
	}

	private Configuration createFreemarkerConfig() {
		Configuration config = new Configuration(VERSION_2_3_30);
		config.setDefaultEncoding("UTF-8");
		config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		config.setClassForTemplateLoading(this.getClass(), "/");
		return config;
	}

	private Map<String, Object> createEbuildModel(MavenProject project)
			throws LicenseNotFoundException, MojoFailureException {
		Map<String, Object> model = new HashMap<>();
		model.put("groupId", project.getGroupId());
		model.put("artifactId", project.getArtifactId());
		model.put("version", project.getVersion());

		model.put("description", limitStringTo(65, project.getDescription().replace('\n', ' ')));
		model.put("homepage", project.getUrl());

		List<String> licenses = getLicenses(project);
		if (licenses.size() > 1) {
			throw new MojoFailureException("No support for projects with multiple licenses currently, failure for pom "
					+ project.getArtifactId());
		} else if (licenses.size() == 1) {
			model.put("license", licenses.get(0));
		} else {
			model.put("license", "TODO: No license provided by POM");
		}

		model.put("jar_src_uri", mavenRepoBase + getJarPath(project));
		model.put("pom_src_uri", mavenRepoBase + getPomPath(project));
		
		List<String> buildDependencies = new ArrayList<String>();
		if (project.getParent() != null) {
			buildDependencies.add(pomCategory + "/" + project.getParent().getArtifactId() + "-pom:" + project.getParent().getVersion());
		}
		model.put("buildDependencies", buildDependencies);

		return model;
	}

	private Map<String, Object> createMetadataModel(MavenProject project)
			throws LicenseNotFoundException, MojoFailureException {
		Map<String, Object> model = new HashMap<>();
		model.put("maintainerEmail", maintainerEmail);
		return model;
	}

	private Object limitStringTo(int maxLength, String s) {
		if (s.length() < maxLength) {
			return s;
		} else {
			return s.substring(0, maxLength);
		}
	}

	private List<String> getLicenses(MavenProject project) throws LicenseNotFoundException {
		List<String> result = new ArrayList<>();
		for (License license : project.getLicenses()) {
			result.add(getGentooLicense(license));
		}
		return result;
	}

	private String getGentooLicense(License license) throws LicenseNotFoundException {
		String gentooLicense = new LicenseMap().getGentooLicenseString(license.getName());
		if (gentooLicense != null) {
			return gentooLicense;
		} else {
			throw new LicenseNotFoundException("License '" + license.getName() + "' unknown");
		}
	}

	private String getJarPath(MavenProject project) {
		return "/" + project.getGroupId().replace('.', '/') + "/" + project.getArtifactId() + "/" + project.getVersion()
				+ "/" + project.getArtifactId() + "-" + project.getVersion() + ".jar";
	}
	
	private String getPomPath(MavenProject project) {
		return "/" + project.getGroupId().replace('.', '/') + "/" + project.getArtifactId() + "/" + project.getVersion()
				+ "/" + project.getArtifactId() + "-" + project.getVersion() + ".pom";
	}

	public MavenProject getProject() {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
				this.session.getProjectBuildingRequest());
		buildingRequest.setRepositorySession(this.session.getRepositorySession());
		buildingRequest.setProject(null);
		buildingRequest.setResolveDependencies(true);
		buildingRequest.setActiveProfileIds(this.profiles);

		DefaultArtifact artifact = new DefaultArtifact(this.groupId, this.artifactId, this.version, SCOPE_COMPILE,
				"jar", this.classifier, new DefaultArtifactHandler());
		try {
			return this.projectBuilder.build(artifact, buildingRequest).getProject();
		} catch (ProjectBuildingException e) {
			throw new IllegalStateException("Error while creating Maven project from Artifact '" + artifact + "'.", e);
		}

	}

}
