package grails.plugin.json.view.api.internal.jbuilder

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors


@CompileStatic
class Errors {
    @InheritConstructors
    static class NullError extends NoSuchMethodException {
        static NullError build(key) {
            new NullError("Failed to add ${key} property to null object")
        }
    }

    @InheritConstructors
    static class ArrayError extends RuntimeException {
        static ArrayError build(key) {
            new ArrayError("Failed to add ${key} property to an array")
        }
    }

    @InheritConstructors
    static class MergeError extends RuntimeException {
        static MergeError build(Object current_value, Object updates) {
            new MergeError("Can't merge ${updates} into ${current_value}")
        }
    }
}