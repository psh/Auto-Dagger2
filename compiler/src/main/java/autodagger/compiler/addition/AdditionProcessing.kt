package autodagger.compiler.addition

import autodagger.AutoInjector
import autodagger.compiler.State
import autodagger.compiler.processorworkflow.AbstractProcessing
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.utils.isNotPresentOn
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import dagger.Provides
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.ANNOTATION_TYPE
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class AdditionProcessing(elements: Elements, types: Types, errors: Errors, state: State) :
    AbstractProcessing<AdditionSpec, AdditionExtractor>(elements, types, errors, state) {

    override fun processElement(element: Element, elementErrors: Errors.ElementErrors): Boolean {
        // @AutoX applied on annotation
        if (element.kind == ANNOTATION_TYPE) {
            // @AutoX is applied on another annotation, find out the targets of that annotation
            roundEnvironment.getElementsAnnotatedWith(MoreElements.asType(element)).forEach {
                if (!process(it, element)) {
                    return false
                }
            }
            return true
        }

        // @AutoX applied on method
        // only valid for @AutoExpose with @Provides
        if (element.kind == METHOD) {
            if (processedAnnotation == AutoInjector::class.java) {
                errors.addInvalid(
                    element,
                    "@AutoInjector cannot be applied on the method %s",
                    element.simpleName.toString()
                )
                return false
            }

            if (Provides::class.java.isNotPresentOn(element)) {
                errors.addInvalid(
                    element,
                    "@AutoExpose can be applied on @Provides method only, %s is missing it",
                    element.simpleName.toString()
                )
                return false
            }

            return process(
                MoreTypes.asElement(MoreElements.asExecutable(element).returnType),
                element
            )
        }

        process(element, element)
        return !errors.hasErrors()
    }

    private fun process(targetElement: Element, element: Element): Boolean {
        val extractor = AdditionExtractor(
            targetElement, processedAnnotation,
            element, types, elements, errors
        )

        if (errors.hasErrors()) {
            return false
        }

        if (extractor.targetTypeMirrors.isEmpty()) {
            throw IllegalArgumentException("Addition target cannot be empty")
        }

        if (processedAnnotation == AutoInjector::class.java) {
            state.addInjectorExtractor(extractor)
        } else {
            state.addExposeExtractor(extractor)
        }

        return true
    }
}
