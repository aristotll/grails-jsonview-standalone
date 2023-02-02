package grails.plugin.json.view

import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Issue
import spock.lang.Specification

class EmbeddedAssociationsSpec extends Specification implements JsonViewTest {

    void "Test render domain object with embedded associations"() {
        given: "A domain class with embedded associations"
        Person p = new Person(name: "Robert")
        p.homeAddress = new Address(postCode: "12345")
        p.otherAddresses = [new Address(postCode: "6789"), new Address(postCode: "54321")]
        p.nickNames = ['Rob', 'Bob']

        when: "A an instance with embedded assocations is rendered"
        def result = render('''
import grails.plugin.json.view.*

model {
    Person person
}
json g.render(person)
''', [person: p])

        then: "The result is correct"
        // 顺序有不一致的地方 不过先无视吧
        result.jsonText == '{"otherAddresses":[{"postCode":"6789"},{"postCode":"54321"}],"name":"Robert","nickNames":["Rob","Bob"],"homeAddress":{"postCode":"12345"}}'
    }

    @Issue("https://github.com/grails/grails-views/issues/171")
    void 'test render domain object with embedded associations and include'() {
        given: 'a domain class with embedded associations'
        Person p = new Person(name: "Robert")
        p.homeAddress = new Address(postCode: "12345")
        p.otherAddresses = [new Address(postCode: "6789"), new Address(postCode: "54321")]
        p.nickNames = ['Rob', 'Bob']

        when: 'an instance with embedded associations is rendered'
        def result = render('''
import grails.plugin.json.view.*

model {
    Person person
}
json g.render(person, [includes: ['name', 'homeAddress']])
''', [person: p])

        then: 'the result is correct'
        // 顺序有不一致的地方 不过先无视吧
        result.jsonText == '{"name":"Robert","homeAddress":{"postCode":"12345"}}'
    }

}


class Person {
    String name
    Address homeAddress
    List<Address> otherAddresses = []
    List<String> nickNames = []

    static embedded = ['homeAddress', 'otherAddresses']
}

class Address {
    String postCode
}