package grails.plugin.json.view

import grails.views.GenericViewConfiguration
import org.springframework.beans.BeanUtils

import java.beans.PropertyDescriptor

/**
 * Default configuration for JSON views
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JsonViewConfiguration implements GenericViewConfiguration {

    public static final String MODULE_NAME = "json"

    List<String> mimeTypes = ["JSON"]


    JsonViewConfiguration() {
        setExtension(JsonViewWritableScript.EXTENSION)
        setCompileStatic(true)
        setBaseTemplateClass(JsonViewWritableScript)
    }

    @Override
    String getViewModuleName() {
         MODULE_NAME
    }

    PropertyDescriptor[] findViewConfigPropertyDescriptor() {
        BeanUtils.getPropertyDescriptors(GenericViewConfiguration)
    }

}
