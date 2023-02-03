package grails.views

import grails.views.compiler.ViewsTransform
import groovy.io.FileType
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.io.FileReaderSource

import java.util.concurrent.*

/**
 * A generic compiler for Groovy templates that are compiled into classes in production
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGroovyTemplateCompiler {

    @Delegate CompilerConfiguration configuration = new CompilerConfiguration()

    String packageName = ""
    File sourceDir
    ViewConfiguration viewConfiguration

    AbstractGroovyTemplateCompiler(ViewConfiguration configuration, File sourceDir) {
        this.viewConfiguration = configuration
        this.packageName = configuration.packageName
        this.sourceDir = sourceDir
        configureCompiler(this.configuration)
    }

    AbstractGroovyTemplateCompiler() {
    }

    protected CompilerConfiguration configureCompiler(CompilerConfiguration configuration) {
        configuration.compilationCustomizers.clear()

        ImportCustomizer importCustomizer = new ImportCustomizer()
        importCustomizer.addStarImports( viewConfiguration.packageImports )
        importCustomizer.addStaticStars( viewConfiguration.staticImports )

        configuration.addCompilationCustomizers(importCustomizer)
        configuration.addCompilationCustomizers(new ASTTransformationCustomizer(newViewsTransform()))
        return configuration
    }

    protected ViewsTransform newViewsTransform() {
        new ViewsTransform(viewConfiguration.extension)
    }

    void compile(List<File> sources) {
        
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2)
        CompletionService completionService = new ExecutorCompletionService(threadPool);
        
        try {
            Integer collationLevel = Runtime.getRuntime().availableProcessors()*2
            if(sources.size() < collationLevel) {
                collationLevel = 1
            }
            configuration.setClasspathList(classpath)
            String pathToSourceDir = sourceDir.canonicalPath
            def collatedSources = sources.collate(collationLevel)
            List<Future<Boolean>> futures = []
            for(int index=0;index < collatedSources.size();index++) {
                def sourceFiles = collatedSources[index]
                futures << completionService.submit({ ->
                    CompilerConfiguration configuration = new CompilerConfiguration(this.configuration)
                    for(int viewIndex=0;viewIndex < sourceFiles.size();viewIndex++) {
                        File source = sourceFiles[viewIndex]
                        configureCompiler(configuration)
                        CompilationUnit unit = new CompilationUnit(configuration)
                        String pathToSource = source.canonicalPath
                        String path = pathToSource - pathToSourceDir
                        String templateName = GenericGroovyTemplateResolver.resolveTemplateName(
                                packageName, path
                        )
                        unit.addSource(new SourceUnit(
                                templateName,
                                new FileReaderSource(source, configuration),
                                configuration,
                                unit.classLoader,
                                unit.errorCollector
                        ))
                        unit.compile()
                    }
                    return true
                } as Callable)
            }

            int pending = futures.size()
                
            while (pending > 0) {
                // Wait for up to 100ms to see if anything has completed.
                // The completed future is returned if one is found; otherwise null.
                // (Tune 100ms as desired)
                def completed = completionService.poll(100, TimeUnit.MILLISECONDS);
                if (completed != null) {
                    Boolean response = completed.get() as Boolean//need this to throw exceptions on main thread it seems
                    --pending;
                }
            } 
        }     
        finally {
                threadPool.shutdown()
        }
                
        

    }

    void compile(File...sources) {
        compile Arrays.asList(sources)
    }
}
