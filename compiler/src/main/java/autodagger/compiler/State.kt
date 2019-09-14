package autodagger.compiler

import autodagger.compiler.addition.AdditionExtractor
import autodagger.compiler.binds.BindsExtractor
import autodagger.compiler.component.ComponentExtractor
import autodagger.compiler.subcomponent.SubcomponentExtractor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

@Suppress("UNCHECKED_CAST")
class State(
    val bindingExtractors: MutableList<BindsExtractor> = mutableListOf(),
    val injectorExtractors: MutableMap<Element, AdditionExtractor> = mutableMapOf(),
    val exposeExtractors: MutableMap<Element, AdditionExtractor> = mutableMapOf(),
    val componentExtractors: MutableMap<Element, ComponentExtractor> = mutableMapOf(),
    val subcomponentExtractors: MutableMap<Element, SubcomponentExtractor> = mutableMapOf(),
    val errors: Errors = Errors()
) {
    private val subcomponentsModules: MutableMap<TypeMirror, List<TypeMirror>> = mutableMapOf()
    lateinit var processingEnv: ProcessingEnvironment
    lateinit var roundEnvironment: RoundEnvironment

    fun addBindingExtractor(extractor: BindsExtractor) {
        bindingExtractors.add(extractor)
    }

    fun addInjectorExtractor(extractor: AdditionExtractor) {
        if (extractor.targetTypeMirrors.isNotEmpty())
            injectorExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addExposeExtractor(extractor: AdditionExtractor) {
        if (extractor.targetTypeMirrors.isNotEmpty())
            exposeExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addComponentExtractor(extractor: ComponentExtractor) {
        componentExtractors.putIfAbsent(extractor.element, extractor)
    }

    fun addSubcomponentExtractor(extractor: SubcomponentExtractor) {
        subcomponentExtractors.putIfAbsent(extractor.element, extractor)
        if (extractor.modulesTypeMirrors.isNotEmpty()) {
            subcomponentsModules[extractor.element.asType()] = extractor.modulesTypeMirrors
        }
    }

    fun subcomponentModulesOf(typeMirror: TypeMirror) = when {
        !subcomponentsModules.containsKey(typeMirror) -> emptyList()
        else -> subcomponentsModules[typeMirror]!!
    }
}

class Errors {
    val list = mutableListOf<Error>()

    fun addInvalid(element: Element?, reason: String, vararg format: String?): Boolean {
        list.add(
            Error(
                element,
                "Invalid value: %s".format(reason.format(*format))
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
