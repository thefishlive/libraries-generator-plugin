package io.github.thefishlive.filter;

import com.google.common.collect.Sets;
import io.github.thefishlive.LibsGenerationMojo;
import org.apache.maven.model.Dependency;

import java.util.Set;

public class ScopeFilter implements Filter<Dependency> {

    private Set<String> includes = Sets.newHashSet();
    private Set<String> excludes = Sets.newHashSet();

    @Override
    public Set<String> getIncludes() {
        return this.includes;
    }

    @Override
    public Set<String> getExcludes() {
        return this.excludes;
    }

    @Override
    public boolean include(Dependency dependency) {
        boolean fallthrough = true;

        if (this.includes.size() > 0) {
            if (getIncludes().contains(dependency.getScope())) {
                LibsGenerationMojo.getLogger().debug("Including " + LibsGenerationMojo.buildArtifactId(dependency));
                return true;
            }

            fallthrough = false;
        }

        if (this.excludes.size() > 0) {
            if (getExcludes().contains(dependency.getScope())) {
                LibsGenerationMojo.getLogger().debug("Excluding " + LibsGenerationMojo.buildArtifactId(dependency));
                return false;
            }
        }

        return fallthrough;
    }
}
