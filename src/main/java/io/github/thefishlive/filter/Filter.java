package io.github.thefishlive.filter;

import java.util.Set;

public interface Filter<T> {

    public Set<String> getIncludes();

    public Set<String> getExcludes();

    public boolean include(T t);

}
