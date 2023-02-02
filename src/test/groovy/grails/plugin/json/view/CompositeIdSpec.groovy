package grails.plugin.json.view

import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

/**
 * Created by jameskleeh on 2/9/17.
 */
class CompositeIdSpec extends Specification implements JsonViewTest {


    void "Test render domain object with a simple composite id"() {
        given:
        CompositeSimple simple = new CompositeSimple(first: "x", second: "y", age: 99)

        when:
        def result = render('''
import grails.plugin.json.view.*

model {
    CompositeSimple simple
}
json g.render(simple)
''', [simple: simple])

        then: "The result is correct"
        result.json == [first: "x", second: "y", age: 99]
    }


}


class CompositeSimple {

    String first
    String second

    Integer age

    static mapping = {
        id(composite: ['first', 'second'])
    }
}
