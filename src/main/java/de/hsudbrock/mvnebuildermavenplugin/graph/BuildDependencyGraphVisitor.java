package de.hsudbrock.mvnebuildermavenplugin.graph;

import static de.hsudbrock.mvnebuildermavenplugin.graph.DependencyType.fromScope;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public class BuildDependencyGraphVisitor implements DependencyVisitor {

	private final Deque<DependencyGraphNode> nodeStack = new ArrayDeque<>();
	private final MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph;

	public BuildDependencyGraphVisitor(MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> graph) {
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
				graph.addNode(graphNode);
				graph.addNode(parentNode);
				graph.putEdgeValue(parentNode, graphNode,
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

	public MutableValueGraph<DependencyGraphNode, DependencyGraphEdge> getGraph() {
		return graph;
	}

}
