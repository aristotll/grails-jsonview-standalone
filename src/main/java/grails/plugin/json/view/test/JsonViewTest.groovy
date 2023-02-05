package grails.plugin.json.view.test


import grails.plugin.json.view.api.JsonView
import grails.plugin.json.view.api.internal.DefaultGrailsJsonViewHelper
import grails.views.JsonGroovyTemplateEngineImpl
import grails.views.ResolvableGroovyTemplateEngine
import groovy.json.JsonSlurper
import groovy.text.Template
import groovy.transform.CompileStatic
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

/**
 * A trait that test classes can implement to add support for easily testing JSON views
 *
 * @author Graeme Rocher
 * @since 1.1.0
 */

@CompileStatic
trait JsonViewTest {
    SpringTemplateEngine jsonViewTemplateEngine = DefaultGrailsJsonViewHelper.JSON_VIEW_TEMPLATE_ENGINE
    ResolvableGroovyTemplateEngine templateEngine = DefaultGrailsJsonViewHelper.GROOVY_TEMPLATE_ENGINE

    /**
     * Render a template for the given source
     *
     * @param source The raw source of the template
     * @param model The model
     *
     * @return The result
     */
    JsonRenderResult render(String source, Map model) {
        def template = templateEngine.createTemplate(source)
        return produceResult(template, model)
    }


    /**
     * Render a template for the given source
     *
     * @param source The raw source of the template
     * @param model The model
     *
     * @return The result
     */
    JsonRenderResult render(String source) {
        def template = templateEngine.createTemplate(source)
        return produceResult(template, Collections.emptyMap())
    }
    /**
     * Render one of the GSON views in the grails-app/views directory for the given arguments
     *
     * @param arguments The named arguments: 'template', 'view' and 'model'
     *
     * @return The render result
     */
    JsonRenderResult render(Map arguments) {
        String templateText
        if (arguments.template) {
            templateText = getJsonViewTemplateEngine().process(arguments.template as String,
                    new Context(Locale.ENGLISH, (arguments.model as Map<String, Object>) ?: Collections.<String, Object> emptyMap()))
        }
        if (templateText == null) {
            throw new IllegalArgumentException("a 'template' argument is required!")
        }
        def template = templateEngine.createTemplate(templateText)

        if (template == null) {
            throw new IllegalArgumentException("No view or template found for URI $templateText")
        }

        def model = arguments.model instanceof Map ? (Map) arguments.model : [:]
        return produceResult(template, model)
    }

    private JsonRenderResult produceResult(Template template, Map model) {
        JsonView writable = (JsonView) template.make(model)

        def result = new JsonRenderResult()
        def sw = new StringWriter()
        writable.writeTo(sw)
        def str = sw.toString()
        result.jsonText = str
        result.json = new JsonSlurper().parseText(str)
        return result
    }

}