package autodagger.compiler

import autodagger.compiler.addition.AdditionExtractor
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

class State {

    val injectorExtractors = mutableMapOf<Element, AdditionExtractor>()
    val exposeExtractors = mutableMapOf<Element, AdditionExtractor>()
    private val subcomponentsModules = mutableMapOf<TypeMirror, List<TypeMirror>>()

    fun addInjectorExtractor(extractor: AdditionExtractor) =
        injectorExtractors.putIfAbsent(extractor.element, extractor)

    fun addExposeExtractor(extractor: AdditionExtractor) =
        exposeExtractors.putIfAbsent(extractor.element, extractor)

    fun addSubcomponentModule(typeMirror: TypeMirror, modules: List<TypeMirror>) =
        subcomponentsModules.put(typeMirror, modules)

    fun getSubcomponentModules(typeMirror: TypeMirror) = when {
        !subcomponentsModules.containsKey(typeMirror) -> null
        else -> subcomponentsModules[typeMirror]
    }
}