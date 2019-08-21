package autodagger.compiler.processorworkflow

import autodagger.compiler.processorworkflow.Errors.ElementErrors
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractExtractor<T_Ext : AbstractExtractor<T_Ext, T_Model>, T_Model>(
    val element: Element,
    protected val types: Types,
    protected val elements: Elements,
    globalErrors: Errors,
    protected val errors: ElementErrors = ElementErrors(globalErrors, element)
) {
    open fun createBuilder(errors: Errors): AbstractProcessingBuilder<T_Ext, T_Model>? = null

    abstract fun extract()
}
