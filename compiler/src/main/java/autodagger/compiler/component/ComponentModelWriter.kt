package autodagger.compiler.component

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.*
import dagger.Component
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

fun ComponentModel.writeTo(state: State, extractors: Set<ComponentExtractor>, filer: Filer) {
    val componentClassName = className.getComponentClassName()
    val builder = TypeSpec.interfaceBuilder(componentClassName.simpleName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(generatedAnnotation())
        .addAnnotation(componentAnnotation())
        .apply {
            getSuperinterfaces(
                superinterfacesTypeNames,
                extractors
            ).forEach { addSuperinterface(it) }

            scopeAnnotation?.let {
                addAnnotation(it.toAnnotationSpec())
            }

            getAdditions(
                targetTypeName,
                state.injectorExtractors.values.toList()
            ).forEach { addMethod(injectMethod(it)) }

            getAdditions(
                targetTypeName,
                state.exposeExtractors.values.toList()
            ).forEach { addMethod(exposeMethod(it)) }

            addMethods(subcomponents(state, subcomponentsTypeMirrors))
        }

    try {
        JavaFile.builder(componentClassName.packageName(), builder.build())
            .build()
            .writeTo(filer)
    } catch (e: Exception) {
    }
}

private fun ComponentModel.componentAnnotation(): AnnotationSpec =
    AnnotationSpec.builder(Component::class.java).apply {
        val dependencies = dependenciesTypeNames
            ?.map { it.getComponentClassName() }
            ?.toSet() ?: emptySet()

        dependencies.forEach {
            addMember("dependencies", "\$T.class", it)
        }

        getTypeNames(modulesTypeNames).forEach {
            addMember("modules", "\$T.class", it)
        }
    }.build()

private fun ComponentModel.getSuperinterfaces(
    superinterfaces: List<TypeMirror>?,
    extractors: Set<ComponentExtractor>
): List<TypeName> {
    val typeNames = mutableListOf<TypeName>()
    if (superinterfaces == null) {
        return typeNames
    }

    mainLoop@ for (typeMirror in superinterfaces) {
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

        typeNames.add(TypeName.get(typeMirror))
    }

    return typeNames
}

private fun subcomponents(
    state: State,
    subcomponentsTypeMirrors: MutableList<TypeMirror>?
): List<MethodSpec> {
    if (subcomponentsTypeMirrors.isNullOrEmpty()) {
        return emptyList()
    }

    val methodSpecs = mutableListOf<MethodSpec>()
    for (typeMirror in subcomponentsTypeMirrors) {
        val e = MoreTypes.asElement(typeMirror)
        val typeName: TypeName
        val name: String
        if (AutoSubcomponent::class.java.isPresentOn(e)) {
            with(e.getComponentClassName()) {
                typeName = this
                name = this.simpleName()
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

