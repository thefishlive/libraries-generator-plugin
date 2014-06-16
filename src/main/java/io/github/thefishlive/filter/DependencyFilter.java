package io.github.thefishlive.filter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import io.github.thefishlive.LibsGenerationMojo;
import org.apache.maven.model.Dependency;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DependencyFilter implements Filter<Dependency> {

    private Set<String> includes = Sets.newHashSet();
    private Set<String> excludes = Sets.newHashSet();

    @Override
    public Set<String> getIncludes () {
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
            for (String include : getIncludes()) {
                if (matches(include, LibsGenerationMojo.buildArtifactId(dependency))) {
                    LibsGenerationMojo.getLogger().debug("Including " + LibsGenerationMojo.buildArtifactId(dependency));
                    return true;
                }
            }

            fallthrough = false;
        }

        if (this.excludes.size() > 0) {
            for (String include : getExcludes()) {
                if (matches(include, LibsGenerationMojo.buildArtifactId(dependency))) {
                    LibsGenerationMojo.getLogger().debug("Excluding " + LibsGenerationMojo.buildArtifactId(dependency));
                    return false;
                }
            }
        }

        return fallthrough;
    }

    public void filter(List<Dependency> dependencies) {
        List<Dependency> iterating = ImmutableList.copyOf(dependencies); // Create immutable copy to iterate over

        for (Dependency current : iterating) {
            if (!include(current)) {
                dependencies.remove(current);
            }
        }
    }

    private boolean matches(String rule, String dependency) {
        return Pattern.compile(rule).matcher(dependency).matches();
    }

}
