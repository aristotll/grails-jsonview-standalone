package grails.plugin.json.view;

import groovy.json.DefaultJsonGenerator;
import groovy.json.JsonGenerator;

/**
 * @author cheng.yao
 * @date 2023/2/2
 */
public class NewDefaultJsonGenerator extends DefaultJsonGenerator {
    public NewDefaultJsonGenerator() {
        super(new JsonGenerator.Options());
    }
}
