package grails.views.compiler

import grails.views.Views
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.trait.TraitComposer

import java.lang.reflect.Modifier

/**
 * Enhances view scripts with Trait behavior
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class ViewsTransform implements ASTTransformation, CompilationUnitAware {
    public static final String APPLIED = "grails.views.transform.APPLIED"
    public static final Parameter[] ZERO_PARAMETERS = new Parameter[0];

    final String extension
    String dynamicPrefix
    CompilationUnit compilationUnit

    ViewsTransform(String extension, String dynamicPrefix = null) {
        this.extension = extension
        this.dynamicPrefix = dynamicPrefix
    }

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        def classes = source.AST.classes


        def sourceName = source.name
        if(!sourceName.endsWith("_$extension") && (dynamicPrefix != null && !sourceName.startsWith(dynamicPrefix))) {
            return
        }
        for(cn in classes) {
            ClassNode classNode = (ClassNode)cn
            if(!classNode.getNodeMetaData(APPLIED)) {

                if ( classNode.isScript() ) {
                    if(classNode.hasPackageName()) {
                        System.err.println("WARN: GSON view ${sourceName} defines package, and should not. Please remove the package statement.")
                        classNode.setName(classNode.nameWithoutPackage)
                    }
                    TraitComposer.doExtendTraits(classNode, source, compilationUnit)


                    def modelTypesVisitor = new ModelTypesVisitor(source)
                    modelTypesVisitor.visitClass(classNode)
                    def runMethod = classNode.getMethod("run", ZERO_PARAMETERS)
                    def stm = runMethod.code
                    if(stm instanceof BlockStatement) {
                        BlockStatement bs = (BlockStatement)stm

                        def statements = bs.statements
                        Statement modelStatement = null
                        for(st in statements) {
                            if(st instanceof ExpressionStatement) {
                                Expression exp = ((ExpressionStatement)st).expression
                                if(exp instanceof MethodCallExpression) {
                                    MethodCallExpression mce = (MethodCallExpression)exp
                                    if(mce.methodAsString == 'model' && modelStatement == null) {
                                        def arguments = mce.getArguments()
                                        def args = arguments instanceof ArgumentListExpression ? ((ArgumentListExpression) arguments).getExpressions() : Collections.emptyList()
                                        if(args.size() == 1 && args[0] instanceof ClosureExpression) {
                                            modelStatement = st
                                        }
                                    }
                                    if(mce.methodAsString == 'json') {
                                        new HalCodeVisitorSupport(compilationUnit).visitMethodCallExpression(mce)
                                    }
                                }
                            }
                        }
                        if(modelStatement != null) {
                            statements.remove(modelStatement)
                        }
                    }


                    def modelTypesMap = new MapExpression()
                    for(entry in modelTypesVisitor.modelTypes) {
                        modelTypesMap.addMapEntryExpression( new MapEntryExpression(
                                new ConstantExpression(entry.key),
                                new ClassExpression(ClassHelper.make(entry.value.name))))
                    }
                    classNode.addField( new FieldNode(Views.MODEL_TYPES_FIELD, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, ClassHelper.make(Map).plainNodeReference, classNode.plainNodeReference, modelTypesMap))
                    classNode.putNodeMetaData(APPLIED, Boolean.TRUE)
                }
            }
        }
    }

    class ModelTypesVisitor extends ClassCodeVisitorSupport {

        final SourceUnit sourceUnit
        ClassNode classNode
        Map<String, ClassNode> modelTypes = [:]

        ModelTypesVisitor(SourceUnit sourceUnit) {
            this.sourceUnit = sourceUnit
        }

        @Override
        void visitClass(ClassNode node) {
            this.classNode = node
            super.visitClass(node)
        }

        @Override
        void visitField(FieldNode node) {
            super.visitField(node)
            modelTypes.put(node.name, node.type)
        }

        @Override
        void visitMethodCallExpression(MethodCallExpression call) {
            def methodName = call.getMethodAsString()
            def arguments = call.getArguments()

            if( methodName == "model" &&  (arguments instanceof ArgumentListExpression) ) {
                def args = ((ArgumentListExpression) arguments).getExpressions()
                if(args.size() == 1 ) {
                    def arg = args.get(0)
                    if(arg instanceof ClosureExpression) {
                        Statement body = ((ClosureExpression)arg).code
                        MapExpression map = new MapExpression()
                        if(body instanceof BlockStatement) {
                            for(Statement st in ((BlockStatement)body).getStatements()) {
                                if(st instanceof ExpressionStatement) {
                                    def expr = ((ExpressionStatement) st).expression
                                    if(expr instanceof DeclarationExpression) {
                                        DeclarationExpression declarationExpression = (DeclarationExpression) expr
                                        VariableExpression var = (VariableExpression) declarationExpression.leftExpression
                                        classNode.addProperty(var.name, Modifier.PUBLIC, var.type, declarationExpression.rightExpression, null, null)
                                        modelTypes[var.name] = var.type
                                        map.addMapEntryExpression(
                                                new MapEntryExpression(new ConstantExpression(var.name), new ClassExpression(var.type))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            classNode.putNodeMetaData(Views.MODEL_TYPES, modelTypes)
            // used by markup template engine
            classNode.putNodeMetaData("MTE.modelTypes", modelTypes)
            super.visitMethodCallExpression(call)
        }
    }

}
