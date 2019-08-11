package autodagger.compiler.processorworkflow

import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractExtractor(
    val element: Element,
    protected val types: Types,
    protected val elements: Elements,
    errors: Errors
) {
    protected val errors: Errors.ElementErrors = errors.getFor(element)
    abstract fun extract()
}
