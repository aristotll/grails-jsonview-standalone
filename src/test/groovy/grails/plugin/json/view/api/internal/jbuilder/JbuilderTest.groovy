package grails.plugin.json.view.api.internal.jbuilder

import grails.plugin.json.view.test.JsonViewTest
import groovy.transform.Canonical
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import spock.lang.Specification

/**
 * @author cheng.yao
 * @date 2023/2/4
 */
class JbuilderTest extends Specification implements JsonViewTest {

    @Canonical
    static class Person {
        String name
        int age
    }
    // class NonEnumerable
    //  def initialize(collection)
    //    @collection = collection
    //  end
    //
    //  delegate :map, :count, to: :@collection
    //end

    static class NonEnumerable {
        @Delegate
        Collection collection

        NonEnumerable(collection) {
            this.collection = collection
        }

    }

    static class RelationMock implements Iterable {
        @Override
        Iterator iterator() {
            return [new Person('Bob', 30), new Person('Frank', 50)].iterator()
        }
    }


    private def jbuild(@ClosureParams(value = SimpleType.class,
            options = "grails.plugin.json.view.api.internal.jbuilder.Jbuilder")
                               Closure block) {
        def jbuilder = new Jbuilder()
        block.call(jbuilder)
        jbuilder.attributes_()
    }

    def "test simple method missing"() {
        when:
        def result = render(template: "jbuilder")

        then:
        result["test"] == "test"
    }

    def "test simple method missing2"() {
        when:
        def result = render('''
jb.test "test"
json {
    abc jb
}
''')

        then:
        result["abc"]["test"] == "test"
    }

    def "test single key"() {
        when:
        def result = jbuild { json ->
            json.content 'hello'
        }

        then:
        result["content"] == "hello"
    }

    def "test single key with false value"() {
        when:
        def result = jbuild { json ->
            json.content false
        }

        then:
        result["content"] == false
    }

    def "test single key with nil value"() {
        when:
        def result = jbuild { json ->
            json.content null
        }

        then:
        result == [:]
    }

    def "test multiple keys"() {
        when:
        def result = jbuild { json ->
            json.title 'hello'
            json.content 'world'
        }
        then:
        result["title"] == "hello"
        result["content"] == "world"
    }

    def "test extracting from object"() {
        when:
        def person = new Expando(name: 'David', age: 32)
        def result = jbuild { json ->
            json.extract_ person, 'name', 'age'
        }
        then:
        result["name"] == "David"
        result["age"] == 32
    }

    def "test extracting from object using call style"() {
        when:
        def person = new Expando(name: 'David', age: 32)
        def result = jbuild { json ->
            json(person, 'name', 'age')
        }
        then:
        result["name"] == "David"
        result["age"] == 32
    }

    def "test extracting from hash using call style"() {
        when:
        def person = [name: 'Jim', age: 34]
        def result = jbuild { json ->
            json(person, 'name', 'age')
        }
        then:
        result["name"] == "Jim"
        result["age"] == 34
    }

