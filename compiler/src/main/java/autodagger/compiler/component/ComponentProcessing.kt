package autodagger.compiler.component

import autodagger.AutoComponent
import autodagger.AutoSubcomponent
import autodagger.compiler.State
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
import autodagger.compiler.processorworkflow.AbstractProcessing
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.processorworkflow.ProcessingBuilder
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.ANNOTATION_TYPE
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class ComponentProcessing(elements: Elements, types: Types, errors: Errors, state: State) :
    AbstractProcessing<ComponentSpec, State>(elements, types, errors, state) {

    /**
     * Build all extractors first, then build all builders, because
     * we want to gather all targets first
     */
    private val extractors = mutableSetOf<ComponentExtractor>()

    override fun supportedAnnotations() =
        setOf(AutoComponent::class.java)

    override fun processElements(annotationElements: Set<Element>) {
        super.processElements(annotationElements)
        if (errors.hasErrors()) {
            return
        }

        processExtractors()
    }

    override fun processElement(element: Element, elementErrors: Errors.ElementErrors): Boolean {
        if (ANNOTATION_TYPE == element.kind) {
            // @AutoComponent is applied on another annotation, find out the targets of that annotation
            val targetElements =
                roundEnvironment.getElementsAnnotatedWith(MoreElements.asType(element))
            for (targetElement in targetElements) {
                process(targetElement, element)
                if (errors.hasErrors()) {
                    return false
                }
            }
            return true
        }

        process(element, element)

        return !errors.hasErrors()
    }

    private fun process(targetElement: Element, element: Element) {
        val extractor = ComponentExtractor(targetElement, element, types, elements, errors)
        if (errors.hasErrors()) {
            return
        }

        extractors.add(extractor)
    }

    private fun processExtractors() {
        extractors.forEach { extractor ->
            val spec = Builder(extractor, errors).build()
            if (errors.hasErrors()) {
                return
            }

            specs.add(spec)
        }
    }

    override fun createComposer() = ComponentComposer(specs)

    private inner class Builder(extractor: ComponentExtractor, errors: Errors) :
        ProcessingBuilder<ComponentExtractor, ComponentSpec>(extractor, errors) {

        private val subcomponents: List<MethodSpec>
            get() {
                if (extractor.subcomponentsTypeMirrors.isEmpty()) {
                    return emptyList()
                }

                val methodSpecs = ArrayList<MethodSpec>(extractor.subcomponentsTypeMirrors.size)
                for (typeMirror in extractor.subcomponentsTypeMirrors) {
                    val e = MoreTypes.asElement(typeMirror)
                    val typeName: TypeName
                    val name: String
                    if (MoreElements.isAnnotationPresent(e, AutoSubcomponent::class.java)) {
                        with (e.getComponentClassName()) {
                            typeName = this
                            name = this.simpleName()
                        }
                    } else {
                        typeName = TypeName.get(typeMirror)
                        name = e.simpleName.toString()
                    }

                    val modules = state.getSubcomponentModules(typeMirror)
                    val parameterSpecs: MutableList<ParameterSpec>
                    if (modules != null) {
                        parameterSpecs = ArrayList(modules.size)
                        var count = 0
                        for (moduleTypeMirror in modules) {
                            parameterSpecs.add(
                                ParameterSpec.builder(
                                    TypeName.get(moduleTypeMirror),
                                    String.format("module%d", ++count)
                                ).build()
                            )
                        }
                    } else {
                        parameterSpecs = ArrayList(0)
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

        // check if dependency type mirror references an @AutoComponent target
        // if so, build the TypeName that matches the target component
        // ignore self
        private val dependencies: List<TypeName>
            get() {
                val typeNames = mutableListOf<TypeName>()

                mainLoop@ for (typeMirror in extractor.dependenciesTypeMirrors) {
                    for (componentExtractor in extractors) {
                        if (componentExtractor === extractor) {
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

        public override fun build() = ComponentSpec(
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
            dependenciesTypeNames = dependencies,
            superinterfacesTypeNames = getSuperinterfaces(extractor.superinterfacesTypeMirrors),
            modulesTypeNames = getTypeNames(extractor.modulesTypeMirrors),
            subcomponentsSpecs = subcomponents
        )

        private fun getSuperinterfaces(superinterfaces: List<TypeMirror>?): List<TypeName> {
            val typeNames = ArrayList<TypeName>()
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
}
