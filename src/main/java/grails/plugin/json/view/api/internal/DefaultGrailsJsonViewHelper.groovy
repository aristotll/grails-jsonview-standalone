package grails.plugin.json.view.api.internal

import com.github.aristotll.jsonview.JsonViewTemplateConfig
import grails.views.JsonGroovyTemplateEngineImpl
import grails.views.ResolvableGroovyTemplateEngine
import org.grails.datastore.mapping.model.MappingFactory
import grails.plugin.json.view.api.GrailsJsonViewHelper
import grails.plugin.json.view.api.JsonView
import grails.util.GrailsNameUtils
import grails.views.ViewException
import grails.views.utils.ViewUtils
import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.StreamingJsonBuilder
import groovy.json.StreamingJsonBuilder.StreamingJsonDelegate
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import org.grails.core.util.IncludeExcludeSupport
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

import java.beans.PropertyDescriptor

import static grails.views.utils.JsonOutputPublic.*

/**
 * Extended version of {@link DefaultGrailsViewHelper} with methods specific to JSON view rendering
 *
 * @author Graeme Rocher
 */
@CompileStatic
@Slf4j
class DefaultGrailsJsonViewHelper extends DefaultJsonViewHelper implements GrailsJsonViewHelper {

    public static final SpringTemplateEngine JSON_VIEW_TEMPLATE_ENGINE = new JsonViewTemplateConfig().jsonViewTemplateEngine()
    public static final ResolvableGroovyTemplateEngine GROOVY_TEMPLATE_ENGINE = new JsonGroovyTemplateEngineImpl()

    DefaultGrailsJsonViewHelper(JsonView view) {
        super(view)
    }
    IncludeExcludeSupport<String> simpleIncludeExcludeSupport = new DefaultJsonViewIncludeExcludeSupport(null, DEFAULT_EXCLUDES)

    SpringTemplateEngine jsonViewTemplateEngine = JSON_VIEW_TEMPLATE_ENGINE

    TemplateEngine templateEngine = GROOVY_TEMPLATE_ENGINE

    public static final String NULL_VALUE = "null"
    protected final Writable NULL_OUTPUT = new Writable() {
        @Override
        Writer writeTo(Writer out) throws IOException {
            out.write(NULL_VALUE)
            return out
        }
    }

    public static final String BEFORE_CLOSURE = "beforeClosure"
    public static final String PROCESSED_OBJECT_VARIABLE = "org.json.views.RENDER_PROCESSED_OBJECTS"

    @Override
    Writable render(Object object, @DelegatesTo(StreamingJsonDelegate) Closure customizer) {
        render object, Collections.emptyMap(), customizer
    }

    static final String INCLUDES_PROPERTY = "includes"
    static final String EXCLUDES_PROPERTY = "excludes"


    void inline(Object object, Map arguments = Collections.emptyMap(), @DelegatesTo(StreamingJsonDelegate) Closure customizer = null, StreamingJsonDelegate jsonDelegate) {
        JsonView jsonView = (JsonView) view
        Map<Object, Writable> processedObjects = initializeProcessedObjects(jsonView.binding)
        List<String> incs = getIncludes(arguments)
        List<String> excs = getExcludes(arguments)
        boolean renderNulls = getRenderNulls(arguments)
        processSimple(jsonDelegate, object, processedObjects, incs, excs, "", renderNulls, customizer)
    }

    void inline(Object object, Map arguments = Collections.emptyMap(), @DelegatesTo(StreamingJsonDelegate) Closure customizer = null) {
        def jsonDelegate = new StreamingJsonDelegate(view.out, true)
        inline(object, arguments, customizer, jsonDelegate)
    }

    @Override
    void inline(Object object, @DelegatesTo(StreamingJsonDelegate) Closure customizer) {
        inline(object, Collections.emptyMap(), customizer)
    }

    private Writable preProcessedOutput(Object object, Map<Object, Writable> processedObjects) {
        boolean rootRender = processedObjects.isEmpty()
        if (object == null) {
            return NULL_OUTPUT
        }

        if (!rootRender && processedObjects.containsKey(object)) {
            def existingOutput = processedObjects.get(object)
            if (!NULL_OUTPUT.equals(existingOutput)) {
                return existingOutput
            }
        }
        return null
    }


    private Writable renderTemplateOrDefault(Object object, Map arguments, Closure customizer, Map<Object, Writable> processedObjects, String path = "") {
        Writable preProcessed = preProcessedOutput(object, processedObjects)
        if (preProcessed != null) {
            return preProcessed
        }
        renderDefault(object, arguments, customizer, processedObjects, path)
    }

