package com.github.aristotll.jsonview.utils

import groovy.transform.CompileStatic

/**
 * https://stackoverflow.com/a/29698326/4680436
 */
@CompileStatic
class MapUtils {
    static Map deepMerge(Map original, Map newMap) {
        return _deepMerge(new HashMap(original), newMap)
    }

    private static Map _deepMerge(Map original, Map newMap) {
        for (def entry : newMap.entrySet()) {
            def key = entry.getKey()
            Object n = entry.getValue()
            Object o = original.get(key)
            if (n instanceof Map && o instanceof Map) {
                original.put(key, deepMerge((Map) o, (Map) n))
            } else {
                original.put(key, n)
            }
        }
        return original
    }
}
