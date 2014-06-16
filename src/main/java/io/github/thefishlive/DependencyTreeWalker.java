package io.github.thefishlive;

import com.google.common.collect.Lists;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.util.List;
import java.util.Set;

public class DependencyTreeWalker {

    private final MavenProjectBuilder mavenProjectBuilder;
    private final List<ArtifactRepository> remote;
    private final ArtifactRepository local;
    private List<Dependency> dependencies;


    public DependencyTreeWalker(MavenProjectBuilder builder, List<ArtifactRepository> remote, ArtifactRepository local) {
        this.mavenProjectBuilder = builder;
        this.remote = remote;
        this.local = local;
    }

    public List<Dependency> walkTree(MavenProject project) throws MojoExecutionException {
        this.dependencies = Lists.newArrayList();
        walkPartialTree(project);
        LibsGenerationMojo.getLogger().info("Found " + this.dependencies.size() + " dependencies");
        return this.dependencies;
    }


    public void walkPartialTree(MavenProject project) throws MojoExecutionException {
        if (project.getDependencies() == null) {
            return;
        }

        for (Dependency current : (List<Dependency>) project.getDependencies()) {
            if (!contains(current)) {
                this.dependencies.add(current);
            }
        }

        if (project.getDependencyArtifacts() == null) {
            return;
        }

        for (Artifact dependency : (Set<Artifact>) project.getDependencyArtifacts()) {
            MavenProject projectDependency = buildProject(dependency);

            if (projectDependency.getDependencies() != null) {
                walkPartialTree(projectDependency);
            }
        }
    }

    private boolean contains(Dependency dependency) {
        for (Dependency current : this.dependencies) {
            if (current.getGroupId().equals(dependency.getGroupId()) && current.getArtifactId().equals(dependency.getArtifactId())) {
                return true;
            }
        }

        return false;
    }

    private MavenProject buildProject(Artifact artifact) throws MojoExecutionException {
        try {
            return mavenProjectBuilder.buildFromRepository(artifact, this.remote, this.local);
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error building project for " + artifact.toString(), e);
        }
    }

}
