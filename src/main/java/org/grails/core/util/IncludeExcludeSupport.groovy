package org.grails.core.util


import groovy.transform.CompileStatic

/**
 * Simple support class for simplifying include/exclude handling
 *
 * @since 2.3
 * @author Graeme Rocher
 */
@CompileStatic
class IncludeExcludeSupport<T> {

    static final String INCLUDES_PROPERTY = "includes"
    static final String EXCLUDES_PROPERTY = "excludes"

    List<T> defaultIncludes
    List<T> defaultExcludes

    IncludeExcludeSupport(List<T> defaultIncludes = null, List<T> defaultExcludes = []) {
        this.defaultIncludes = defaultIncludes
        this.defaultExcludes = defaultExcludes
    }

    boolean shouldInclude(List<T> incs, List excs, T object) {
        includes(defaultIncludes, object) && includes(incs, object) && !excludes(defaultExcludes, object) && !excludes(excs, object)
    }

    boolean includes(List<T> includes, T object) {
        includes == null || includes.contains(object)
    }

    boolean excludes(List<T> excludes, T object) {
        excludes != null && excludes.contains(object)
    }
}
