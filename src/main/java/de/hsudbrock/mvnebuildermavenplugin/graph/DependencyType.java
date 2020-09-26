package de.hsudbrock.mvnebuildermavenplugin.graph;

public enum DependencyType {
	
	COMPILE_DEP("compile"),
	TEST_DEP("test"),
	PROVIDED_DEP("provided"),
	RUNTIME_DEP("runtime"),
	SYSTEM_DEP("system"),
	IMPORT_DEP("import"),
	PLUGIN_DEP("plugin"),
	REPORT_DEP("report"),
	UNKNOWN_DEP("unknown"), 
	PARENT("parent");

	private final String name;
	
	DependencyType(String name) {
		this.name = name;
	}

	public static DependencyType fromScope(String scope) {
		for (DependencyType depType: DependencyType.values()) {
			if (depType.name.equals(scope)) {
				return depType;
			}
		}
		
		return UNKNOWN_DEP;
	}
	
	public String toString() {
		return name;
	}

}
