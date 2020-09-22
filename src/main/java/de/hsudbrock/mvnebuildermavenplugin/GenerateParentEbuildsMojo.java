package de.hsudbrock.mvnebuildermavenplugin;

import static freemarker.template.Configuration.VERSION_2_3_30;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "generate-parent-ebuilds")
public class GenerateParentEbuildsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;
	
	@Parameter(defaultValue = "https://repo1.maven.org/maven2")
	private String mavenRepoBase;
	
	@Parameter(property = "pomCategory", defaultValue = "dev-java")
	private String pomCategory;
	
	@Parameter(property = "repoTarget", defaultValue="${project.build.directory}/portage-repo")
	private String repoTarget;
	
	@Parameter(property = "maintainerEmail", defaultValue="gentoo@hsudbrock.de")
	private String maintainerEmail;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Configuration freemarkerConfig = createFreemarkerConfig();

		Template pomEbuildTemplate;
		Template metadataTemplate;
		try {
			pomEbuildTemplate = freemarkerConfig.getTemplate("pom-ebuild.ftl");
			metadataTemplate = freemarkerConfig.getTemplate("metadata.xml.ftl");
		} catch (IOException e) {
			throw new MojoFailureException("Could not load template for pom ebuilds", e);
		}
		
		Path targetFolder = Paths.get(repoTarget, pomCategory);
		try {
			Files.createDirectories(targetFolder);
		} catch (IOException e) {
			throw new MojoFailureException("Could not create target folder", e);
		}

		for (MavenProject parentProject : getParentProjects(project)) {
			Path ebuildFolder = targetFolder.resolve(getFolderName(parentProject));
			try {
				Files.createDirectories(ebuildFolder);
			} catch (IOException e) {
				throw new MojoFailureException("Could not creat the folder" + ebuildFolder, e);
			}
			
			Path ebuildFile = ebuildFolder.resolve(getEbuildName(parentProject) + ".ebuild");
			try (Writer out = new FileWriter(ebuildFile.toFile())) {
				pomEbuildTemplate.process(createEbuildModel(parentProject), out);
			} catch (TemplateException | IOException | LicenseNotFoundException e) {
				throw new MojoFailureException("Could not render the pom ebuild for parent project " + parentProject.toString(), e);
			}
			
			Path metadataFile = ebuildFolder.resolve("metadata.xml");
			try (Writer out = new FileWriter(metadataFile.toFile())) {
				metadataTemplate.process(createMetadataModel(parentProject), out);
			} catch (TemplateException | IOException | LicenseNotFoundException e) {
				throw new MojoFailureException("Could not render the metadata file for parent project " + parentProject.toString(), e);
			}
		}
	}
	
	private String getFolderName(MavenProject project) {
		return project.getArtifactId() + "-pom";
	}

	private String getEbuildName(MavenProject project) {
		return project.getArtifactId() + "-pom-" + project.getVersion();
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

	private Configuration createFreemarkerConfig() {
		Configuration config = new Configuration(VERSION_2_3_30);
		config.setDefaultEncoding("UTF-8");
		config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		config.setClassForTemplateLoading(this.getClass(), "/");
		return config;
	}

	private Map<String, Object> createEbuildModel(MavenProject project) throws LicenseNotFoundException, MojoFailureException {
		Map<String, Object> model = new HashMap<>();
		model.put("groupId", project.getGroupId());
		model.put("artifactId", project.getArtifactId());
		model.put("version", project.getVersion());
		
		model.put("description", limitStringTo(70, project.getDescription().replace('\n', ' ')));
		model.put("homepage", project.getUrl());
		
		
		List<String> licenses = getLicenses(project);
		if (licenses.size() > 1) {
			throw new MojoFailureException("No support for projects with multiple licenses currently, failure for pom " + project.getArtifactId());
		} else if (licenses.size() == 1) {
			model.put("license", licenses.get(0));
		} else {
			model.put("license", "TODO: No license provided by POM");
		}
		
		model.put("src_uri", mavenRepoBase + getPomPath(project));
		model.put("pomFileName", project.getArtifactId() + "-" + project.getVersion() + ".pom");
		
		if (project.getParent() != null) {
			model.put("parentPomAtom", pomCategory + "/" + getEbuildName(project.getParent()));
		}
		return model;
	}
	
	private Map<String, Object> createMetadataModel(MavenProject project) throws LicenseNotFoundException, MojoFailureException {
		Map<String, Object> model = new HashMap<>();
		model.put("maintainerEmail", maintainerEmail);
		return model;
	}
	
	private Object limitStringTo(int maxLength, String s) {
		if (s.length() < maxLength) {
			return s;
		} else {
			return s.substring(0, maxLength);
		}
	}

	private List<String> getLicenses(MavenProject project) throws LicenseNotFoundException {
		List<String> result = new ArrayList<>();
		for (License license: project.getLicenses()) {
			result.add(getGentooLicense(license));
		}
		return result;
	}
	
	private String getGentooLicense(License license) throws LicenseNotFoundException {
		String gentooLicense = new LicenseMap().getGentooLicenseString(license.getName());
		if (gentooLicense != null) {
			return gentooLicense;
		} else {
			throw new LicenseNotFoundException("License '" + license.getName() + "' unknown");
		}
	}

	private String getPomPath(MavenProject project) {
		return "/" + project.getGroupId().replace('.', '/') + 
				"/" + project.getArtifactId() + "/" + 
				project.getVersion() + "/" + 
				project.getArtifactId() + "-" + project.getVersion() + ".pom";
	}

}
