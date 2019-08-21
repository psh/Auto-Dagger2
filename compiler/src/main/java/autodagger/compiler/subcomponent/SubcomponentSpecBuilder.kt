package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.State
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.processorworkflow.AbstractProcessingBuilder
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

class SubcomponentSpecBuilder(extractor: SubcomponentExtractor, errors: Errors) :
    AbstractProcessingBuilder<SubcomponentExtractor, SubcomponentSpec>(extractor, errors) {

    override fun build(state: State, extractors: Set<SubcomponentExtractor>) =
        SubcomponentSpec(
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
                    name = simpleName()
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
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameters(parameterSpecs)
                    .returns(typeName)
                    .build()
            )
        }

        return methodSpecs
    }
}