package autodagger.compiler.component

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.binds.BindsExtractor
import autodagger.compiler.binds.extractorsMatchingElement
import autodagger.compiler.binds.getBindingModuleName
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import dagger.Component
import javax.annotation.processing.Filer
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Modifier
import com.squareup.javapoet.AnnotationSpec as JavapoetAnnotationSpec
import com.squareup.javapoet.JavaFile as JavapoetJavaFile
import com.squareup.javapoet.MethodSpec as JavapoetMethodSpec
import com.squareup.javapoet.ParameterSpec as JavapoetParameterSpec
import com.squareup.javapoet.TypeName as JavapoetTypeName
import com.squareup.javapoet.TypeSpec as JavapoetTypeSpec

fun ComponentModel.writeTo(state: State, extractors: Set<ComponentExtractor>, filer: Filer) {
    val autoBindingModuleContent = extractorsMatchingElement(
        targetTypeName, state.bindingExtractors
    )
    val autoBindingModuleName = className.getBindingModuleName()
    if (autoBindingModuleContent.isNotEmpty()) {
        createAutoBindingModule(autoBindingModuleName, autoBindingModuleContent, filer)
    }

    val componentClassName = className.getComponentClassName()
    val builder = JavapoetTypeSpec.interfaceBuilder(componentClassName.simpleName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(generatedAnnotation())
        .addAnnotation(
            componentAnnotation(
                autoBindingModuleContent.isNotEmpty(),
                autoBindingModuleName
            )
        )
        .apply {
            superinterfaces(extractors).forEach {
                addSuperinterface(it)
            }

            scopeAnnotation?.let {
                addAnnotation(it.toJavapoetAnnotationSpec())
            }

            additionsMatchingElement(
                targetTypeName,
                state.injectorExtractors.values
            ).forEach { addMethod(injectMethod(it)) }

            additionsMatchingElement(
                targetTypeName,
                state.exposeExtractors.values
            ).forEach { addMethod(exposeMethod(it)) }

            addMethods(subcomponents(state))
        }

    try {
        JavapoetJavaFile.builder(componentClassName.packageName(), builder.build())
            .build()
            .writeTo(filer)
    } catch (e: Exception) {
    }
}

private fun createAutoBindingModule(
    moduleName: ClassName,
    moduleContent: List<BindsExtractor>,
    filer: Filer
) {
    val moduleBuilder = JavapoetTypeSpec.classBuilder(moduleName)
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
        .addAnnotation(generatedAnnotation())
        .addAnnotation(moduleAnnotation()).apply {
            moduleContent.forEach {
                addMethod(createAutoBindingMethod(it))
            }
        }

    try {
        JavapoetJavaFile.builder(moduleName.packageName(), moduleBuilder.build())
            .build()
            .writeTo(filer)
    } catch (e: Exception) {
    }
}

private fun createAutoBindingMethod(extractor: BindsExtractor) =
    JavapoetMethodSpec.methodBuilder(extractor.bindingMethodName)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addAnnotation(bindsAnnotation())
        .addParameter(
            JavapoetParameterSpec.builder(
                JavapoetTypeName.get(extractor.element.asType()),
                "value"
            ).build()
        )
        .returns(JavapoetTypeName.get(extractor.implementedInterface)).apply {
            extractor.qualifierAnnotationMirror?.let<AnnotationMirror, JavapoetMethodSpec.Builder?> { am ->
                addAnnotation(am.toJavapoetAnnotationSpec())
            }
        }
        .build()

private fun ComponentModel.componentAnnotation(
    moduleContent: Boolean,
    moduleName: ClassName
): JavapoetAnnotationSpec =
    JavapoetAnnotationSpec.builder(Component::class.java).apply {
        val dependencies = dependenciesTypeNames
            ?.map { it.getComponentClassName() }
            ?.toSet() ?: emptySet()

        dependencies.forEach {
            addMember("dependencies", "\$T.class", it)
        }

        getTypeNames(modulesTypeNames).forEach {
            addMember("modules", "\$T.class", it)
        }

        if (moduleContent) {
            addMember("modules", "\$T.class", moduleName)
        }
    }.build()

private fun ComponentModel.superinterfaces(
    extractors: Set<ComponentExtractor>
): List<JavapoetTypeName> {
    val typeNames = mutableListOf<JavapoetTypeName>()
    if (superinterfacesTypeNames == null) {
        return typeNames
    }

    mainLoop@ for (typeMirror in superinterfacesTypeNames!!) {
        // check if dependency type mirror references an @AutoComponent target
        // if so, build the TypeName that matches the target component
        for (componentExtractor in extractors) {
            if (componentExtractor === extractor) {
                // ignore self
                continue
            }

            if (areTypesEqual(componentExtractor.targetTypeMirror, typeMirror)) {
                typeNames.add(componentExtractor.componentElement.getComponentClassName())
                continue@mainLoop
            }
        }

        typeNames.add(JavapoetTypeName.get(typeMirror))
    }

    return typeNames
}

private fun ComponentModel.subcomponents(state: State): List<JavapoetMethodSpec> {
    if (subcomponentsTypeMirrors.isNullOrEmpty()) {
        return emptyList()
    }

    val methodSpecs = mutableListOf<JavapoetMethodSpec>()
    for (typeMirror in subcomponentsTypeMirrors) {
        val e = MoreTypes.asElement(typeMirror)
        val typeName: JavapoetTypeName
        val name: String
        if (e annotatedWith AutoSubcomponent::class.java) {
            with(e.getComponentClassName()) {
                typeName = this
                name = this.simpleName()
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
                .addModifiers(
                    Modifier.PUBLIC,
                    Modifier.ABSTRACT
                )
                .addParameters(parameterSpecs)
                .returns(typeName)
                .build()
        )
    }

    return methodSpecs
}

