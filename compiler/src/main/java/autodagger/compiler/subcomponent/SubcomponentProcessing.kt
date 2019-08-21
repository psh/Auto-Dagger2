package autodagger.compiler.subcomponent

import autodagger.compiler.State
import autodagger.compiler.processorworkflow.AbstractProcessing
import autodagger.compiler.processorworkflow.Errors
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class SubcomponentProcessing(elements: Elements, types: Types, errors: Errors, state: State) :
    AbstractProcessing<SubcomponentSpec, SubcomponentExtractor>(
        elements,
        types,
        errors,
        state
    ) {

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
}