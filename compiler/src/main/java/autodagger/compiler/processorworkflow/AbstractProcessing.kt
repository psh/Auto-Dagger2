package autodagger.compiler.processorworkflow

import autodagger.compiler.State
import autodagger.compiler.processorworkflow.Errors.ElementErrors
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractProcessing<T_Model, T_Ext : AbstractExtractor<T_Ext, T_Model>>(
    protected val elements: Elements,
    protected val types: Types,
    protected val errors: Errors,
    protected val state: State,
    protected val extractors: MutableSet<T_Ext> = mutableSetOf(),
    val specs: MutableList<T_Model> = mutableListOf()
) {
    protected lateinit var processedAnnotation: Class<out Annotation>
    protected lateinit var roundEnvironment: RoundEnvironment

    fun process(
        annotationElements: Set<Element>,
        processedAnnotation: Class<out Annotation>,
        roundEnvironment: RoundEnvironment
    ) {
        this.processedAnnotation = processedAnnotation
        this.roundEnvironment = roundEnvironment

        annotationElements.forEach { e ->
            if (!processElement(e, ElementErrors(errors, e))) {
                return
            }
        }

        if (!errors.hasErrors()) {
            extractors.forEach { extractor ->
                val spec = extractor.createBuilder(errors)?.build(state, extractors)
                if (!errors.hasErrors()) {
                    spec?.let { specs.add(it) }
                } else {
                    return
                }
            }
        }
    }

    /**
     * @return true if element processed with success, false otherwise and it will stop
     * the processing
     */
    abstract fun processElement(element: Element, elementErrors: ElementErrors): Boolean
}
