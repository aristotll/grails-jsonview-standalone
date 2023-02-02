package grails.plugin.json.view

import grails.views.JsonGroovyTemplateEngineImpl
import groovy.json.JsonSlurper
import spock.lang.Specification

/**
 * Created by graemerocher on 21/08/15.
 */
class JsonGroovyTemplateEngineImplSpec extends Specification {
//    void "Test model with default value"() {
//        when: "An engine is created and a template parsed"
//        def templateEngine = new JsonGroovyTemplateEngineImpl()
//        def template = templateEngine.createTemplate('''
//model {
//    String foo = "bar"
//}
//
//json {
//    name foo
//}
//''')
//        def writer = new StringWriter()
//        template.make().writeTo(writer)
//        then:"The output is correct"
//        writer.toString() == '{"name":"bar"}'
//    }
    // 暂时不支持

    void "Test static compilation with collections"() {
        when: "An engine is created and a template parsed"
        def templateEngine = new JsonGroovyTemplateEngineImpl()
        def template = templateEngine.createTemplate('''
model {
    List<URL> urls
}

json urls, { URL url ->
    protocol url.protocol
}
''')
        def writer = new StringWriter()
        template.make(urls: [new URL("http://foo.com")]).writeTo(writer)
        then:"The output is correct"
        writer.toString() == '[{"protocol":"http"}]'
    }

    void "Test render with includes"() {
        when: "An engine is created and a template parsed"
        def templateEngine = new JsonGroovyTemplateEngineImpl()
        def template = templateEngine.createTemplate('''
import grails.plugin.json.view.*
model {
    Book book
}

json g.render(book, [includes:['title']])
''')
        def writer = new StringWriter()
        template.make(book: new Book(title:"The Stand")).writeTo(writer)
        then:"The output is correct"
        writer.toString() == '{"title":"The Stand"}'
    }


    void "Test static compilation"() {
        when:"An engine is created and a template parsed"
        def templateEngine = new JsonGroovyTemplateEngineImpl()
        def template = templateEngine.createTemplate('''
model {
    URL url
}
json.site {
    protocol url.protocol
}
''')

        def writer = new StringWriter()
        template.make(url: new URL("http://foo.com")).writeTo(writer)

        then:"The output is correct"
        writer.toString() == '{"site":{"protocol":"http"}}'

        when:"A template is compiled with a compilation error"
        template = templateEngine.createTemplate('''
model {
    URL url
}
json.site {
    protocol url.frotocol
}
''')
        writer = new StringWriter()
        template.make(url: new URL("http://foo.com")).writeTo(writer)

        then:"A compilation error is thrown"
        thrown ViewCompilationException
    }

    void "Test parsing a JSON view template"() {
        when:"An engine is created and a template parsed"
        def templateEngine = new JsonGroovyTemplateEngineImpl()
        def template = templateEngine.createTemplate('''
json.person {
    name "bob"
}
''')

        def writer = new StringWriter()
        template.make().writeTo(writer)

        then:"The output is correct"
        writer.toString() == '{"person":{"name":"bob"}}'
    }


}

class Book  {
    String title
    Set<Author> authors
}
class Author {
    String name
}