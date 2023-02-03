package grails.views


import groovy.transform.CompileStatic
/**
 * Default configuration
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
trait GenericViewConfiguration implements ViewConfiguration {

    /**
     * The encoding to use
     */
    String encoding = "UTF-8"
    /**
     * Whether to pretty print
     */
    boolean prettyPrint = false
    /**
     * Whether to use absolute links
     */
    boolean useAbsoluteLinks = false
    /**
     * Whether to enable reloading
     */
    boolean enableReloading = true
    /**
     * The package name to use
     */
    String packageName = "me.json.generated"
    /**
     * Whether to compile templates statically
     */
    boolean compileStatic = true
    /**
     * The file extension of the templates
     */
    String extension
    /**
     * The template base class
     */
    Class baseTemplateClass
    /**
     * Whether the cache templates
     */
    boolean cache = true
    /**
     * Whether resource expansion is allowed
     */
    boolean allowResourceExpansion = true
    /**
     * The default package imports
     */
    String[] packageImports = ['groovy.transform'] as String[]
    /**
     * The default static imports
     */
    String[] staticImports = [] as String[]

}
