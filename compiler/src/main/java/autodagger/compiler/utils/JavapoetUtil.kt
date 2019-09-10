package autodagger.compiler.utils

import autodagger.compiler.AutoDaggerAnnotationProcessor
import autodagger.compiler.addition.AdditionModel
import com.google.auto.common.MoreElements.getPackage
import dagger.Binds
import dagger.Module
import javax.annotation.Generated
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import com.squareup.javapoet.AnnotationSpec as JavapoetAnnotationSpec
import com.squareup.javapoet.ClassName as JavapoetClassName
import com.squareup.javapoet.MethodSpec as JavapoetMethodSpec
import com.squareup.javapoet.ParameterizedTypeName as JavapoetParameterizedTypeName
import com.squareup.javapoet.TypeName as JavapoetTypeName

fun Element.getComponentClassName(): JavapoetClassName {
    var e = this
    var name = e.simpleName.toString()

    if (e.enclosingElement.kind == ElementKind.CLASS) {
        e = e.enclosingElement
        name = "${e.simpleName}.$name"
    }

    return JavapoetClassName.get(
        getPackage(e).qualifiedName.toString(),
        when {
            name.endsWith("Component") -> name
            else -> "${name}Component"
        }
    )
}

fun moduleAnnotation(): JavapoetAnnotationSpec =
    JavapoetAnnotationSpec.builder(Module::class.java).build()

fun bindsAnnotation(): JavapoetAnnotationSpec =
    JavapoetAnnotationSpec.builder(Binds::class.java).build()

fun generatedAnnotation(): JavapoetAnnotationSpec =
    JavapoetAnnotationSpec.builder(Generated::class.java)
        .addMember("value", "\$S", AutoDaggerAnnotationProcessor::class.java.name)
        .build()

fun exposeMethod(it: AdditionModel): JavapoetMethodSpec = JavapoetMethodSpec.methodBuilder(it.name)
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .returns(
        typename(
            it.additionElement,
            it.parameterizedTypeMirrors
        )
    ).apply {
        it.qualifierAnnotation?.let<AnnotationMirror, JavapoetMethodSpec.Builder?> {
            addAnnotation(it.toJavapoetAnnotationSpec())
        }
    }.build()

fun injectMethod(it: AdditionModel): JavapoetMethodSpec =
    JavapoetMethodSpec.methodBuilder("inject")
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addParameter(typename(it.additionElement, it.parameterizedTypeMirrors), it.name)
        .build()

fun AnnotationMirror?.toJavapoetAnnotationSpec(): JavapoetAnnotationSpec? =
    if (this != null) JavapoetAnnotationSpec.get(this) else null

private fun typename(
    additionElement: TypeElement,
    parameterizedTypeMirrors: MutableList<TypeMirror>
): JavapoetTypeName =
    if (parameterizedTypeMirrors.isEmpty()) {
        JavapoetClassName.get(additionElement)
    } else {
        // with parameterized types
        JavapoetParameterizedTypeName.get(
            JavapoetClassName.get(additionElement),
            *parameterizedTypeMirrors.map { JavapoetTypeName.get(it) }.toTypedArray()
        )
    }

fun getTypeNames(typeMirrors: List<TypeMirror>?) =
    mutableListOf<JavapoetTypeName>().apply { typeMirrors?.forEach { add(JavapoetTypeName.get(it)) } }