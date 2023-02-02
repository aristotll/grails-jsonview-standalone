package grails.plugin.json.view.test

import spock.lang.Specification

class JsonViewTestSpec extends Specification implements JsonViewTest {

    void "Test render nested object"() {

        given:
        String source = '''
import grails.plugin.json.view.test.Customer

model {
    Customer customer
}

json g.render(customer)
'''

        when:
        def result = render source, [customer: new Customer(firstName: "John", lastName: "Doe", contactInfo: new ContactInfo())]

        then:
        result.jsonText == '''{"contactInfo":{},"firstName":"John","lastName":"Doe"}'''

        when:
        result = render source, [customer: new Customer(firstName: "John", lastName: "Doe", contactInfo: new ContactInfo(phoneNumber: "+123-234479343"))]

        then:
        result.jsonText == '''{"contactInfo":{"phoneNumber":"+123-234479343"},"firstName":"John","lastName":"Doe"}'''

    }

    void "Test render a raw GSON view"() {
        when: "A gson view is rendered"
        def result = render '''
model {
    String person
}
json.person {
    name person
    model person
}
''', [person: "æøå ÆØÅ"]

        then: "The json is correct"
        result.jsonText == '''{"person":{"name":"æøå ÆØÅ","model":"æøå ÆØÅ"}}'''
    }

    void "Test render a GSON view"() {
        when: "A gson view is rendered"
        def result = render(template: "/foo")

        then: "The json is correct"
        result.json.person.name == 'bob'
    }

    void "Test render a GSON template"() {
        when: "A gson view is rendered"
        def result = render(template: "/child", model: [age: 10])

        then: "The json is correct"
        result.json.name == 'Fred'
        result.json.age == 10
    }

    void "Test render a GSON view that generates a link"() {
        when: "A gson view is rendered"
        def result = render(view: "/linkingView")

        then: "The json is correct"
        result.json.person.name == 'bob'
        result.json.person.homepage == '/person'
    }
}


class Customer {
    String firstName
    String lastName
    ContactInfo contactInfo
}

class ContactInfo {
    String phoneNumber
}