    private Writable renderDefault(Object object, Map arguments, Closure customizer, Map<Object, Writable> processedObjects, String path = "") {
        Writable preProcessed = preProcessedOutput(object, processedObjects)
        if (preProcessed != null) {
            return preProcessed
        }

        JsonView jsonView = (JsonView) view
        boolean rootRender = processedObjects.isEmpty()
        def binding = jsonView.getBinding()
        final Closure beforeClosure = (Closure) arguments.get(BEFORE_CLOSURE)
        boolean renderNulls = getRenderNulls(arguments)


        Closure doProcessSimple = { StreamingJsonDelegate jsonDelegate, List<String> incs, List<String> excs ->
            processSimple(jsonDelegate, object, processedObjects, incs, excs, path, renderNulls, customizer)
        }

        JsonGenerator generator = getGenerator()
        def jsonWritable = new Writable() {
            @Override
            @CompileStatic
            Writer writeTo(Writer out) throws IOException {
                try {
                    StreamingJsonBuilder builder = new StreamingJsonBuilder(out, generator)
                    builder.call {
                        StreamingJsonDelegate jsonDelegate = (StreamingJsonDelegate) getDelegate()
                        if (beforeClosure != null) {
                            beforeClosure.setDelegate(jsonDelegate)
                            beforeClosure.call(object)
                        }
                        List<String> incs = ViewUtils.getStringListFromMap(INCLUDES_PROPERTY, arguments, null)
                        List<String> excs = ViewUtils.getStringListFromMap(EXCLUDES_PROPERTY, arguments)

                        doProcessSimple(jsonDelegate, incs, excs)
                    }


                    processedObjects.put(object, this)
                    return out
                } finally {
                    if (rootRender) {
                        binding.variables.remove(PROCESSED_OBJECT_VARIABLE)
                    }
                }
            }
        }

        return jsonWritable
    }

    protected Writable getIterableWritable(Iterable object, Map arguments, Closure customizer, Map<Object, Writable> processedObjects, String path = "") {
        return getIterableWritable(object) { Object o, Writer out ->
            handleValue(o, out, arguments, customizer, processedObjects, path)
        }

    }

    protected Writable getIterableWritable(Iterable object, Closure forEach) {
        return new Writable() {
            @Override
            Writer writeTo(Writer out) throws IOException {
                Iterable iterable = (Iterable) object
                boolean first = true
                out.append OPEN_BRACKET
                for (o in iterable) {
                    if (!first) {
                        out.append COMMA
                    }
                    forEach.call(o, out)
                    first = false
                }
                out.append CLOSE_BRACKET
            }
        }
    }

    protected Writable getMapWritable(Map object, Map arguments, Closure customizer, Map<Object, Writable> processedObjects) {
        return new Writable() {

            @Override
            Writer writeTo(Writer out) throws IOException {
                List<String> incs = ViewUtils.getStringListFromMap(INCLUDES_PROPERTY, arguments, null)
                List<String> excs = ViewUtils.getStringListFromMap(EXCLUDES_PROPERTY, arguments)
                Map map = (Map) object
                boolean entryRendered = false

                out.append OPEN_BRACE
                for (entry in map.entrySet()) {
                    if (!simpleIncludeExcludeSupport.shouldInclude(incs, excs, entry.key.toString())) {
                        continue
                    }

                    if (entryRendered) {
                        out.append COMMA
                    }
                    out.append(JsonOutput.toJson(entry.key.toString()))
                    out.append(COLON)
                    def value = entry.value
                    if (value instanceof Iterable) {
                        getIterableWritable(value, arguments, customizer, processedObjects, entry.key.toString() + ".").writeTo(out)
                    } else {
                        handleValue(value, out, arguments, customizer, processedObjects, entry.key.toString() + ".")
                    }
                    entryRendered = true
                }
                out.append CLOSE_BRACE
                return out
            }
        }
    }

    protected void handleValue(Object value, Writer out, Map arguments, Closure customizer, Map<Object, Writable> processedObjects, String path = "") {
        if (isSimpleValue(value)) {
            out.append(generator.toJson((Object) value))
        } else {
            renderTemplateOrDefault(value, arguments, customizer, processedObjects, path).writeTo(out)
        }
    }

