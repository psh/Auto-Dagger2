package autodagger.compiler.processorworkflow

import autodagger.compiler.State
import autodagger.compiler.processorworkflow.Errors.ElementErrors

abstract class ProcessingBuilder<T_Extractor : AbstractExtractor<T_Extractor, T_Model>, T_Model>(
    protected val extractor: T_Extractor,
    errors: Errors
) {
    protected val errors = ElementErrors(errors, extractor.element)

    abstract fun build(state: State, extractors: Set<T_Extractor>): T_Model
}
