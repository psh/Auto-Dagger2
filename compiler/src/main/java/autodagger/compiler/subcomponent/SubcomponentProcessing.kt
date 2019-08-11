package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.utils.getAdditions
import autodagger.compiler.utils.getComponentClassName
import autodagger.compiler.utils.getTypeNames
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import dagger.Subcomponent
import processorworkflow.AbstractComposer
import processorworkflow.AbstractProcessing
import processorworkflow.Errors
import processorworkflow.ProcessingBuilder
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class SubcomponentProcessing(elements: Elements, types: Types, errors: Errors, state: State) :
    AbstractProcessing<SubcomponentSpec, State>(elements, types, errors, state) {

    /**
     * Build all extractors first, then build all builders, because
     * we want to gather all addsTo that can be related to other annoted auto subcomponents
     */
    private val extractors = HashSet<SubcomponentExtractor>()

    override fun supportedAnnotations(): Set<Class<out Annotation>> {
        return ImmutableSet.of(AutoSubcomponent::class.java, Subcomponent::class.java)
    }

    override fun processElements(annotationElements: Set<Element>) {
        super.processElements(annotationElements)
        if (errors.hasErrors()) {
            return
        }

        processExtractors()
    }

    override fun processElement(element: Element, elementErrors: Errors.ElementErrors): Boolean {
        val extractor = SubcomponentExtractor(element, types, elements, errors)
        if (errors.hasErrors()) {
            return false
        }

        extractors.add(extractor)

        if (extractor.modulesTypeMirrors.isNotEmpty()) {
            state.addSubcomponentModule(element.asType(), extractor.modulesTypeMirrors)
        }

        return true
    }

    private fun processExtractors() {
        for (extractor in extractors) {
            val spec = Builder(extractor, errors).build()
            if (errors.hasErrors()) {
                return
            }

            specs.add(spec)
        }
    }

    override fun createComposer(): AbstractComposer<SubcomponentSpec> {
        return SubcomponentComposer(specs)
    }

    private inner class Builder(extractor: SubcomponentExtractor, errors: Errors) :
        ProcessingBuilder<SubcomponentExtractor, SubcomponentSpec>(extractor, errors) {

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
                        with(e.getComponentClassName()) {
                            typeName = this
                            name = simpleName()
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

        public override fun build() = SubcomponentSpec(
            className = extractor.element.getComponentClassName(),
            scopeAnnotationSpec = if (extractor.scopeAnnotationTypeMirror != null) {
                AnnotationSpec.get(extractor.scopeAnnotationTypeMirror!!)
            } else null,
            modulesTypeNames = getTypeNames(extractor.modulesTypeMirrors),
            superinterfacesTypeNames = getTypeNames(extractor.superinterfacesTypeMirrors),
            exposeSpecs = getAdditions(
                extractor.element,
                state.exposeExtractors.values.toList()
            ),
            injectorSpecs = getAdditions(
                extractor.element,
                state.injectorExtractors.values.toList()
            ),
            subcomponentsSpecs = subcomponents
        )
    }

}