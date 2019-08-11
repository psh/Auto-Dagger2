package autodagger.compiler.addition

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName

data class AdditionSpec(
    val name: String?,
    val typeName: TypeName,
    var qualifierAnnotationSpec: AnnotationSpec? = null
)
