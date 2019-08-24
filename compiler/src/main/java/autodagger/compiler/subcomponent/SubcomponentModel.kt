package autodagger.compiler.subcomponent

import autodagger.compiler.addition.AdditionModel
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

data class SubcomponentModel(
    val className: Element,
    var scopeAnnotation: AnnotationMirror? = null,
    var injectorModels: List<AdditionModel>? = null,
    var exposeModels: List<AdditionModel>? = null,
    var modulesTypeNames: MutableList<TypeMirror>,
    var superinterfacesTypeNames: MutableList<TypeMirror>,
    var subcomponents: MutableList<TypeMirror>
)