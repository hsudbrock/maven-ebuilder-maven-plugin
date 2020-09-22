package de.hsudbrock.mvnebuildermavenplugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps license Strings used by Maven POMs to license Strings used by Gentoo.
 * <p>
 * This is a temporary solution; for a better version of this plugin, use some config-file-based variant. 
 */
public class LicenseMap {
	
	private Map<String, String> licenseMap = new HashMap<>(); 
	
	public LicenseMap() {
		licenseMap.put("Apache License, Version 2.0", "Apache-2.0");
	}

	public String getGentooLicenseString(String pomLicenseString) {
		return licenseMap.get(pomLicenseString);
	}
	
}
