package grails.plugin.json.view

import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

class Circular {
    String name
}

/**
 * Created by graemerocher on 24/05/16.
 */
class TemplateInheritanceSpec extends Specification implements JsonViewTest {


    void "test circular rendering is handled"() {
        when:
        def result = render(template: 'circular/circular', model: [circular: new Circular(name: "Cantona")])

        then:
        notThrown(StackOverflowError)
        result.jsonText == '{"name":"Cantona"}'
    }

}
