package grails.views

import grails.plugin.json.view.JsonViewWritableScript
import grails.views.compiler.ViewsTransform
import groovy.transform.CompileStatic
/**
 * @author cheng.yao
 * @date 2023/2/2
 */
@CompileStatic
class JsonGroovyTemplateEngineImpl extends ResolvableGroovyTemplateEngine {
    JsonGroovyTemplateEngineImpl() {
        super(Thread.currentThread().contextClassLoader,
                "grails.plugin.json.view.JsonViewWritableScript")
    }

    @Override
    String getDynamicTemplatePrefix() {
        return "me.json.generated.JsonViewWritableScript"
    }

    @Override
    protected ViewsTransform newViewsTransform() {
        return new ViewsTransform(JsonViewWritableScript.EXTENSION, getDynamicTemplatePrefix())
    }
}

