package de.hsudbrock.mvnebuildermavenplugin.graph;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyGraphNode {
	
	private final String groupId;
	private final String artifactId;
	private final String version;
	
	private final String type;
	private final String classifier;
	
	private final boolean optional;
	
	private boolean asProject = false;
	private boolean projectFailure = false;

	public DependencyGraphNode(DependencyNode depNode) {
		this.groupId = depNode.getArtifact().getGroupId();
		this.artifactId = depNode.getArtifact().getArtifactId();
		this.version = depNode.getArtifact().getVersion();
		
		this.type = depNode.getArtifact().getExtension();
		this.classifier = depNode.getArtifact().getClassifier();
		
		this.optional = depNode.getDependency() != null ? depNode.getDependency().isOptional() : false;
	}
	
	public DependencyGraphNode(MavenProject project, String type) {
		this.groupId = project.getGroupId();
		this.artifactId = project.getArtifactId();
		this.version = project.getVersion();
		
		this.type = type;
		this.classifier = project.getArtifact().getClassifier();
		
		this.optional = false;
	}

	public DependencyGraphNode(Plugin plugin) {
		this.groupId = plugin.getGroupId();
		this.artifactId = plugin.getArtifactId();
		this.version = plugin.getVersion();
		
		this.type = "jar";
		this.classifier = "";
		
		this.optional = false;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}
	
	public String getClassifier() {
		return classifier;
	}
	
	public String getType() {
		return type;
	}

	public boolean isAsProject() {
		return asProject;
	}
	
	public boolean isOptional() {
		return optional;
	}

	public void setAsProject(boolean asProject) {
		this.asProject = asProject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DependencyGraphNode other = (DependencyGraphNode) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version;
	}

	public void setProjectFailure(boolean projectFailure) {
		this.projectFailure = projectFailure;
	}

	public boolean isProjectFailure() {
		return projectFailure;
	}
	
}
