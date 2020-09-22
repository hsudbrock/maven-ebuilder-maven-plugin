package de.hsudbrock.mvnebuildermavenplugin;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "list-parents")
public class ListParentsMojo extends AbstractMojo {
	
	@Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Parent projects:");
		getLog().info(getParentProjects(project).stream().map(project -> getDescription(project)).collect(joining("\n")));
	}
	
	private List<MavenProject> getParentProjects(MavenProject project) {
		List<MavenProject> result = new ArrayList<>();
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			result.add(parentProject);
			result.addAll(getParentProjects(parentProject));
		}
		return result;
	}
	
	private String getDescription(MavenProject project) {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
	}

}
