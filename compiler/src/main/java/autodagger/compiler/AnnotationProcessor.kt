package autodagger.compiler

import autodagger.compiler.AnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import autodagger.compiler.addition.AdditionProcessing
import autodagger.compiler.component.ComponentProcessing
import autodagger.compiler.subcomponent.SubcomponentProcessing
import autodagger.compiler.processorworkflow.AbstractProcessing
import autodagger.compiler.processorworkflow.AbstractProcessor
import autodagger.compiler.processorworkflow.Logger
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AnnotationProcessor : AbstractProcessor<State>() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override val processings: List<AbstractProcessing<*, *>> by lazy {
        listOf(
            AdditionProcessing(elements, types, errors, state),
            SubcomponentProcessing(elements, types, errors, state),
            ComponentProcessing(elements, types, errors, state)
        )
    }

    override val state = State()

    init {
        // don't forget to disable logging before releasing
        // find a way to have the boolean set automatically via gradle
        Logger.init("AutoDagger2 Processor", false)
    }

}
