package autodagger.compiler

import autodagger.*
import autodagger.compiler.component.writeTo
import autodagger.compiler.subcomponent.writeTo
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("kapt.kotlin.generated")
class AutoDaggerAnnotationProcessor : AbstractProcessor() {
    companion object {
        val ANNOTATION_VISITORS = mapOf(
            AutoBinds::class.java to BINDS_VISITOR,
            AutoInjector::class.java to INJECT_VISITOR,
            AutoExpose::class.java to EXPOSE_VISITOR,
            AutoComponent::class.java to COMPONENT_VISITOR,
            AutoSubcomponent::class.java to SUBCOMPONENT_VISITOR
        )
    }

    private val state: State = State()
    private var stop: Boolean = false

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (stop || annotations.isEmpty()) return false

        with(state) {
            processingEnv = this@AutoDaggerAnnotationProcessor.processingEnv
            roundEnvironment = roundEnv

            gatherAnnotationConfiguration()

            writeSubcomponents()

            writeComponents()

            if (errors.hasErrors()) {
                processingEnv.messager.deliver(errors)
                stop = true
                return false
            }
        }

        return false
    }

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes() =
        ANNOTATION_VISITORS.keys.asSequence().map { it.name }.toSet()

    private fun State.gatherAnnotationConfiguration() =
        ANNOTATION_VISITORS.forEach { (k, v) -> k.visit(this, v) }

    private fun State.writeSubcomponents() = subcomponentExtractors.values.asSequence()
        .map { it.buildModel(this) }
        .forEach { it.writeTo(this, processingEnv.filer) }

    private fun State.writeComponents() = componentExtractors.values.toSet().apply {
        asSequence()
            .map { it.buildModel(this) }
            .forEach { it.writeTo(this@writeComponents, this, processingEnv.filer) }
    }

    private fun <T : Annotation> Class<T>.visit(state: State, visitor: Visitor) =
        state.roundEnvironment.getElementsAnnotatedWith(this).forEach { e ->
            visitor.invoke(state, this, e)
        }
}
