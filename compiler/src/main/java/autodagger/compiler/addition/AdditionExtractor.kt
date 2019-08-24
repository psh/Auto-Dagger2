package autodagger.compiler.addition

import autodagger.AutoExpose
import autodagger.compiler.Errors
import autodagger.compiler.State
import autodagger.compiler.utils.DiagnosticsSource
import autodagger.compiler.utils.findAnnotatedAnnotation
import autodagger.compiler.utils.getValueFromAnnotation
import javax.inject.Qualifier
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class AdditionExtractor(
    private val additionAnnotation: Class<out Annotation>,
    val additionElement: TypeElement?,
    override val element: Element,
    state: State
) : DiagnosticsSource {
    private val errors: Errors.ElementErrors = Errors.ElementErrors(state.errors, element)
    var providerMethodName: String? = null
    var qualifierAnnotationMirror: AnnotationMirror? = null
    var parameterizedTypeMirrors = mutableListOf<TypeMirror>()
    var targetTypeMirrors = mutableListOf<TypeMirror>()

    /**
     * The addition element represented by @AutoInjector or @AutoExpose
     * It's either the element itself, or the element of an annotation if the @AutoXXX
     * is applied on the annotation
     */
    init {
        extract()
    }

    override fun toDiagnostics(): MutableMap<String, String?> = mutableMapOf(
        "providerMethodName" to providerMethodName,
        "qualifierAnnotationMirror" to qualifierAnnotationMirror?.toString(),
        "parameterizedTypeMirrors" to parameterizedTypeMirrors.toString(),
        "targetTypeMirrors" to targetTypeMirrors.toString()
    )

    private fun extract() {
        targetTypeMirrors = getTypeMirrors("value")
        if (targetTypeMirrors.isEmpty()) {
            // if there's no value, the target is the element itself
            targetTypeMirrors.add(additionElement!!.asType())
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
