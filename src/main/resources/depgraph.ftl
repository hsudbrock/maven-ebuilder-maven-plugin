strict digraph "dependency-graph" {
  node [shape="box",style="rounded",fontname="Helvetica",fontsize="14"]
  edge [fontsize="10",fontname="Helvetica"]
  
<#list nodes as node>
  "${node.groupId}:${node.artifactId}:${node.version!"missing"}" [label="${node.groupId}:${node.artifactId}:${node.version!"missing"}${node.projectFailure?string("FAIL", "")}"];
</#list>

<#list edges as edge>
  "${edge.nodeU.groupId}:${edge.nodeU.artifactId}:${edge.nodeU.version!"missing"}" -> "${edge.nodeV.groupId}:${edge.nodeV.artifactId}:${edge.nodeV.version!"missing"}" [label="${edge.edgeValue.dependencyType}"]
</#list>
}