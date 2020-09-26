package de.hsudbrock.mvnebuildermavenplugin.graph;

public class EdgeTemplateModel {

	private final DependencyGraphNode nodeU;
	private final DependencyGraphNode nodeV;
	private final DependencyGraphEdge edge;

	public EdgeTemplateModel(DependencyGraphNode nodeU, DependencyGraphNode nodeV,
			DependencyGraphEdge edge) {
		this.nodeU = nodeU;
		this.nodeV = nodeV;
		this.edge = edge;
	}

	public DependencyGraphNode getNodeU() {
		return nodeU;
	}

	public DependencyGraphNode getNodeV() {
		return nodeV;
	}

	public DependencyGraphEdge getEdgeValue() {
		return edge;
	}
	
}
