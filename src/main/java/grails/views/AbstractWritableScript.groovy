package grails.views

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A script that is writable
 *
 * @author Graeme Rocher
 */
@CompileStatic
abstract class AbstractWritableScript extends Script implements WritableScript {

    /**
     * A logger that can be used within views
     */
    protected static final Logger log = LoggerFactory.getLogger(AbstractWritableScript)

    Writer out
    /**
     * The source file
     */
    File sourceFile

    private Map<String, Class> modelTypes

    /**
     * @return The current writer
     */
    Writer getOut() {
        return out
    }

    @Override
    final Writer writeTo(Writer out) throws IOException {
        setOut(out)
        try {
            return doWrite(out)
        } catch (Throwable e) {
            throw new ViewRenderException("Error rendering view: ${e.message}", e, this)
        }
    }

    /**
     * Subclasses should implement to perform the write
     * @param writer The writer
     * @return The original writer or a wrapped version
     */
    abstract Writer doWrite(Writer writer)
}
