package autodagger.compiler.addition

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

data class AdditionModel(
    val name: String?,
    val additionElement: TypeElement,
    val parameterizedTypeMirrors: MutableList<TypeMirror>,
    var qualifierAnnotation: AnnotationMirror? = null
)
