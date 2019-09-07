package autodagger.compiler.component

import autodagger.compiler.addition.AdditionModel
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

data class ComponentModel(
    val className: Element,
    var targetTypeName: TypeMirror? = null,
    var scopeAnnotation: AnnotationMirror? = null,
    var injectorModels: List<AdditionModel>? = null,
    var exposeModels: List<AdditionModel>? = null,
    var dependenciesTypeNames: List<Element>? = null,
    var dependenciesTypeMirrors: MutableList<TypeMirror>,
    var modulesTypeNames: MutableList<TypeMirror>? = null,
    var superinterfacesTypeNames: MutableList<TypeMirror>? = null,
    val subcomponentsTypeMirrors: MutableList<TypeMirror>? = null,
    val extractor: ComponentExtractor
)