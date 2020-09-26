package de.hsudbrock.mvnebuildermavenplugin.graph;

import java.util.Optional;

public class EdgeTemplateModel {

	private final DependencyGraphNode nodeU;
	private final DependencyGraphNode nodeV;
	private final DependencyGraphEdge edgeValue;

	public EdgeTemplateModel(DependencyGraphNode nodeU, DependencyGraphNode nodeV,
			Optional<DependencyGraphEdge> edgeValue) {
		this.nodeU = nodeU;
		this.nodeV = nodeV;
		this.edgeValue = edgeValue.get();
	}

	public DependencyGraphNode getNodeU() {
		return nodeU;
	}

	public DependencyGraphNode getNodeV() {
		return nodeV;
	}

	public DependencyGraphEdge getEdgeValue() {
		return edgeValue;
	}
	
}
