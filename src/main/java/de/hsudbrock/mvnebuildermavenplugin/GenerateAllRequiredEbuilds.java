package de.hsudbrock.mvnebuildermavenplugin;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-all-required-ebuilds")
public class GenerateAllRequiredEbuilds extends AbstractMojo {
	
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Set<Artifact> allRequiredArtifacts = new HashSet<>();
		
		addParentArtifacts(project, allRequiredArtifacts);
		
		for (Plugin plugin: project.getBuildPlugins()) {
			addBuildArtifacts(plugin, allRequiredArtifacts);
		}
		
		for (Artifact artifact: allRequiredArtifacts) {
			getLog().info(artifact.toString());
		}
	}

	private void addParentArtifacts(MavenProject mavenProject, Set<Artifact> allRequiredArtifacts) {
		if (mavenProject.hasParent()) {
			MavenProject parentProject = mavenProject.getParent();
			Artifact parentPomArtifact = new Artifact(parentProject.getGroupId(), parentProject.getArtifactId(), parentProject.getVersion(), "pom");
			if (!allRequiredArtifacts.contains(parentPomArtifact)) {
				allRequiredArtifacts.add(parentPomArtifact);
				addParentArtifacts(parentProject, allRequiredArtifacts);
			}
		}
	}
	
	private void addBuildArtifacts(Plugin plugin, Set<Artifact> allRequiredArtifacts) {
		Artifact pluginArtifact = new Artifact(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), "jar");
		if (!allRequiredArtifacts.contains(pluginArtifact)) {
			allRequiredArtifacts.add(pluginArtifact);
			for (Dependency dependency: plugin.getDependencies()) {{
				addDependencyArtifacts(dependency, allRequiredArtifacts);
			}
		}
	}

}

	private void addDependencyArtifacts(Dependency dependency, Set<Artifact> allRequiredArtifacts) {
		Artifact dependencyArtifact = new Artifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getType());
		if (! allRequiredArtifacts.contains(dependencyArtifact)) {
			allRequiredArtifacts.add(dependencyArtifact);
		}
	}
}