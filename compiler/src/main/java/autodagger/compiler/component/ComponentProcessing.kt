package autodagger.compiler.component

import autodagger.compiler.State
import autodagger.compiler.processorworkflow.AbstractProcessing
import autodagger.compiler.processorworkflow.Errors
import com.google.auto.common.MoreElements
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.ANNOTATION_TYPE
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class ComponentProcessing(elements: Elements, types: Types, errors: Errors, state: State) :
    AbstractProcessing<ComponentSpec, ComponentExtractor>(elements, types, errors, state) {

    override fun processElement(element: Element, elementErrors: Errors.ElementErrors): Boolean {
        if (ANNOTATION_TYPE == element.kind) {
            // @AutoComponent is applied on another annotation, find out the targets of that annotation
            val targetElements =
                roundEnvironment.getElementsAnnotatedWith(MoreElements.asType(element))
            for (targetElement in targetElements) {
                val extractor = ComponentExtractor(targetElement, element, types, elements, errors)
                if (!errors.hasErrors()) {
                    extractors.add(extractor)
                } else {
                    return false
                }
            }
            return true
        }

        val extractor = ComponentExtractor(element, element, this.types, this.elements, this.errors)
        if (!this.errors.hasErrors()) {
            this.extractors.add(extractor)
        }

        return !errors.hasErrors()
    }
}
