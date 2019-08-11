package autodagger.compiler.subcomponent

import autodagger.compiler.addition.AdditionSpec
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName

data class SubcomponentSpec(
    val className: ClassName,
    var scopeAnnotationSpec: AnnotationSpec? = null,
    var injectorSpecs: List<AdditionSpec>? = null,
    var exposeSpecs: List<AdditionSpec>? = null,
    var modulesTypeNames: List<TypeName>? = null,
    var superinterfacesTypeNames: List<TypeName>? = null,
    var subcomponentsSpecs: List<MethodSpec>? = null
)