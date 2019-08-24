package autodagger.compiler.utils

import autodagger.compiler.AutoDaggerAnnotationProcessor
import autodagger.compiler.addition.AdditionModel
import com.google.auto.common.MoreElements.getPackage
import com.squareup.javapoet.*
import javax.annotation.Generated
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

fun Element.getComponentClassName(): ClassName {
    var e = this
    var name = e.simpleName.toString()

    if (e.enclosingElement.kind == ElementKind.CLASS) {
        e = e.enclosingElement
        name = "${e.simpleName}.$name"
    }

    return ClassName.get(
        getPackage(e).qualifiedName.toString(),
        name.getComponentSimpleName()
    )
}

fun generatedAnnotation(): AnnotationSpec = AnnotationSpec.builder(Generated::class.java)
    .addMember("value", "\$S", AutoDaggerAnnotationProcessor::class.java.name)
    .build()

fun exposeMethod(it: AdditionModel): MethodSpec = MethodSpec.methodBuilder(it.name)
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .returns(
        typename(
            it.additionElement,
            it.parameterizedTypeMirrors
        )
    ).apply {
        it.qualifierAnnotation?.let<AnnotationMirror, MethodSpec.Builder?> {
            addAnnotation(it.toAnnotationSpec())
        }
    }.build()

fun injectMethod(it: AdditionModel): MethodSpec = MethodSpec.methodBuilder("inject")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addParameter(
        typename(
            it.additionElement,
            it.parameterizedTypeMirrors
        ), it.name
    ).build()

fun AnnotationMirror?.toAnnotationSpec(): AnnotationSpec? =
    if (this != null) AnnotationSpec.get(this) else null

fun typename(
    additionElement: TypeElement,
    parameterizedTypeMirrors: MutableList<TypeMirror>
): TypeName =
    if (parameterizedTypeMirrors.isEmpty()) {
        ClassName.get(additionElement)
    } else {
        // with parameterized types
        ParameterizedTypeName.get(
            ClassName.get(additionElement),
            *parameterizedTypeMirrors.map { TypeName.get(it) }.toTypedArray()
        )
    }

fun getTypeNames(typeMirrors: List<TypeMirror>?): List<TypeName> = mutableListOf<TypeName>().apply {
    typeMirrors?.forEach { add(TypeName.get(it)) }
}