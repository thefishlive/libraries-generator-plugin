package io.github.thefishlive;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"MismatchedReadAndWriteOfArray", "MismatchedQueryAndUpdateOfCollection"})
@Mojo( name = "generate-libs", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class LibsGenerationMojo extends AbstractMojo {

    @Component
    private MavenProjectBuilder projectBuilder;

    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository local;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remote;

    @Parameter( defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir" )
    private File outputDirectory;

    @Parameter( defaultValue = "${project.build.finalName}.${project.packaging}.json", property ="outputName")
    private String outputName;

    @Parameter( defaultValue = ":", property = "artifactSeparator")
    private char separator;

    @Parameter( defaultValue = "true", property = "prettyJson")
    private boolean pretty;

    @Parameter( property = "groupUrls" )
    private Map<String, String> urls;

    @Parameter( property = "excludes" )
    private List<String> excludes;

    @Parameter( property = "scopes" )
    private List<String> scopes;

    public void execute() throws MojoExecutionException {
        if (project == null) {
            throw new MojoExecutionException("Project is null");
        }

        JsonObject json = new JsonObject();
        JsonArray libs = new JsonArray();

        for (Dependency dep : getDependencies()) {

            if (!shouldInclude(dep)) {
                getLog().debug("Skipped " + buildArtifactId(dep) + separator + dep.getScope());
                continue;
            }

            JsonObject depJson = new JsonObject();
            depJson.add("name", new JsonPrimitive(buildArtifactId(dep)));
            depJson.add("url", new JsonPrimitive(getUrl(dep)));
            libs.add(depJson);

            getLog().info("Added " + buildArtifactId(dep) + separator + dep.getScope() + " to libraries file");
        }

        json.add("libs", libs);
        File libsFile = new File(outputDirectory, outputName);

        if (libsFile.exists() && !libsFile.delete()) {
            throw new MojoExecutionException("Could not delete old libraries file");
        }

        try {
            if (!libsFile.getParentFile().exists() && !libsFile.getParentFile().mkdirs()) {
                throw new IOException("Could not create parent file for libraries file");
            }

            if (!libsFile.createNewFile()) {
                throw new IOException("New file was not created correctly");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create libraries file", e);
        }

        try (FileWriter writer = new FileWriter(libsFile)) {
            GsonBuilder builder = new GsonBuilder();

            if (pretty) {
                builder.setPrettyPrinting();
            }

            builder.create().toJson(json, writer);
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not write libraries file", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Dependency> getDependencies() throws MojoExecutionException {
        List<Dependency> deps = project.getDependencies();

        for (Artifact artifact : (Set<Artifact>) project.getDependencyArtifacts()) {
            MavenProject dependency = buildProject(artifact);

            if (dependency.getDependencies() != null) {
                for (Dependency dep : (List<Dependency>) dependency.getDependencies()) {
                    if (!contains(deps, dep)) {
                        deps.add(dep);
                    }
                }
            }
        }

        return deps;
    }

    private boolean contains(List<Dependency> deps, Dependency dep) {
        for (Dependency current : deps) {
            if (current.getGroupId().equals(dep.getGroupId()) && current.getArtifactId().equals(dep.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private MavenProject buildProject(Artifact artifact) throws MojoExecutionException {
        try {
            return projectBuilder.buildFromRepository(artifact, this.remote, this.local);
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error building project for " + artifact.toString(), e);
        }
    }

    private boolean shouldInclude(Dependency dep) {
        return !excludes.contains(buildArtifactId(dep)) && scopes.contains(dep.getScope());
    }

    private String buildArtifactId(Dependency dep) {
        return dep.getGroupId() + separator + dep.getArtifactId() + separator + dep.getVersion();
    }

    private String getUrl(Dependency dep) {
        String baseUrl = "http://repo1.maven.org/maven2/";

        if (urls.containsKey(dep.getGroupId())) {
            baseUrl = urls.get(dep.getGroupId());
        }

        baseUrl += dep.getGroupId().replace('.', '/') + "/";
        baseUrl += dep.getArtifactId() + "/";
        baseUrl += dep.getVersion() + "/";
        baseUrl += dep.getArtifactId() + "-" + dep.getVersion() + ".jar";

        return baseUrl;
    }
}