    def "test nesting single child with block"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.name 'David'
                json.age 32
            }
        }
        then:
        result["author"]["name"] == "David"
        result["author"]["age"] == 32
    }

    def "empty block handling"() {
        when:
        def result = jbuild { json ->
            json.foo 'bar'
            json.author {
            }
        }
        then:
        result["foo"] == "bar"
        !result.containsKey("author")
    }


    def "blocks are additive"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.name 'David'
            }
            json.author {
                json.age 32
            }
        }
        then:
        result["author"]["name"] == "David"
        result["author"]["age"] == 32
    }


    def "nested blocks are additive"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.name {
                    json.first 'David'
                }
            }
            json.author {
                json.name {
                    json.last 'Heinemeier Hansson'
                }
            }
        }
        then:
        result["author"]["name"]["first"] == "David"
        result["author"]["name"]["last"] == "Heinemeier Hansson"
    }


    def "support merge! method"() {
        when:
        def result = jbuild { json ->
            json.merge_(['foo': 'bar'])
        }
        then:
        result["foo"] == "bar"
    }

    def "support merge! method in a block"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.merge_('name': 'Pavel')
            }
        }
        then:
        result["author"]["name"] == "Pavel"
    }

    def "support merge! method with Jbuilder instance in a block"() {
        when:
        def obj = jbuild { json ->
            json.foo 'bar'
        }
        def result = jbuild { json ->
            json.author {
                json.merge_ obj
            }
        }
        then:
        result["author"]["foo"] == "bar"
    }

    def "nesting single child with inline extract"() {
        when:
        def person = new Person(name: 'David', age: 32)
        def result = jbuild { json ->
            json.author person, 'name', 'age'
        }
        then:
        result["author"]["name"] == "David"
        result["author"]["age"] == 32
    }


    def "nesting multiple children from array"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)]
        def result = jbuild { json ->
            json.comments comments, 'content'
        }
        then:
        result["comments"].first()["content"] == "hello"
        result["comments"].last()["content"] == "world"
    }

    def "nesting multiple children from array when child array is empty"() {
        when:
        def comments = []
        def result = jbuild { json ->
            json.name 'Parent'
            json.comments comments, 'content'
        }
        then:
        result["name"] == "Parent"
        result["comments"] == []
    }


    def "nesting multiple children from array with inline loop"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)]
        def result = jbuild { json ->
            json.comments comments, 'content'
        }
        then:
        result["comments"].first()["content"] == "hello"
        result["comments"].last()["content"] == "world"
    }

    def "handles nil-collections as empty arrays"() {
        when:
        def result = jbuild { json ->
            json.comments(null) {
                json.content it.content
            }
        }
        then:
        result["comments"] == []
    }

    def "nesting multiple children from array with inline loop and extract"() {
        when:
        def comments = new NonEnumerable([new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)])
        def result = jbuild { json ->
            json.comments comments, 'content', 'id'
        }
        then:
        result["comments"].first()["content"] == "hello"
        result["comments"].first()["id"] == 1
        result["comments"].last()["content"] == "world"
        result["comments"].last()["id"] == 2
    }

    def "nesting multiple children from a non-Enumerable that responds to #map with inline loop"() {
        when:
        def comments = new NonEnumerable([new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)])
        def result = jbuild { json ->
            json.comments comments, {
                json.content it.content
            }
        }
        then:
        result["comments"].first().keySet() == ['content'] as Set
        result["comments"].first()["content"] == "hello"
        result["comments"].last()["content"] == "world"
    }

    def "array nested inside nested hash"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.name 'David'
                json.age 32
                json.comments {
                    json.child_ {
                        json.content 'hello'
                    }
                    json.child_ {
                        json.content 'world'
                    }
                }
            }
        }
        then:
        result["author"]["comments"].first()["content"] == "hello"
        result["author"]["comments"].last()["content"] == "world"
    }

    def "array nested inside array"() {
        when:
        def result = jbuild { json ->
            json.comments {
                json.child_ {
                    json.authors {
                        json.child_ {
                            json.name 'david'
                        }
                    }
                }
            }
        }
        then:
        result["comments"].first()["authors"].first()["name"] == "david"
    }

    def "directly set an array nested in another array"() {
        when:
        def data = [[department: 'QA', not_in_json: 'hello', names: ['John', 'David']]]
        def result = jbuild { json ->
            json.array_ data, { object ->
                json.department object.department
                json.names {
                    json.array_ object.names
                }
            }
        }
        then:
        result[0]["names"].last() == "David"
        !result[0].containsKey("not_in_json")
    }

    def "nested jbuilder objects"() {
        when:
        def to_nest = jbuild { json ->
            json.nested_value 'Nested Test'
        }
        def result = jbuild { json ->
            json.value 'Test'
            json.nested to_nest
        }
        then:
        result["value"] == "Test"
        result["nested"]["nested_value"] == "Nested Test"
    }

    def "nested jbuilder object via set!"() {
        when:
        def to_nest = jbuild { json ->
            json.nested_value 'Nested Test'
        }
        def result = jbuild { json ->
            json.value 'Test'
            json.set_ 'nested', to_nest
        }
        then:
        result["value"] == "Test"
        result["nested"]["nested_value"] == "Nested Test"
    }

    def "top-level array"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)]
        def result = jbuild { json ->
            json.array_ comments, { comment ->
                json.content comment.content
            }
        }
        then:
        result.first()["content"] == "hello"
        result.last()["content"] == "world"
    }

    def "it allows using next in array block to skip value"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'skip', id: 2), new Expando(content: 'world', id: 3)]
        def result = jbuild { json ->
            json.array_ comments, { comment ->
                if (comment.id == 2) {
                    return
                }
                json.content comment.content
            }
        }
        then:
        result.size() == 2
        result.first()["content"] == "hello"
        result[1]["content"] == "world"
    }

    def "extract attributes directly from array"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)]
        def result = jbuild { json ->
            json.array_ comments, 'content', 'id'
        }
        then:
        result.first()["content"] == "hello"
        result.first()["id"] == 1
        result.last()["content"] == "world"
        result.last()["id"] == 2
    }

    def "empty top-level array"() {
        when:
        def comments = []
        def result = jbuild { json ->
            json.array_ comments, { comment ->
                json.content comment.content
            }
        }
        then:
        result == []
    }

    def "dynamically set a key/value"() {
        when:
        def result = jbuild { json ->
            json.set_ 'each', 'stuff'
        }
        then:
        result["each"] == "stuff"
    }

    def "dynamically set a key/nested child with block"() {
        when:
        def result = jbuild { json ->
            json.set_ 'author', {
                json.name 'David'
                json.age 32
            }
        }
        then:
        result["author"]["name"] == "David"
        result["author"]["age"] == 32
    }

    def "dynamically sets a collection"() {
        when:
        def comments = [new Expando(content: 'hello', id: 1), new Expando(content: 'world', id: 2)]
        def result = jbuild { json ->
            json.set_ 'comments', comments, 'content'
        }
        then:
        result["comments"].first().keySet() == ['content'] as Set
        result["comments"].first()["content"] == "hello"
        result["comments"].last()["content"] == "world"
    }

    def "query like object"() {
        when:
        def result = jbuild { json ->
            json.relations new RelationMock(), 'name', 'age'
        }
        then:
        result["relations"].size() == 2
        result["relations"].first()["name"] == "Bob"
        result["relations"].last()["age"] == 50
    }

    def "null!"() {
        when:
        def result = jbuild { json ->
            json.key 'value'
            json.null_()
        }
        then:
        result == null
    }

    def "null! in a block"() {
        when:
        def result = jbuild { json ->
            json.author {
                json.name 'David'
            }
            json.author {
                json.null_()
            }
        }
        then:
        result.containsKey("author")
        // ignore null so the value is still there
        result["author"]["name"] == 'David'
    }

    def "throws ArrayError when trying to add a key to an array"() {
        when:
        jbuild { json ->
            json.array_ 'foo', 'bar'
            json.fizz 'buzz'
        }
        then:
        thrown Errors.ArrayError
    }

    def "throws NullError when trying to add properties to null"() {
        when:
        jbuild { json ->
            json.null_()
            json.foo 'bar'
        }
        then:
        thrown Errors.NullError
    }

    def "throws MergeError when trying to merge array with hash"() {
        when:
        jbuild { json ->
            json.name 'Daniel'
            json.merge_([])
        }
        then:
        thrown Errors.MergeError
    }


    def "throws MergeError when trying to merge hash with array"() {
        when:
        jbuild { json ->
            json.array_()
            json.merge_([:])
        }
        then:
        thrown Errors.MergeError
    }

    def "throws MergeError when trying to merge invalid objects"() {
        when:
        jbuild { json ->
            json.name 'Daniel'
            json.merge_('Nope')
        }
        then:
        thrown Errors.MergeError
    }
}
