package grails.plugin.json.view.api.internal.jbuilder


import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.ArrayUtil

import static com.github.aristotll.jsonview.utils.MapUtils.deepMerge

/**
 * reimplementation of rails jbuilder syntax
 * @author cheng.yao
 * @date 2023/2/4
 */
@CompileStatic
class Jbuilder {
    // the rails impl is keep an attributes hash/array that will be used to generate json
    private Object attributes = [:]


    public static final Object BLANK = new Object()

    //   def set!(key, value = BLANK, *args, &block)
    def set_(String key, Object value = BLANK, Closure block = null, Object[] args = ArrayUtil.createArray()) {
        def result
        if (block) {
            if (_blank(value)) {
                //        # json.comments { ... }
                //        # { "comments": ... }
                //        _merge_block(key){ yield self }
                result = _merge_block(key) { block.call(this) }
            } else {
                //        # json.comments @post.comments { |comment| ... }
                //        # { "comments": [ { ... }, { ... } ] }
                result = _scope { array_((Iterable) value, block) }
            }
        } else {
            if (args) {
                if (value instanceof Iterable) {
                    //      # json.comments @post.comments, :content, :created_at
                    //      # { "comments": [ { "content": "hello", "created_at": "..." }, { "content": "world", "created_at": "..." } ] }
                    result = _scope { array_(value, null, args) }
                } else {
                    //      # json.author @post.creator, :name, :email_address
                    //      # { "author": { "name": "David", "email_address": "david@loudthinking.com" } }
                    result = _merge_block(key) { extract_(value, args) }
                }
            } else {
                if (value instanceof Jbuilder) {
                    //        # json.age 32
                    //        # json.person another_jbuilder
                    //        # { "age": 32, "person": { ...  }
                    result = value.attributes_()
                } else {
                    if (value instanceof Closure) {
                        result = _merge_block(key) { value.call(this) }
                    } else {
                        //        # json.age 32
                        //        # { "age": 32 }
                        result = value
                    }
                }
            }
        }
        _set_value key, result
    }

    def set_(String key, Object value = BLANK, Object... args) {
        set_(key, value, null, args)
    }

    def methodMissing(String name, def args) {
        Object[] typedArgs = (Object[]) args
        if (typedArgs) {
            def last = typedArgs[-1]
            def lastClosure = last instanceof Closure

            def length = typedArgs.length
            if (length == 1) {
                if (lastClosure) {
                    set_(name, BLANK, last as Closure)
                } else {
                    set_(name, typedArgs[0])
                }
            } else if (length == 2) {
                if (lastClosure) {
                    set_(name, typedArgs[0], last as Closure)
                } else {
                    set_(name, typedArgs[0], null, typedArgs[1])
                }
            } else {
                if (lastClosure) {
                    set_(name, typedArgs[0], last as Closure, typedArgs[1..length - 2].toArray())
                } else {
                    set_(name, typedArgs[0], null, typedArgs[1..length - 1].toArray())
                }
            }
        } else {
            set_(name)
        }
    }

//   # Turns the current element into an array and yields a builder to add a hash.
    //  #
    //  # Example:
    //  #
    //  #   json.comments do
    //  #     json.child! { json.content "hello" }
    //  #     json.child! { json.content "world" }
    //  #   end
    //  #
    //  #   { "comments": [ { "content": "hello" }, { "content": "world" } ]}
    //  #
    //  # More commonly, you'd use the combined iterator, though:
    //  #
    //  #   json.comments(@post.comments) do |comment|
    //  #     json.content comment.formatted_content
    //  #   end

    //  def child!
    //    @attributes = [] unless ::Array === @attributes
    //    @attributes << _scope{ yield self }
    //  end
    def child_(Closure block) {
        if (!(attributes instanceof Collection)) {
            attributes = []
        }
        (Collection) attributes << _scope { block.call(this) }
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
    def array_(Iterable collection = [], Closure block = null, Object[] args = ArrayUtil.createArray()) {
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

    def array_(Iterable collection, Object... args) {
        array_(collection, null, args)
    }

    def array_(Object... args) {
        array_(args.toList(), null, ArrayUtil.createArray())
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
    def extract_(object, Object... attributes) {
        if (!object) {
            object = Collections.emptyList()
        }
        //   def _extract_hash_values(object, attributes)
        //    attributes.each{ |key| _set_value key, _format_keys(object.fetch(key)) }
        //  end
        //
        //  def _extract_method_values(object, attributes)
        //    attributes.each{ |key| _set_value key, _format_keys(object.public_send(key)) }
        //  end
        attributes.each { key ->
            // object get property key
            def strKey = key.toString()
            _set_value(strKey, object[strKey])
        }
    }

    def call(object, Object... args) {
        if (args) {
            def last = args[-1]
            def lastClosure = last instanceof Closure
            if (lastClosure) {
                return array_((Iterable) object, last as Closure)
            }
        }
        extract_(object, args)
    }

    //   # Returns the nil JSON.
    def nil_() {
        attributes = null
    }

    def null_() {
        nil_()
    }

    Object attributes_() {
        return attributes
    }

    //   # Merges hash, array, or Jbuilder instance into current builder.
    def merge_(object) {
        def hash_or_array = object instanceof Jbuilder ? object.attributes : object
        attributes = _merge_values(attributes, hash_or_array)
    }


    private def _merge_block(key, Closure closure) {
        def current_value = _blank() ? BLANK : (attributes[key.toString()] ?: BLANK)
        if (current_value == null) {
            throw Errors.NullError.build(key)
        }
        def new_value = _scope { closure.call(this) }
        _merge_values(current_value, new_value)
    }


    private def _merge_values(current_value, updates) {
        if (_blank(updates)) {
            return current_value
        }
        if (_blank(current_value) || updates == null || !current_value && updates instanceof Collection) {
            return updates
        }
        if (current_value instanceof Iterable && updates instanceof Iterable) {
            return current_value + updates
        }
        if (current_value instanceof Map && updates instanceof Map) {
            // deep merge two map
            return deepMerge(current_value, updates)
        }
        throw Errors.MergeError.build(current_value, updates)
    }

    private def _set_value(key, value) {
        if (attributes == null) {
            throw Errors.NullError.build(key)
        }
        if (attributes instanceof Collection) {
            throw Errors.ArrayError.build(key)
        }
        if (value == null || value == BLANK) {
            return
        }
        if (_blank()) {
            attributes = [:]
        }
        attributes[key.toString()] = value
    }

    private def _map_collection(collection, Closure closure) {
        collection.collect { element ->
            _scope { closure.call(element) }
        }.findAll { it != BLANK }
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

    private boolean _blank(Object value = attributes) {
        BLANK == value
    }
}
