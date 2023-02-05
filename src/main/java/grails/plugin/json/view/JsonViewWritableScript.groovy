package grails.plugin.json.view

import grails.plugin.json.view.api.JsonView
import grails.plugin.json.view.api.internal.jbuilder.Jbuilder
import grails.views.AbstractWritableScript
import groovy.json.JsonOutput
import groovy.json.StreamingJsonBuilder
import groovy.transform.CompileStatic

@CompileStatic
abstract class JsonViewWritableScript extends AbstractWritableScript implements JsonView {

    static final char OPEN_BRACE = '{';
    static final char CLOSE_BRACE = '}';
    public static final String EXTENSION = "gson"
    public static final String TYPE = "view.gson"

    Object root
    boolean inline = false

    @Override
    Writer doWrite(Writer out) {
        this.json = new StreamingJsonBuilder(out, this.generator)
        run()
        return out
    }


    /**
     * @param callable
     * @return
     */
    StreamingJsonBuilder json(@DelegatesTo(value = StreamingJsonBuilder.StreamingJsonDelegate, strategy = Closure.DELEGATE_FIRST) Closure callable) {
        this.root = callable
        if (inline) {
            def jsonDelegate = new StreamingJsonBuilder.StreamingJsonDelegate(out, true, generator)
            callable.setDelegate(jsonDelegate)
            callable.call()
        } else {
            json.call callable
        }
        return json
    }

    StreamingJsonBuilder json(Iterable iterable) {
        this.root = iterable
        json.call iterable.asList()
        return json
    }

    StreamingJsonBuilder json(Map map) {
        this.root = map
        json.call map
        return json
    }

    /**
     * Print unescaped json directly
     *
     * @param unescaped The unescaped JSON produced from templates
     *
     * @return The json builder
     */
    StreamingJsonBuilder json(JsonOutput.JsonUnescaped unescaped) {
        print(unescaped.text)
        return json
    }

    /**
     * Print unescaped json directly
     *
     * @param writable The unescaped JSON produced from templates
     *
     * @return The json builder
     */
    StreamingJsonBuilder json(Writable writable) {
//        writable.setInline(inline)
        writable.writeTo(out)
        return json
    }

    StreamingJsonBuilder json(Jbuilder jbuilder) {
        out.write(generator.toJson(jbuilder.attributes_()))
        return json
    }

    /**
     * @param callable
     * @return
     */
    StreamingJsonBuilder json(Iterable iterable, @DelegatesTo(value = StreamingJsonBuilder.StreamingJsonDelegate, strategy = Closure.DELEGATE_FIRST) Closure callable) {
        json.call(iterable.asList(), callable)
        return json
    }

    StreamingJsonBuilder json(Object... args) {
        if (args.length == 1) {
            def val = args[0]
            if (val instanceof JsonOutput.JsonUnescaped) {
                this.json((JsonOutput.JsonUnescaped) val)
            } else if (val instanceof Writable) {
                this.json((Writable) val)
            } else {
                json.call val
            }
        } else {
            json.call args
        }
        return json
    }
}
