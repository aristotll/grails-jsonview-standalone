package com.github.aristotll.jsonview

import grails.plugin.json.view.api.JsonView
import grails.plugin.json.view.api.internal.DefaultGrailsJsonViewHelper
import grails.views.JsonGroovyTemplateEngineImpl
import grails.views.ResolvableGroovyTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
/**
 * @author cheng.yao
 * @date 2023/2/3
 */
@CompileStatic
class JsonViewer {
    SpringTemplateEngine jsonViewTemplateEngine = DefaultGrailsJsonViewHelper.JSON_VIEW_TEMPLATE_ENGINE

    ResolvableGroovyTemplateEngine templateEngine = new JsonGroovyTemplateEngineImpl()

    /**
     * Render a template for the given source
     *
     * @param source The raw source of the template
     * @param model The model
     *
     * @return The result
     */
    String render(String source, Map model) {
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
    String render(String source) {
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
    String render(Map arguments) {
        String templateText
        if (arguments.template) {
            templateText = getJsonViewTemplateEngine().process(arguments.template as String,
                    new Context(Locale.ENGLISH, arguments.model as Map<String, Object>))
        }
        if (templateText == null) {
            throw new IllegalArgumentException("a 'template' argument is required!")
        }
        def template = templateEngine.createTemplate(templateText)

        if (template == null) {
            throw new IllegalArgumentException("No template found for $templateText")
        }

        def model = arguments.model instanceof Map ? (Map) arguments.model : [:]
        return produceResult(template, model)
    }

    private static String produceResult(Template template, Map model) {
        JsonView writable = (JsonView) template.make(model)
        def sw = new StringWriter()
        writable.writeTo(sw)
        return sw.toString()
    }
}
