package io.github.thefishlive.filter;

public class PluginFilter {

    private DependencyFilter dependency;
    private ScopeFilter scope;

    public DependencyFilter getDependencyFilter() {
        return this.dependency;
    }

    public ScopeFilter getScopeFilter() {
        return this.scope;
    }
}
