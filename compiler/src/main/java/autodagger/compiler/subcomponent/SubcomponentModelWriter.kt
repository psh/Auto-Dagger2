package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.*
import dagger.Subcomponent
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror


fun SubcomponentModel.writeTo(state: State, filer: Filer) {
    val componentClassName = className.getComponentClassName()
    val builder = TypeSpec.interfaceBuilder(componentClassName.simpleName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(generatedAnnotation())
        .addAnnotation(subcomponentAnnotation())
        .apply {
            getTypeNames(superinterfacesTypeNames).forEach { addSuperinterface(it) }

            scopeAnnotation?.let { addAnnotation(it.toAnnotationSpec()) }

            injectorModels?.forEach { addMethod(injectMethod(it)) }

            exposeModels?.forEach { addMethod(exposeMethod(it)) }

            if (subcomponents.isNotEmpty()) {
                addMethods(subcomponents(subcomponents, state))
            }
        }

    try {
        JavaFile.builder(componentClassName.packageName(), builder.build())
            .build()
            .writeTo(filer)
    } catch (e: Exception) {
    }
}

private fun SubcomponentModel.subcomponentAnnotation(): AnnotationSpec? {
    return AnnotationSpec.builder(Subcomponent::class.java).apply {
        getTypeNames(modulesTypeNames).forEach { typeName ->
            addMember("modules", "\$T.class", typeName)
        }
    }.build()
}

private fun subcomponents(
    subcomponentsSpecs: MutableList<TypeMirror>,
    state: State
): List<MethodSpec> {
    if (subcomponentsSpecs.isEmpty()) {
        return emptyList()
    }

    val methodSpecs = mutableListOf<MethodSpec>()
    for (typeMirror in subcomponentsSpecs) {
        val e = MoreTypes.asElement(typeMirror)
        val typeName: TypeName
        val name: String
        if (AutoSubcomponent::class.java.isPresentOn(e)) {
            with(e.getComponentClassName()) {
                typeName = this
                name = simpleName()
            }
        } else {
            typeName = TypeName.get(typeMirror)
            name = e.simpleName.toString()
        }

        val modules = state.getSubcomponentModules(typeMirror)
        val parameterSpecs = mutableListOf<ParameterSpec>().apply {
            modules?.forEachIndexed { count, moduleTypeMirror ->
                add(
                    ParameterSpec.builder(
                        TypeName.get(moduleTypeMirror),
                        String.format("module%d", count)
                    ).build()
                )
            }
        }

        methodSpecs.add(
            MethodSpec.methodBuilder("plus$name")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameters(parameterSpecs)
                .returns(typeName)
                .build()
        )
    }

    return methodSpecs
}
