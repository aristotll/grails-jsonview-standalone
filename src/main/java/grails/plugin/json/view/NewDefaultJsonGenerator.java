package grails.plugin.json.view;

import grails.plugin.json.view.api.internal.jbuilder.Jbuilder;
import groovy.json.DefaultJsonGenerator;
import groovy.json.JsonGenerator;

/**
 * @author cheng.yao
 * @date 2023/2/2
 */
public class NewDefaultJsonGenerator extends DefaultJsonGenerator {
    public NewDefaultJsonGenerator() {
        super(new JsonGenerator.Options().addConverter(
                new Converter() {
                    @Override
                    public boolean handles(Class<?> type) {
                        return Jbuilder.class.isAssignableFrom(type);
                    }

                    @Override
                    public Object convert(Object value, String key) {
                        return ((Jbuilder) value).attributes_();
                    }
                }
        ));
    }
}
