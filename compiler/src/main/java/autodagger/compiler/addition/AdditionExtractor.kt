package autodagger.compiler.addition

import autodagger.AutoExpose
import autodagger.compiler.processorworkflow.AbstractExtractor
import autodagger.compiler.processorworkflow.Errors
import autodagger.compiler.processorworkflow.getValueFromAnnotation
import autodagger.compiler.utils.findAnnotatedAnnotation
import com.google.auto.common.MoreElements
import javax.inject.Qualifier
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class AdditionExtractor(
    additionElement: Element,
    private val additionAnnotation: Class<out Annotation>,
    element: Element,
    types: Types,
    elements: Elements,
    errors: Errors
) : AbstractExtractor<AdditionExtractor, AdditionSpec>(element, types, elements, errors) {

    /**
     * The addition element represented by @AutoInjector or @AutoExpose
     * It's either the element itself, or the element of an annotation if the @AutoXXX
     * is applied on the annotation
     */
    lateinit var additionElement: TypeElement
    var providerMethodName: String? = null
    var qualifierAnnotationMirror: AnnotationMirror? = null
    var parameterizedTypeMirrors = mutableListOf<TypeMirror>()
    var targetTypeMirrors = mutableListOf<TypeMirror>()

    init {
        try {
            this.additionElement = MoreElements.asType(additionElement)
            extract()
        } catch (e: Exception) {
            errors.addInvalid(
                additionElement,
                "%s must be a class",
                additionElement.simpleName.toString()
            )
        }
    }

    override fun extract() {
        targetTypeMirrors = getTypeMirrors("value")
        if (targetTypeMirrors.isEmpty()) {
            // if there's no value, the target is the element itself
            targetTypeMirrors.add(additionElement.asType())
        }

        parameterizedTypeMirrors = getTypeMirrors("parameterizedTypes")

        // @AutoExpose on provider method can have qualifier
        if (additionAnnotation == AutoExpose::class.java && element.kind == METHOD) {
            qualifierAnnotationMirror = findQualifier(element)
            providerMethodName = element.simpleName.toString()
        }
    }

    private fun findQualifier(element: Element): AnnotationMirror? {
        val annotationMirrors = findAnnotatedAnnotation(element, Qualifier::class.java)
        if (annotationMirrors.isEmpty()) {
            return null
        }

        if (annotationMirrors.size > 1) {
            errors.parent.addInvalid(element, "Cannot have several qualifiers (@Qualifier).")
            return null
        }

        return annotationMirrors[0]
    }

    private fun getTypeMirrors(member: String): MutableList<TypeMirror> {
        val values =
            getValueFromAnnotation<List<AnnotationValue>>(
                element,
                additionAnnotation,
                member
            )
        if (values == null || values.isEmpty()) {
            return mutableListOf()
        }

        return mutableListOf<TypeMirror>().apply {
            values.forEach {
                try {
                    add(it.value as TypeMirror)
                } catch (e: Exception) {
                    errors.addInvalid("Cannot extract member %s because %s", member, e.message)
                }
            }
        }
    }
}
