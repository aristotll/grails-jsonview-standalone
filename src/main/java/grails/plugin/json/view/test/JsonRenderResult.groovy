package grails.plugin.json.view.test

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * A result object returned by {@link JsonViewTest}
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
@Canonical
class JsonRenderResult {
    /**
     * The JSON result
     */
    Object json
    /**
     * The raw JSON text
     */
    String jsonText
}
