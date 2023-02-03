package com.github.aristotll.jsonview

import com.google.common.collect.Iterables
import com.google.common.collect.Iterators
import grails.plugin.json.view.test.JsonViewTest
import groovy.transform.Canonical
import spock.lang.Specification

/**
 * @author cheng.yao
 * @date 2023/2/3
 */
class JsonViewerTemplateTest extends Specification implements JsonViewTest {
    // reimpl https://github.com/rails/jbuilder/blob/5ee23caee43c038030afe1a02faf71162933cd06/test/jbuilder_template_test.rb

    // class Post < Struct.new(:id, :body, :author_name)
    //  def cache_key
    //    "post-#{id}"
    //  end
    //end
    // reimp ruby code in groovy
    @Canonical
    static class Post {
        int id
        String body
        String author_name

        String cache_key() {
            "post-${id}"
        }
    }
    //
    //class Racer < Struct.new(:id, :name)
    //  extend ActiveModel::Naming
    //  include ActiveModel::Conversion
    //end
    // reimp ruby code in groovy
    static class Racer {
        int id
        String name

        String model_name() {
            "Racer"
        }
    }
    //   AUTHORS = [ "David Heinemeier Hansson", "Pavel Pravosud" ].cycle
    Iterator<String> AUTHORS = Iterators.cycle(["David Heinemeier Hansson", "Pavel Pravosud"])
    //  POSTS   = (1..10).collect { |i| Post.new(i, "Post ##{i}", AUTHORS.next) }
    // reimp ruby code in groovy
    List<Post> POSTS = (1..10).collect { i ->
        new Post(i, "Post #${i}", AUTHORS.next()) )
    }

    def "basic template"() {
        expect:
//        result = render('json.content "hello"')
//        in rails jbuilder
        render('json { content  "hello" }').json["content"] == "hello"
    }

    //   test "partial by name with top-level locals" do
    //    result = render('json.partial! "partial", content: "hello"')
    //    assert_equal "hello", result["content"]
    //  end
    def "partial by name with top-level locals"() {
        expect:
        render(' json g.render(template: "jsonview/partial", model: [content: "hello"]) ')
                .json["content"] == "hello"
    }
    //   test "partial collection by name with symbol local" do
    //    result = render('json.partial! "post", collection: @posts, as: :post', posts: POSTS)
    //    assert_equal 10, result.count
    //    assert_equal "Post #5", result[4]["body"]
    //    assert_equal "Heinemeier Hansson", result[2]["author"]["last_name"]
    //    assert_equal "Pavel", result[5]["author"]["first_name"]
    //  end
    def "partial collection by name with symbol local"() {
        when:
        def obj = render(template: 'jsonview/post_collection', posts: POSTS).json
        then:
        obj.count == 10
    }
}
