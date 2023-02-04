package grails.plugin.json.view.api.internal.jbuilder


import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.ArrayUtil
/**
 * reimplementation of rails jbuilder syntax
 * @author cheng.yao
 * @date 2023/2/4
 */
@CompileStatic
class Jbuilder {
    // the rails impl is keep an attributes map that will be used to generate json

//    private final JsonView view
    private Object attributes

    Object attributes_() {
        return attributes
    }

//    Jbuilder(JsonView view) {
//        this.view = view
//    }

    def methodMissing(String name, def args) {
        Object[] typedArgs = (Object[]) args
        if (typedArgs) {
            def last = typedArgs[-1]
            def lastClosure = last instanceof Closure
            if (typedArgs.length == 1) {
                if (lastClosure) {
                    set_(name, BLANK, last as Closure)
                } else {
                    set_(name, typedArgs[0])
                }
            } else if (typedArgs.length == 2) {
                if (lastClosure) {
                    set_(name, typedArgs[0], last as Closure)
                } else {
                    set_(name, typedArgs[0], null, typedArgs[1])
                }
            } else {
                if (lastClosure) {
                    set_(name, typedArgs[0], last as Closure, typedArgs[1..-2])
                } else {
                    set_(name, typedArgs[0], null, typedArgs[1..-1])
                }
            }
        } else {
            set_(name)
        }
    }

    public static final Object BLANK = new Object()

    //   def set!(key, value = BLANK, *args, &block)
    def set_(String key, Object value = BLANK, Closure block = null, Object[] args = ArrayUtil.createArray()) {
        def result
        //    result = if ::Kernel.block_given?
        if (block) {
            if (_blank(value)) {
                //        # json.comments { ... }
                //        # { "comments": ... }
                //        _merge_block(key){ yield self }
                result = _merge_block(key) { block.call(this) }
            } else {
                //        # json.comments @post.comments { |comment| ... }
                //        # { "comments": [ { ... }, { ... } ] }
                result = _scope { array_ value, block }
            }
        } else {
            //    elsif args.empty?
            if (!args) {
                if (value instanceof Jbuilder) {
                    //        # json.age 32
                    //        # json.person another_jbuilder
                    //        # { "age": 32, "person": { ...  }
                    result = value.attributes_()
                } else {
                    //        # json.age 32
                    //        # { "age": 32 }
                    result = value
                }
            } else {
                if (value instanceof Iterable) {
                    //      # json.comments @post.comments, :content, :created_at
                    //      # { "comments": [ { "content": "hello", "created_at": "..." }, { "content": "world", "created_at": "..." } ] }
                    result = _scope { array_(value, null, args) }
                } else {
                    //      # json.author @post.creator, :name, :email_address
                    //      # { "author": { "name": "David", "email_address": "david@loudthinking.com" } }
                    result = _merge_block(key) { extract_(value, args) }
                }
            }
        }
        _set_value key, result
    }

    private def _set_value(key, value) {
        if (key == null) {
            throw Errors.NullError.build(key)
        }
        if (key instanceof Iterable) {
            throw Errors.ArrayError.build(key)
        }
        if (value == null || value == BLANK) {
            return
        }
        if (attributes == null) {
            attributes = [:]
        }
        attributes[key.toString()] = value
    }

    private boolean _blank(Object value = attributes) {
        BLANK == value
    }

    private Object _scope(Closure closure) {
        def parentAttributes = attributes
        attributes = BLANK
        try {
            closure.call()
            return attributes
        } finally {
            attributes = parentAttributes
        }
    }

    //   # Turns the current element into an array and iterates over the passed collection, adding each iteration as
    //  # an element of the resulting array.
    //  #
    //  # Example:
    //  #
    //  #   json.array!(@people) do |person|
    //  #     json.name person.name
    //  #     json.age calculate_age(person.birthday)
    //  #   end
    //  #
    //  #   [ { "name": David", "age": 32 }, { "name": Jamie", "age": 31 } ]
    //  #
    //  # You can use the call syntax instead of an explicit extract! call:
    //  #
    //  #   json.(@people) { |person| ... }
    //  #
    //  # It's generally only needed to use this method for top-level arrays. If you have named arrays, you can do:
    //  #
    //  #   json.people(@people) do |person|
    //  #     json.name person.name
    //  #     json.age calculate_age(person.birthday)
    //  #   end
    //  #
    //  #   { "people": [ { "name": David", "age": 32 }, { "name": Jamie", "age": 31 } ] }
    //  #
    //  # If you omit the block then you can set the top level array directly:
    //  #
    //  #   json.array! [1, 2, 3]
    //  #
    //  #   [1,2,3]
    def array_(collection = [], Closure block = null, Object[] args = ArrayUtil.createArray()) {
        //  def array!(collection = [], *attributes, &block)
        //    array = if collection.nil?
        //      []
        //    elsif ::Kernel.block_given?
        //      _map_collection(collection, &block)
        //    elsif attributes.any?
        //      _map_collection(collection) { |element| extract! element, *attributes }
        //    else
        //      _format_keys(collection.to_a)
        //    end
        //
        //    @attributes = _merge_values(@attributes, array)
        //  end
        def array
        if (collection == null) {
            array = []
        } else if (block) {
            array = _map_collection(collection, block)
        } else if (args) {
            array = _map_collection(collection) { element ->
                extract_(element, args)
            }
        } else {
            array = collection
        }
        attributes = _merge_values(attributes, array)
    }

    private def _merge_values(current_value, updates) {
        if (_blank(updates)) {
            return current_value
        }
        if (_blank(current_value) || updates == null || !current_value && updates instanceof Iterable) {
            return updates
        }
        if (current_value instanceof Iterable && updates instanceof Iterable) {
            return current_value + updates
        }
        if (current_value instanceof Map && updates instanceof Map) {
            return current_value + updates
        }
        throw Errors.MergeError.build(current_value, updates)
    }

    private def _map_collection(collection, Closure closure) {
        collection.collect { element ->
            _scope { closure.call(element) }
        }.findAll { it != BLANK }
    }

    //   # Extracts the mentioned attributes or hash elements from the passed object and turns them into attributes of the JSON.
    //  #
    //  # Example:
    //  #
    //  #   @person = Struct.new(:name, :age).new('David', 32)
    //  #
    //  #   or you can utilize a Hash
    //  #
    //  #   @person = { name: 'David', age: 32 }
    //  #
    //  #   json.extract! @person, :name, :age
    //  #
    //  #   { "name": David", "age": 32 }, { "name": Jamie", "age": 31 }
    //  #
    //  # You can also use the call syntax instead of an explicit extract! call:
    //  #
    //  #   json.(@person, :name, :age)
    def extract_(object, Object[] attributes) {
        //   def _extract_hash_values(object, attributes)
        //    attributes.each{ |key| _set_value key, _format_keys(object.fetch(key)) }
        //  end
        //
        //  def _extract_method_values(object, attributes)
        //    attributes.each{ |key| _set_value key, _format_keys(object.public_send(key)) }
        //  end
        attributes.each { key ->
            // object get property key
            _set_value(key, object[key.toString()])
        }
    }

    private def _merge_block(key, Closure closure) {
        def current_value = _blank() ? BLANK : attributes[key.toString()]
        if (current_value == null) {
            throw Errors.NullError.build(key)
        }
        def new_value = _scope { closure.call(this) }
        _merge_values(current_value, new_value)
    }
}
