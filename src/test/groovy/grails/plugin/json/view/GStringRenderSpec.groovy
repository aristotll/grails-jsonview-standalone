package grails.plugin.json.view

import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

/**
 * Created by graemerocher on 27/09/2016.
 */
class GStringRenderSpec extends Specification implements JsonViewTest {

    void "Test render an string with model init value"() {

        when:
        def renderResult = render('''
model{
    String value = 'abc'
}

json {
    example1 "abc"
    example2 "${value}"
    example3 "$value"
    example4 "$value".toString()
}
''')

        then:
        renderResult.json.example1 == 'abc'
        renderResult.json.example2 == 'abc'
        renderResult.json.example3 == 'abc'
        renderResult.json.example4 == 'abc'

    }

    void "Test render an exception type"() {

        when:"An exception is rendered"
        def renderResult = render('''
@groovy.transform.Field String value = 'abc'

json {
    example1 "abc"
    example2 "${value}"
    example3 "$value"
    example4 "$value".toString()
}
''')

        then:"The exception is rendered"
        renderResult.json.example1 == 'abc'
        renderResult.json.example2 == 'abc'
        renderResult.json.example3 == 'abc'
        renderResult.json.example4 == 'abc'

    }

}