    @Override
    Writable render(Object object, Map arguments = Collections.emptyMap(), @DelegatesTo(StreamingJsonDelegate) Closure customizer = null) {

        JsonView jsonView = (JsonView) view
        def binding = jsonView.getBinding()
        JsonGenerator generator = getGenerator()
        Map<Object, Writable> processedObjects = initializeProcessedObjects(binding)
        if (object instanceof Iterable) {
            return getIterableWritable((Iterable) object, arguments, customizer, processedObjects)
        } else if (object instanceof Map) {
            return getMapWritable((Map) object, arguments, customizer, processedObjects)
        } else if (object instanceof Throwable) {
            Throwable e = (Throwable) object
            List<Object> stacktrace = getJsonStackTrace(e)
            return new Writable() {
                @Override
                Writer writeTo(Writer out) throws IOException {
                    new StreamingJsonBuilder(out, generator).call {
                        StreamingJsonDelegate jsonDelegate = (StreamingJsonDelegate) getDelegate()
                        jsonDelegate.call("message", e.message)
                        jsonDelegate.call("stacktrace", stacktrace)
                    }
                    return out
                }
            }
        } else {
            return renderTemplateOrDefault(object, arguments, customizer, processedObjects)
        }
    }

    protected Map<Object, Writable> initializeProcessedObjects(Binding binding) {
        Map<Object, Writable> processedObjects

        if (binding.hasVariable(PROCESSED_OBJECT_VARIABLE)) {
            processedObjects = (Map<Object, Writable>) binding.getVariable(PROCESSED_OBJECT_VARIABLE)
        } else {
            processedObjects = new LinkedHashMap<Object, Writable>()
            binding.setVariable(PROCESSED_OBJECT_VARIABLE, processedObjects)
        }
        processedObjects
    }

    protected void processSimple(StreamingJsonDelegate jsonDelegate, Object object, Map<Object, Writable> processedObjects, List<String> incs, List<String> excs, String path, Boolean renderNulls, Closure customizer = null) {

        if (!processedObjects.containsKey(object)) {
            processedObjects.put(object, NULL_OUTPUT)


            def declaringClass = object.getClass()
            def cpf = ClassPropertyFetcher.forClass(declaringClass)
            def descriptors = cpf.getPropertyDescriptors()
            IncludeExcludeSupport includeExcludeSupport = simpleIncludeExcludeSupport
            for (PropertyDescriptor desc in descriptors) {
                def readMethod = desc.readMethod
                if (readMethod != null && desc.writeMethod != null) {
                    def propertyName = desc.name
                    String qualified = "${path}${propertyName}"
                    if (includeExcludeSupport.shouldInclude(incs, excs, qualified)) {
                        def value = cpf.getPropertyValue(object, desc.name)
                        if (value != null) {
                            def propertyType = desc.propertyType
                            boolean isArray = propertyType.isArray()
                            if (isStringType(propertyType)) {
                                jsonDelegate.call propertyName, value.toString()
                            } else if (isSimpleType(propertyType, value)) {
                                jsonDelegate.call propertyName, value
                            } else if (isArray || Iterable.isAssignableFrom(propertyType)) {
                                Class componentType

                                if (isArray) {
                                    componentType = propertyType.componentType
                                } else {
                                    componentType = getGenericType(declaringClass, desc)
                                }

                                if (!Object.is(componentType) && MappingFactory.isSimpleType(componentType.name) || componentType.isEnum()) {
                                    jsonDelegate.call(propertyName, value)
                                } else {
                                    Iterable iterable = isArray ? value as List : (Iterable) value
                                    jsonDelegate.call(propertyName, getIterableWritable(iterable) { Object o, Writer out ->
                                        if (isStringType(o.class)) {
                                            out.append(o.toString())
                                        } else if (isSimpleType(o.class, o)) {
                                            out.append(JsonOutput.toJson((Object) o))
                                        } else {
                                            out.append OPEN_BRACE
                                            processSimple(new StreamingJsonDelegate(out, true), o, processedObjects, incs, excs, "${path}${propertyName}.", renderNulls)
                                            out.append CLOSE_BRACE
                                        }
                                    })
                                }
                            } else {
                                if (!processedObjects.containsKey(value)) {
                                    jsonDelegate.call(propertyName) {
                                        if (delegate instanceof StreamingJsonDelegate) {
                                            jsonDelegate = (StreamingJsonDelegate) getDelegate()
                                        }
                                        processSimple(jsonDelegate, value, processedObjects, incs, excs, "${path}${propertyName}.", renderNulls)
                                    }
                                }
                            }
                        } else if (renderNulls) {
                            jsonDelegate.call(propertyName, NULL_OUTPUT)
                        }
                    }
                }
            }

            // it is protected
            jsonDelegate.setProperty("first", false)

            if (customizer != null) {
                customizer.setDelegate(jsonDelegate)
                if (customizer.maximumNumberOfParameters == 1) {
                    customizer.call(object)
                } else {
                    customizer.call()
                }
            }

        }
    }


