package io.github.thefishlive;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.thefishlive.filter.DependencyFilter;
import io.github.thefishlive.filter.PluginFilter;
import io.github.thefishlive.filter.ScopeFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
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

    // Allow public access to the logger
    private static Log logger;

    @Component
    private MavenProjectBuilder projectBuilder;

    @Component
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository local;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remote;

    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir" )
    private File outputDirectory;

    @Parameter( defaultValue = "${project.build.finalName}.${project.packaging}.json", property ="outputName")
    private String outputName;

    @Parameter( defaultValue = "true", property = "prettyJson")
    private boolean pretty;

    @Parameter( property = "groupUrls" )
    private Map<String, String> urls;

    @Parameter( property = "filter" )
    private PluginFilter filter;


    public void execute() throws MojoExecutionException {
        if (project == null) {
            throw new MojoExecutionException("Project is null");
        }

        logger = getLog();

        JsonObject json = new JsonObject();
        JsonArray libs = new JsonArray();

        DependencyTreeWalker walker = new DependencyTreeWalker(this.projectBuilder, this.remote, this.local);
        
        List<Dependency> dependencies = walker.walkTree(this.project);
        int count = dependencies.size();
        filter.getDependencyFilter().filter(dependencies);

        getLog().debug("Skipped " + (count - dependencies.size()) + " artifacts");

        count = 0;

        for (Dependency dep : dependencies) {
            if (!filter.getScopeFilter().include(dep)) {
                getLog().debug("Excluded " + buildArtifactId(dep));
                continue;
            }

            JsonObject depJson = new JsonObject();
            depJson.add("name", new JsonPrimitive(buildArtifactId(dep)));
            depJson.add("url", new JsonPrimitive(getUrl(dep)));
            libs.add(depJson);

            getLog().info("Included " + buildArtifactId(dep) + " to libraries file");
            count++;
        }

        getLog().info("Included " + count + " dependencies in the libraries file");

        json.add("libs", libs);
        File libsFile = new File(outputDirectory, outputName);

        getLog().debug("Libraries File: " + libsFile.toString());

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
                getLog().debug("Setting gson to pretty print");
                builder.setPrettyPrinting();
            }

            builder.create().toJson(json, writer);
            getLog().debug("Written json correctly");
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not write libraries file", ex);
        }

        projectHelper.attachArtifact(project, "json", "libraries", libsFile);
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

    public static String buildArtifactId(Dependency dep) {
        return dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
    }

    public static Log getLogger() {
        return logger;
    }

}
