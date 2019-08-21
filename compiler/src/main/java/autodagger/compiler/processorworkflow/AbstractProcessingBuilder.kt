package autodagger.compiler.processorworkflow

import autodagger.compiler.State
import autodagger.compiler.processorworkflow.Errors.ElementErrors

abstract class AbstractProcessingBuilder<T_Extractor : AbstractExtractor<T_Extractor, T_Model>, T_Model>(
    protected val extractor: T_Extractor,
    globalErrors: Errors,
    protected val errors: ElementErrors = ElementErrors(globalErrors, extractor.element)
) {
    abstract fun build(state: State, extractors: Set<T_Extractor>): T_Model
}
