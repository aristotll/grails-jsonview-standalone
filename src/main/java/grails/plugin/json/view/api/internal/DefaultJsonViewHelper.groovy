package grails.plugin.json.view.api.internal

import org.grails.datastore.mapping.model.MappingFactory
import grails.plugin.json.view.api.JsonView
import grails.views.utils.ViewUtils
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.StackTraceUtils
import org.grails.core.util.IncludeExcludeSupport
import org.springframework.util.ReflectionUtils

import java.beans.PropertyDescriptor
import java.lang.reflect.ParameterizedType

@CompileStatic
class DefaultJsonViewHelper {

    final JsonView view

    DefaultJsonViewHelper(JsonView view) {
        this.view = view
    }

    JsonGenerator getGenerator() {
        view.generator
    }
    public static final String PAGINATION_SORT = "sort"
    public static final String PAGINATION_ORDER = "order"
    public static final String PAGINATION_MAX = "max"
    public static final String PAGINATION_OFFSET = "offset"
    public static final String PAGINATION_TOTAL = "total"
    public static final String PAGINATION_RESROUCE = "resource"
    public static final List<String> DEFAULT_EXCLUDES = ["class", 'metaClass', 'properties']
    public static final List<String> DEFAULT_VALIDATEABLE_EXCLUDES = DEFAULT_EXCLUDES + ["errors"]
    public static final List<String> DEFAULT_GORM_EXCLUDES = DEFAULT_VALIDATEABLE_EXCLUDES + ["version", "attached", "dirty"]

    /**
     * The expand parameter
     */
    String EXPAND = "expand"

    /**
     * The associations parameter
     */
    String ASSOCIATIONS = "associations"

    protected final Set<String> TO_STRING_TYPES = [
            "org.bson.types.ObjectId"
    ] as Set

    public static final String NULL_VALUE = "null"
    protected final Writable NULL_OUTPUT = new Writable() {
        @Override
        Writer writeTo(Writer out) throws IOException {
            out.write(NULL_VALUE)
            return out
        }
    }

    IncludeExcludeSupport<String> simpleIncludeExcludeSupport = new DefaultJsonViewIncludeExcludeSupport(null, DEFAULT_EXCLUDES)
    IncludeExcludeSupport<String> validateableIncludeExcludeSupport = new DefaultJsonViewIncludeExcludeSupport(null, DEFAULT_VALIDATEABLE_EXCLUDES)
    IncludeExcludeSupport<String> includeExcludeSupport = new DefaultJsonViewIncludeExcludeSupport(null, DEFAULT_GORM_EXCLUDES)

    List<String> getIncludes(Map arguments) {
        ViewUtils.getStringListFromMap(IncludeExcludeSupport.INCLUDES_PROPERTY, arguments, null)
    }

    List<String> getExcludes(Map arguments) {
        ViewUtils.getStringListFromMap(IncludeExcludeSupport.EXCLUDES_PROPERTY, arguments)
    }

    Boolean getRenderNulls(Map arguments) {
        ViewUtils.getBooleanFromMap('renderNulls', arguments, false)
    }


    protected Class getGenericType(Class declaringClass, PropertyDescriptor descriptor) {
        def field = ReflectionUtils.findField(declaringClass, descriptor.getName())
        if (field != null) {

            def type = field.genericType
            if (type instanceof ParameterizedType) {
                def args = ((ParameterizedType) type).getActualTypeArguments()
                if (args.length > 0) {
                    def t = args[0]
                    if (t instanceof Class) {
                        return (Class) t
                    }
                }
            }
        }
        return Object
    }

    boolean isStringType(Class propertyType) {
        return TO_STRING_TYPES.contains(propertyType.name)
    }

    boolean isSimpleType(Class propertyType, value) {
        MappingFactory.isSimpleType(propertyType.name) || (value instanceof Enum) || (value instanceof Map)
    }

    protected List<Object> getJsonStackTrace(Throwable e) {
        StackTraceUtils.sanitize(e)
        e.stackTrace
                .findAll() { StackTraceElement element -> element.lineNumber > -1 }
                .collect() { StackTraceElement element ->
                    "$element.lineNumber | ${element.className}.$element.methodName".toString()
                }.toList() as List<Object>
    }


    protected boolean includeAssociations(Map arguments) {
        ViewUtils.getBooleanFromMap(ASSOCIATIONS, arguments, true)
    }


    /**
     * Creates a new Parameter map with the new offset
     * Note: necessary to avoid clone until Groovy 2.5.x https://issues.apache.org/jira/browse/GROOVY-7325
     *
     * @param map The parameter map to copy
     * @param offset The new offset to use
     * @return The resulting parameters
     */
    protected Map<String, Object> paramsWithOffset(Map<String, Object> originalParameters, Integer offset) {
        Map<String, Object> params = [:]
        originalParameters.each { String k, Object v ->
            params.put(k, v)
        }
        params.put(PAGINATION_OFFSET, offset)
        return params
    }

    protected Integer getPrevOffset(Integer offset, Integer max) {
        if (offset <= 0) {
            return null
        }
        return Math.max(offset - max, 0)
    }

    protected Integer getNextOffset(Integer total, Integer offset, Integer max) {
        if (offset < 0 || offset + max >= total) {
            return null
        }
        return offset + max
    }

    protected Integer getLastOffset(Integer total, Integer max) {
        if (total <= 0) {
            return null
        }
        Integer laststep = ((int) Math.round(Math.ceil((double) total / max))) - 1
        return Math.max((laststep * max), 0)
    }


}
