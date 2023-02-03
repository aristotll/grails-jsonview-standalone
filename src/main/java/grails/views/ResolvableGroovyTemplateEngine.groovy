package grails.views

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import grails.views.compiler.ViewsTransform
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * A TemplateEngine that can resolve templates using the configured TemplateResolver
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@Slf4j
abstract class ResolvableGroovyTemplateEngine extends TemplateEngine {

    private static final WritableScriptTemplate NULL_ENTRY = new WritableScriptTemplate(null) {
        @Override
        Writable make() {}

        @Override
        Writable make(Map binding) {}

        @Override
        protected void initModelTypes(Class<? extends WritableScript> templateClass) {
        }
    }

    protected static final Cache<String, WritableScriptTemplate> cachedTemplates = Caffeine.newBuilder()
            .maximumSize(250)
            .build()

    private volatile int templateCounter

    /**
     * The class loader to use
     */
    final GroovyClassLoader classLoader

    /**
     * Whether to enable reloading
     */
    final boolean enableReloading

    final boolean shouldCache

    /**
     * Whether to reload views
     */
    protected CompilerConfiguration compilerConfiguration


    /**
     * Creates a ResolvableGroovyTemplateEngine for the given base class name and file extension
     *
     * @param baseClass baseClassName The base class name
     * @param extension The file extension
     */
    ResolvableGroovyTemplateEngine(ClassLoader classLoader, String baseClass) {
        this.enableReloading = false
        this.shouldCache = true

        this.compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.setScriptBaseClass(baseClass)
        this.classLoader = new GroovyClassLoader(classLoader, new CompilerConfiguration(compilerConfiguration))

    }

    @Override
    Template createTemplate(String templateText) throws CompilationFailedException, ClassNotFoundException, IOException {
        cachedTemplates.get(templateText, { (WritableScriptTemplate) super.createTemplate(it) })
    }

    @Override
    WritableScriptTemplate createTemplate(Reader reader) throws CompilationFailedException, ClassNotFoundException, IOException {
        def cc = new CompilerConfiguration(compilerConfiguration)
        prepareCustomizers(cc)
        // if we reach here, use a throw away child class loader for dynamic templates
        def fileName = getDynamicTemplatePrefix() + templateCounter++
        try {
            def clazz = new GroovyClassLoader(classLoader, cc).parseClass(new GroovyCodeSource(reader, fileName, GroovyShell.DEFAULT_CODE_BASE))
            return createTemplate(clazz)
        } catch (CompilationFailedException e) {
            throw new IllegalStateException(fileName, e)
        }
    }

    /**
     * Creates a template for the given template class
     *
     * @param cls The class
     * @return The template
     */
    protected WritableScriptTemplate createTemplate(Class<?> cls) {
        new WritableScriptTemplate(cls as Class<? extends WritableScript>)

    }

    abstract String getDynamicTemplatePrefix()

    protected void prepareCustomizers(CompilerConfiguration compilerConfiguration) {
        // this hack is required because of https://issues.apache.org/jira/browse/GROOVY-7560
        compilerConfiguration.compilationCustomizers.clear()


        def importCustomizer = new ImportCustomizer()
        compilerConfiguration.addCompilationCustomizers(
                importCustomizer,
                new ASTTransformationCustomizer(newViewsTransform())
        )
    }

    abstract protected ViewsTransform newViewsTransform();
}