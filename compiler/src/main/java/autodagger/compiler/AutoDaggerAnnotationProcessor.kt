package autodagger.compiler

import autodagger.AutoComponent
import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.AutoSubcomponent
import autodagger.compiler.AutoDaggerAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import autodagger.compiler.addition.AdditionProcessing
import autodagger.compiler.component.ComponentProcessing
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.processorworkflow.Logger
import autodagger.compiler.processorworkflow.deliver
import autodagger.compiler.subcomponent.SubcomponentProcessing
import dagger.Subcomponent
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AutoDaggerAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private lateinit var elements: Elements
    private lateinit var types: Types
    private lateinit var errors: Errors
    private lateinit var filer: Filer
    private val state: State = State()
    private var stop: Boolean = false

    init {
        // don't forget to disable logging before releasing
        // find a way to have the boolean set automatically via gradle
        Logger.init("AutoDagger2 Processor", false)
    }

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

        with(AdditionProcessing(elements, types, errors, state)) {
            process(
                roundEnv.getElementsAnnotatedWith(AutoInjector::class.java),
                AutoInjector::class.java,
                roundEnv
            )

            process(
                roundEnv.getElementsAnnotatedWith(AutoExpose::class.java),
                AutoExpose::class.java,
                roundEnv
            )

            if (errors.hasErrors()) {
                processingEnv.messager.deliver(errors)
                stop = true
                return false
            }
        }

        with(SubcomponentProcessing(elements, types, errors, state)) {
            process(
                roundEnv.getElementsAnnotatedWith(AutoSubcomponent::class.java),
                AutoSubcomponent::class.java,
                roundEnv
            )

            process(
                roundEnv.getElementsAnnotatedWith(Subcomponent::class.java),
                Subcomponent::class.java,
                roundEnv
            )

            if (errors.hasErrors()) {
                processingEnv.messager.deliver(errors)
                stop = true
                return false
            }

            specs.forEach { it.writeTo(filer) }
        }

        with(ComponentProcessing(elements, types, errors, state)) {
            process(
                roundEnv.getElementsAnnotatedWith(AutoComponent::class.java),
                AutoComponent::class.java,
                roundEnv
            )

            if (errors.hasErrors()) {
                processingEnv.messager.deliver(errors)
                stop = true
                return false
            }

            specs.forEach { it.writeTo(filer) }
        }

        return false
    }

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes() = setOf(
        AutoInjector::class.java.name,
        AutoExpose::class.java.name,
        AutoSubcomponent::class.java.name,
        Subcomponent::class.java.name,
        AutoComponent::class.java.name
    )
}
