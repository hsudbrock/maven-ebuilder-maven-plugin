package de.hsudbrock.mvnebuildermavenplugin.graph;

public class DependencyGraphEdge {
	
	private final DependencyType dependencyType;

	public DependencyGraphEdge(DependencyType dependencyType) {
		this.dependencyType = dependencyType;
	}

	public DependencyType getDependencyType() {
		return dependencyType;
	}
	
}
