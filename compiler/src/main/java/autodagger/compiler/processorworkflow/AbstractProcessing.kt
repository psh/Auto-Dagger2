package autodagger.compiler.processorworkflow

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractProcessing<T_Model, T_State>(
    protected val elements: Elements,
    protected val types: Types,
    protected val errors: Errors,
    protected val state: T_State
) {
    protected val specs: MutableList<T_Model> = mutableListOf()
    protected lateinit var processedAnnotation: Class<out Annotation>
    protected lateinit var roundEnvironment: RoundEnvironment

    abstract fun supportedAnnotations(): Set<Class<out Annotation>>

    fun process(
        annotationElements: Set<Element>,
        processedAnnotation: Class<out Annotation>,
        roundEnvironment: RoundEnvironment
    ) {
        this.processedAnnotation = processedAnnotation
        this.roundEnvironment = roundEnvironment

        processElements(annotationElements)
    }

    protected open fun processElements(annotationElements: Set<Element>) {
        annotationElements.forEach { e ->
            if (!processElement(e, errors.getFor(e))) {
                return
            }
        }
    }

    /**
     * @return true if element processed with success, false otherwise and it will stop
     * the processing
     */
    abstract fun processElement(element: Element, elementErrors: Errors.ElementErrors): Boolean

    open fun createComposer(): AbstractComposer<T_Model>? = null
}
