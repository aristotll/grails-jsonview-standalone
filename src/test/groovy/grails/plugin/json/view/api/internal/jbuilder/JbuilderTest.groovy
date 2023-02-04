package grails.plugin.json.view.api.internal.jbuilder


import grails.plugin.json.view.test.JsonViewTest
import spock.lang.Specification


/**
 * @author cheng.yao
 * @date 2023/2/4
 */
class JbuilderTest extends Specification implements JsonViewTest {


    def "test simple method missing"() {
        when:
        def result = render(template: "jbuilder")

        then:
        result["test"] == "test"
    }
}
