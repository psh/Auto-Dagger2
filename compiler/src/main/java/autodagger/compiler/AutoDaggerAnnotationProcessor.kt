package autodagger.compiler

import autodagger.AutoComponent
import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.AutoSubcomponent
import autodagger.compiler.AutoDaggerAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import autodagger.compiler.addition.addition
import autodagger.compiler.addition.annotatedAdditionAnnotation
import autodagger.compiler.addition.annotatedAdditionMethod
import autodagger.compiler.component.componentAnnotations
import autodagger.compiler.component.writeTo
import autodagger.compiler.subcomponent.subcomponentAnnotations
import autodagger.compiler.subcomponent.writeTo
import com.google.gson.GsonBuilder
import dagger.Subcomponent
import java.io.PrintWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AutoDaggerAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        private val PROCESSORS = mapOf(
            AutoInjector::class.java to arrayOf(
                annotatedAdditionAnnotation,
                annotatedAdditionMethod,
                addition
            ),
            AutoExpose::class.java to arrayOf(
                annotatedAdditionAnnotation,
                annotatedAdditionMethod,
                addition
            ),
            AutoComponent::class.java to arrayOf(
                componentAnnotations
            ),
            AutoSubcomponent::class.java to arrayOf(
                subcomponentAnnotations
            )
        )
    }

    private val state: State = State()
    private var stop: Boolean = false

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes() = setOf(
        AutoInjector::class.java.name,
        AutoExpose::class.java.name,
        AutoSubcomponent::class.java.name,
        Subcomponent::class.java.name,
        AutoComponent::class.java.name
    )

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (stop) return false

        with(state) {
            processingEnv = this@AutoDaggerAnnotationProcessor.processingEnv
            roundEnvironment = roundEnv
            diagnosticReport["options"] = processingEnv.options

            gatherAnnotationConfiguration()

            writeDaggerCode()

            writeDiagnosticReport()

            if (errors.hasErrors()) {
                processingEnv.messager.deliver(errors)
                stop = true
                return false
            }
        }

        return false
    }

    private fun State.gatherAnnotationConfiguration() {
        val beforeGatherConfigurationStep = System.currentTimeMillis()
        PROCESSORS.forEach { (k, v) -> k.process(state, *v) }
        timing(
            "Gathering Configuration",
            "${System.currentTimeMillis() - beforeGatherConfigurationStep} ms"
        )
    }

    private fun State.writeDaggerCode() {
        val beforeWriteDaggerCodeStep = System.currentTimeMillis()
        subcomponentExtractors.values.toSet().apply {
            asSequence().map { it.buildModel(state) }
                .forEach { it.writeTo(state, processingEnv.filer) }
        }

        componentExtractors.values.toSet().apply {
            componentExtractors.asSequence().map { it.value.buildModel(this) }
                .forEach { it.writeTo(state, this, processingEnv.filer) }
        }
        timing(
            "Writing Dagger Code",
            "${System.currentTimeMillis() - beforeWriteDaggerCodeStep} ms"
        )
    }

    private fun writeDiagnosticReport() {
        try {
            val reportFiler = processingEnv.filer.createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "annotation_processing_report.json"
            )
            val gson = GsonBuilder().setPrettyPrinting().create()
            val pw = PrintWriter(reportFiler.openOutputStream())
            pw.println(gson.toJson(state.diagnosticReport))
            pw.flush()
            pw.close()
            processingEnv.messager.printMessage(
                Diagnostic.Kind.NOTE,
                "==> Wrote: ${reportFiler.name}"
            )
        } catch (e: Exception) {
        }
    }

    private fun <T : Annotation> Class<T>.process(state: State, vararg processor: Processor) =
        state.roundEnvironment.getElementsAnnotatedWith(this)
            .forEach { e ->
                processor.forEach { it.invoke(state, this, e) }
            }
}

typealias Processor = State.(processedAnnotation: Class<out Annotation>, element: Element) -> Unit
