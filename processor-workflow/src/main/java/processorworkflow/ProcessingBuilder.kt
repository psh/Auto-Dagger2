package processorworkflow

abstract class ProcessingBuilder<T_Extractor : AbstractExtractor, T_Model>(
    protected val extractor: T_Extractor,
    errors: Errors
) {
    protected val errors: Errors.ElementErrors = errors.getFor(extractor.element)
    protected abstract fun build(): T_Model
}
