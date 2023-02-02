package grails.views;

import java.io.Serializable;

/**
 * An interface that represents an exception that is capable of providing more information about the source code
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface SourceCodeAware extends Serializable {
    String getFileName();

    int getLineNumber();
}