    protected boolean isSimpleValue(Object value) {
        if (value == null) {
            return true
        }

        Class propertyType = value.getClass()
        return MappingFactory.isSimpleType(propertyType.getName()) || (value instanceof Enum) || (value instanceof Map)

    }

    protected void process(StreamingJsonDelegate jsonDelegate, Object object, Map<Object, Writable> processedObjects, List<String> incs, List<String> excs, String path, boolean isDeep, boolean renderNulls, List<String> expandProperties = [], boolean includeAssociations = true, Closure customizer = null) {

        if (customizer != null) {
            customizer.setDelegate(jsonDelegate)
            if (customizer.maximumNumberOfParameters == 1) {
                customizer.call(object)
            } else {
                customizer.call()
            }
        }

    }


//    protected void processSimpleProperty(StreamingJsonDelegate jsonDelegate, PersistentProperty prop, String propertyName, Object value) {
//        if (prop instanceof Custom) {
//            def propertyType = value.getClass()
//            def template = renderTemplate(value, propertyType)
//            if (template != null) {
//                jsonDelegate.call(propertyName, template)
//                return
//            }
//        }
//
//        if (isStringType(prop.type)) {
//            jsonDelegate.call propertyName, value.toString()
//        } else if (prop.type.isEnum()) {
//            jsonDelegate.call propertyName, ((Enum) value).name()
//        } else if (value instanceof TimeZone) {
//            jsonDelegate.call propertyName, value.getID()
//        } else {
//            jsonDelegate.call(propertyName, value)
//        }
//
//    }
//

    @Override
    Writable render(Map arguments) {
        def template = arguments.template

        if (template) {
            // Reset the previous state in case were are rendering the same template
            if (view.binding.variables.containsKey(PROCESSED_OBJECT_VARIABLE)) {
                view.binding.variables.remove(PROCESSED_OBJECT_VARIABLE)
            }

            Map model = (Map) arguments.model ?: [:]
            def collection = arguments.containsKey('collection') ? (arguments.collection ?: []) : null
            def var = arguments.var ?: 'it'
            String templateName = template.toString()
            Template childTemplate

            if (childTemplate == null) {
                childTemplate = templateEngine.createTemplate(jsonViewTemplateEngine.process(templateName, new Context(Locale.ENGLISH, model as Map<String, Object>)))
            }

            if (childTemplate != null) {
                return new Writable() {

                    @Override
                    Writer writeTo(Writer out) throws IOException {
                        if (collection instanceof Iterable) {
                            Iterable iterable = (Iterable) collection
                            int size = iterable.size()
                            int i = 0
                            out.append OPEN_BRACKET
                            for (o in collection) {
                                model.put(var, o)
                                model.put(GrailsNameUtils.getPropertyName(o.class), o)
                                def writable = prepareWritable(childTemplate, model)
                                writable.writeTo(out)
                                if (++i != size) {
                                    out.append COMMA
                                }
                            }
                            out.append CLOSE_BRACKET
                        } else {
                            prepareWritable(childTemplate, model).writeTo(out)
                        }
                    }
                }
            } else {
                throw new ViewException("Template not found for name $template")
            }
        } else {
            return render((Object) arguments)
        }
    }

    protected void populateModelWithViewState(Map model) {
        def parentViewBinding = view.binding
        if (parentViewBinding.variables.containsKey(PROCESSED_OBJECT_VARIABLE)) {
            model.put(PROCESSED_OBJECT_VARIABLE, parentViewBinding.getVariable(PROCESSED_OBJECT_VARIABLE))
        }
    }

    protected Writable prepareWritable(Template childTemplate, Map model) {
        populateModelWithViewState(model)
        model ? childTemplate.make((Map) model) : childTemplate.make()
    }

    /**
     * Obtains a model value for the given name and type
     *
     * @param name The name
     * @param targetType The type
     * @return The model value or null if it doesn't exist
     */
    def <T> T model(String name, Class<T> targetType = Object) {
        def value = view.binding.variables.get(name)
        if (targetType.isInstance(value)) {
            return (T) value
        }
        return null
    }
}
