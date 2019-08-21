package autodagger.compiler.component

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.processorworkflow.AbstractProcessingBuilder
import autodagger.compiler.utils.areTypesEqual
import autodagger.compiler.utils.getAdditions
import autodagger.compiler.utils.getComponentClassName
import autodagger.compiler.utils.getTypeNames
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

class ComponentSpecBuilder(extractor: ComponentExtractor, errors: Errors) :
    AbstractProcessingBuilder<ComponentExtractor, ComponentSpec>(extractor, errors) {

    override fun build(state: State, extractors: Set<ComponentExtractor>) = ComponentSpec(
        className = extractor.componentElement.getComponentClassName(),
        targetTypeName = TypeName.get(extractor.targetTypeMirror),
        scopeAnnotationSpec = if (extractor.scopeAnnotationTypeMirror != null) AnnotationSpec.get(
            extractor.scopeAnnotationTypeMirror
        ) else null,
        injectorSpecs = getAdditions(
            extractor.targetTypeMirror,
            state.injectorExtractors.values.toList()
        ),
        exposeSpecs = getAdditions(
            extractor.targetTypeMirror,
            state.exposeExtractors.values.toList()
        ),
        dependenciesTypeNames = dependencies(extractors),
        superinterfacesTypeNames = getSuperinterfaces(
            extractor.superinterfacesTypeMirrors,
            extractors
        ),
        modulesTypeNames = getTypeNames(extractor.modulesTypeMirrors),
        subcomponentsSpecs = subcomponents(state)
    )

    private fun subcomponents(state: State): List<MethodSpec> {
        if (extractor.subcomponentsTypeMirrors.isEmpty()) {
            return emptyList()
        }

        val methodSpecs = mutableListOf<MethodSpec>()
        for (typeMirror in extractor.subcomponentsTypeMirrors) {
            val e = MoreTypes.asElement(typeMirror)
            val typeName: TypeName
            val name: String
            if (MoreElements.isAnnotationPresent(e, AutoSubcomponent::class.java)) {
                with(e.getComponentClassName()) {
                    typeName = this
                    name = this.simpleName()
                }
            } else {
                typeName = TypeName.get(typeMirror)
                name = e.simpleName.toString()
            }

            val modules = state.getSubcomponentModules(typeMirror)
            val parameterSpecs = mutableListOf<ParameterSpec>()
            if (modules != null) {
                var count = 0
                for (moduleTypeMirror in modules) {
                    parameterSpecs.add(
                        ParameterSpec.builder(
                            TypeName.get(moduleTypeMirror),
                            String.format("module%d", ++count)
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

    // check if dependency type mirror references an @AutoComponent target
    // if so, build the TypeName that matches the target component
    // ignore self
    private fun dependencies(extractors: Set<ComponentExtractor>): List<TypeName> {
        val typeNames = mutableListOf<TypeName>()

        mainLoop@ for (typeMirror in extractor.dependenciesTypeMirrors) {
            for (componentExtractor in extractors) {
                if (componentExtractor === extractor) {
                    continue
                }

                if (areTypesEqual(
                        componentExtractor.targetTypeMirror,
                        typeMirror
                    )
                ) {
                    typeNames.add(componentExtractor.componentElement.getComponentClassName())
                    continue@mainLoop
                }
            }

            typeNames.add(TypeName.get(typeMirror))
        }

        return typeNames
    }

    private fun getSuperinterfaces(
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
}