package grails.plugin.json.view

import functional.tests.Player
import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification

class NullRenderingSpec extends Specification implements JsonViewTest {

    void "test rendering nulls with a domain"() {
        given:
        def templateText = '''
import grails.plugin.json.view.*

model {
    functional.tests.Player player
}

json g.render(player)
'''

        when:
        def renderResult = render(templateText, [player: new Player()])

        then: "No fields are rendered because they are null"
        renderResult.jsonText == '{}'
    }

    void "test rendering nulls with a domain  renderNulls = true "() {
        given:
        def templateText = '''
import grails.plugin.json.view.*

model {
    functional.tests.Player player
}

json g.render(player, [renderNulls: true])
'''

        when:
        def renderResult = render(templateText, [player: new Player()])

        then: "No fields are rendered because they are null"
        renderResult.jsonText == '{"name":null}'
    }

    void "test rendering nulls with a map"() {
        given:
        def templateText = '''
model {
    Map map
}

json g.render(map)
'''

        when:
        def renderResult = render(templateText, [map: [foo: null, bar: null]])

        then: "Maps with nulls are rendered by default"
        renderResult.jsonText == '{"foo":null,"bar":null}'
    }

    void "test rendering nulls with a pogo"() {
        given:
        def templateText = '''
model {
    Object obj
}

json g.render(obj)
'''

        when:
        def renderResult = render(templateText, [obj: new Child2()])

        then: "No fields are rendered because they are null"
        renderResult.jsonText == '{}'
    }

    void "test rendering nulls with a pogo  renderNulls = true "() {
        given:
        def templateText = '''
model {
    Object obj
}

json g.render(obj, [renderNulls: true])
'''

        when:
        def renderResult = render(templateText, [obj: new Child2()])

        then:
        renderResult.jsonText == '{"name":null,"parent":null}'
    }

    static class Parent2 {

        String name

        List<Child2> children
    }

    static class Child2 {

        Parent2 parent
        String name

    }
}
