package autodagger.compiler.component

import autodagger.compiler.addition.AdditionSpec
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName

data class ComponentSpec(
    val className: ClassName,
    var targetTypeName: TypeName? = null,
    var scopeAnnotationSpec: AnnotationSpec? = null,
    var injectorSpecs: List<AdditionSpec>? = null,
    var exposeSpecs: List<AdditionSpec>? = null,
    var dependenciesTypeNames: List<TypeName>? = null,
    var modulesTypeNames: List<TypeName>? = null,
    var superinterfacesTypeNames: List<TypeName>? = null,
    var subcomponentsSpecs: List<MethodSpec>? = null
)
