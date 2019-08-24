package autodagger.compiler

import autodagger.compiler.addition.AdditionExtractor
import autodagger.compiler.component.ComponentExtractor
import autodagger.compiler.subcomponent.SubcomponentExtractor
import autodagger.compiler.utils.DiagnosticsSource
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

@Suppress("UNCHECKED_CAST")
data class State(
    val injectorExtractors: MutableMap<Element, AdditionExtractor> = mutableMapOf(),
    val exposeExtractors: MutableMap<Element, AdditionExtractor> = mutableMapOf(),
    val componentExtractors: MutableMap<Element, ComponentExtractor> = mutableMapOf(),
    val subcomponentExtractors: MutableMap<Element, SubcomponentExtractor> = mutableMapOf(),
    private val subcomponentsModules: MutableMap<TypeMirror, List<TypeMirror>> = mutableMapOf(),
    val errors: Errors = Errors(),
    val diagnosticReport: MutableMap<String, Any> = mutableMapOf()
) {
    lateinit var processingEnv: ProcessingEnvironment
    lateinit var roundEnvironment: RoundEnvironment

    fun addInjectorExtractor(extractor: AdditionExtractor): AdditionExtractor? {
        logExtractorDiagnostic("@AutoInject", extractor)
        return injectorExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addExposeExtractor(extractor: AdditionExtractor): AdditionExtractor? {
        logExtractorDiagnostic("@AutoExpose", extractor)
        return exposeExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addComponentExtractor(extractor: ComponentExtractor): ComponentExtractor? {
        logExtractorDiagnostic("@AutoComponent", extractor)
        return componentExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addSubcomponentExtractor(extractor: SubcomponentExtractor): SubcomponentExtractor? {
        logExtractorDiagnostic("@AutoSubcomponent", extractor)
        return subcomponentExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addSubcomponentModule(typeMirror: TypeMirror, modules: List<TypeMirror>) =
        subcomponentsModules.put(typeMirror, modules)

    fun getSubcomponentModules(typeMirror: TypeMirror) = when {
        !subcomponentsModules.containsKey(typeMirror) -> null
        else -> subcomponentsModules[typeMirror]
    }

    fun log(section: String, message: String) {
        val list = (diagnosticReport.getOrPut(
            "Messages",
            { mutableMapOf<String, Any>() }) as MutableMap<String, Any>).getOrPut(
            section,
            { mutableListOf<String>() }) as MutableList<String>
        list.add(message)
    }

    fun timing(key: String, message: String) = (diagnosticReport.getOrPut(
        "Timing",
        { mutableMapOf<String, Any>() }) as MutableMap<String, Any>).put(key, message)

    private fun logExtractorDiagnostic(key: String, extractor: DiagnosticsSource) =
        (diagnosticReport.getOrPut(
            key,
            { mutableMapOf<String, Any>() }) as MutableMap<String, Any>).put(
            extractor.element.simpleName.toString(),
            extractor.toDiagnostics()
        )
}

class Errors {
    val list = mutableListOf<Error>()

    fun addInvalid(element: Element?, reason: String, vararg format: String?): Boolean {
        list.add(
            Error(
                element,
                String.format("Invalid value: %s", String.format(reason, *format))
            )
        )
        return false
    }

    fun hasErrors(): Boolean = list.isNotEmpty()

    class Error(val element: Element?, val text: String)

    class ElementErrors(val parent: Errors, private val element: Element) {
        fun addInvalid(reason: String, vararg format: String?) {
            parent.addInvalid(element, reason, *format)
        }
    }
}

fun Messager.deliver(errors: Errors) = errors.list.forEach {
    printMessage(Diagnostic.Kind.ERROR, it.text, it.element)
}