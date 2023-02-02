package org.grails.datastore.mapping.reflect;

import org.codehaus.groovy.runtime.MetaClassHelper;

import java.beans.Introspector;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class NameUtils {

    private static final String PROPERTY_SET_PREFIX = "set";
    private static final String PROPERTY_GET_PREFIX = "get";
    private static final String PROPERTY_IS_PREFIX = "is";

    public static final String DOLLAR_SEPARATOR = "$";



    /**
     * Retrieves the name of a setter for the specified property name
     * @param propertyName The property name
     * @return The setter equivalent
     */
    public static String getSetterName(String propertyName) {
        return PROPERTY_SET_PREFIX + capitalize(propertyName);
    }
    
    /**
     * Retrieves the name of a setter for the specified property name
     * @param propertyName The property name
     * @return The getter equivalent
     */
    public static String getGetterName(String propertyName) {
        return getGetterName(propertyName, false);
    }

    /**
     * Retrieves the name of a setter for the specified property name
     * @param propertyName The property name
     * @param useBooleanPrefix true if property is type of boolean
     * @return The getter equivalent
     */
    public static String getGetterName(String propertyName, boolean useBooleanPrefix) {
        String prefix = useBooleanPrefix ? PROPERTY_IS_PREFIX : PROPERTY_GET_PREFIX;
        return prefix + capitalize(propertyName);
    }

    /**
     * Get the class name, taking into account proxies
     *
     * @param clazz The class
     * @return The class name
     */
    public static String getClassName(Class clazz) {
        final String sn = clazz.getSimpleName();
        if(sn.contains(DOLLAR_SEPARATOR)) {
            return clazz.getSuperclass().getName();
        }
        return clazz.getName();
    }

    /**
     * Returns the property name for a getter or setter
     * @param getterOrSetterName The getter or setter name
     * @return The property name
     */
    public static String getPropertyNameForGetterOrSetter(String getterOrSetterName) {
        if (getterOrSetterName == null || getterOrSetterName.length() == 0) return null;

        if (getterOrSetterName.startsWith(PROPERTY_GET_PREFIX) || getterOrSetterName.startsWith(PROPERTY_SET_PREFIX)) {
            return decapitalize(getterOrSetterName.substring(3));
        } else if (getterOrSetterName.startsWith(PROPERTY_IS_PREFIX)) {
            return decapitalize(getterOrSetterName.substring(2));
        }
        return null;
    }

    /**
     * Converts class name to property name using JavaBean decapitalization
     *
     * @param name The class name
     * @return The decapitalized name
     */
    public static String decapitalize(String name) {
        return Introspector.decapitalize(name);
    }

    /**
     * Transforms the first character of a string into a lowercase letter
     * @param name String to be transformed
     * @return Original string with the first char as a lowercase letter
     */
    public static String decapitalizeFirstChar(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * Converts a property name to class name according to the JavaBean convention
     *
     * @param name The property name
     * @return The class name
     */
    public static String capitalize(String name) {
        return MetaClassHelper.capitalize(name);
    }
}
