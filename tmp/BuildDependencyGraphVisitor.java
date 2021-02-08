package de.hsudbrock.mvnebuildermavenplugin.graph;

import static de.hsudbrock.mvnebuildermavenplugin.graph.DependencyType.fromScope;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.jgrapht.graph.DefaultDirectedGraph;

public class BuildDependencyGraphVisitor implements DependencyVisitor {

	private final Deque<DependencyGraphNode> nodeStack = new ArrayDeque<>();
	private final DefaultDirectedGraph<DependencyGraphNode, DependencyGraphEdge> graph;

	public BuildDependencyGraphVisitor(DefaultDirectedGraph<DependencyGraphNode, DependencyGraphEdge> graph) {
		this.graph = graph;
	}

	@Override
	public boolean visitEnter(DependencyNode node) {
		DependencyGraphNode graphNode = new DependencyGraphNode(node);
		nodeStack.push(graphNode);

		return true;
	}

	@Override
	public boolean visitLeave(DependencyNode node) {
		DependencyGraphNode graphNode = new DependencyGraphNode(node);
		nodeStack.pop();
		DependencyGraphNode parentNode = nodeStack.peek();

		if (parentNode != null) {
			if (permit(node)) {
				graph.addVertex(graphNode);
				graph.addVertex(parentNode);
				graph.addEdge(parentNode, graphNode,
						new DependencyGraphEdge(fromScope(node.getDependency().getScope())));
			}
		}

		return true;
	}

	private boolean permit(DependencyNode node) {
		if (node.getDependency() != null && "test".equals(node.getDependency().getScope())) {
			return false;
		} else if (node.getDependency() != null && node.getDependency().isOptional()) {
			return false;
		} else {
			return true;
		}
	}

	public DefaultDirectedGraph<DependencyGraphNode, DependencyGraphEdge> getGraph() {
		return graph;
	}

}
