package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import dagger.Subcomponent
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier
import com.squareup.javapoet.AnnotationSpec as JavapoetAnnotationSpec
import com.squareup.javapoet.JavaFile as JavapoetJavaFile
import com.squareup.javapoet.MethodSpec as JavapoetMethodSpec
import com.squareup.javapoet.ParameterSpec as JavapoetParameterSpec
import com.squareup.javapoet.TypeName as JavapoetTypeName
import com.squareup.javapoet.TypeSpec as JavapoetTypeSpec

fun SubcomponentModel.writeTo(state: State, filer: Filer) {
    val componentClassName = className.getComponentClassName()
    val builder = JavapoetTypeSpec.interfaceBuilder(componentClassName.simpleName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(generatedAnnotation())
        .addAnnotation(subcomponentAnnotation())
        .apply {
            getTypeNames(superinterfacesTypeNames).forEach { addSuperinterface(it) }

            scopeAnnotation?.let { addAnnotation(it.toJavapoetAnnotationSpec()) }

            injectorModels?.forEach { addMethod(injectMethod(it)) }

            exposeModels?.forEach { addMethod(exposeMethod(it)) }

            if (subcomponents.isNotEmpty()) {
                addMethods(subcomponents(state))
            }
        }

    try {
        JavapoetJavaFile.builder(componentClassName.packageName(), builder.build())
            .build()
            .writeTo(filer)
    } catch (e: Exception) {
    }
}

private fun SubcomponentModel.subcomponentAnnotation(): JavapoetAnnotationSpec? {
    return JavapoetAnnotationSpec.builder(Subcomponent::class.java).apply {
        getTypeNames(modulesTypeNames).forEach { typeName ->
            addMember("modules", "\$T.class", typeName)
        }
    }.build()
}

private fun SubcomponentModel.subcomponents(state: State): List<JavapoetMethodSpec> {
    if (subcomponents.isEmpty()) {
        return emptyList()
    }

    val methodSpecs = mutableListOf<JavapoetMethodSpec>()
    for (typeMirror in subcomponents) {
        val e = MoreTypes.asElement(typeMirror)
        val typeName: JavapoetTypeName
        val name: String
        if (e annotatedWith AutoSubcomponent::class.java) {
            with(e.getComponentClassName()) {
                typeName = this
                name = simpleName()
            }
        } else {
            typeName = JavapoetTypeName.get(typeMirror)
            name = e.simpleName.toString()
        }

        val modules = state.subcomponentModulesOf(typeMirror)
        val parameterSpecs = mutableListOf<JavapoetParameterSpec>().apply {
            modules.forEachIndexed { count, moduleTypeMirror ->
                add(
                    JavapoetParameterSpec.builder(
                        JavapoetTypeName.get(moduleTypeMirror),
                        String.format("module%d", count)
                    ).build()
                )
            }
        }

        methodSpecs.add(
            JavapoetMethodSpec.methodBuilder("plus$name")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameters(parameterSpecs)
                .returns(typeName)
                .build()
        )
    }

    return methodSpecs
}
