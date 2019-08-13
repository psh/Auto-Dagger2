package autodagger.compiler.processorworkflow

import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractAnnotationProcessor<T_State> : AbstractProcessor() {

    abstract val state: T_State
    abstract val processings: List<AbstractProcessing<*, *>>
    protected lateinit var elements: Elements
    protected lateinit var types: Types
    protected lateinit var errors: Errors
    private var stop: Boolean = false
    private lateinit var filer: Filer

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        filer = processingEnv.filer
        errors = Errors()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (stop) return false

        val composers = mutableListOf<AbstractComposer<*>>().apply {
            processings.forEach { processing ->
                processing.supportedAnnotations().forEach { annotation ->
                    val elements = roundEnv.getElementsAnnotatedWith(annotation)
                    processing.process(elements, annotation, roundEnv)
                    if (errors.hasErrors()) {
                        errors.deliver(processingEnv.messager)
                        stop = true
                        return false
                    }
                }
                processing.createComposer()?.let { add(it) }
            }
        }

        composers.map { it.compose() }.flatten().forEach {
            try {
                it.writeTo(filer)
            } catch (e: Exception) {
                val stackTrace = StringWriter()
                e.printStackTrace(PrintWriter(stackTrace))
            }
        }

        return false
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes() = processings.map {
        it.supportedAnnotations().map { annotation -> annotation.name }
    }.flatten().toSet()
}
