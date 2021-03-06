package autodagger.compiler.utils

import autodagger.compiler.Errors
import autodagger.compiler.addition.AdditionExtractor
import autodagger.compiler.addition.AdditionModel
import com.google.auto.common.MoreElements.*
import com.google.auto.common.MoreTypes
import javax.inject.Qualifier
import javax.inject.Scope
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

const val ANNOTATION_DEPENDENCIES = "dependencies"
const val ANNOTATION_MODULES = "modules"
const val ANNOTATION_TARGET = "target"
const val ANNOTATION_SUPERINTERFACES = "superinterfaces"
const val ANNOTATION_INCLUDES = "includes"
const val ANNOTATION_SUBCOMPONENTS = "subcomponents"

/**
 * Types.isSameType() does not work when the origin element that triggers annotation
 * processing, and calls Types.isSameType() is generated by an other annotation processor
 * Workaround is to compare the full qualified names of the two types
 */

fun areTypesEqual(typeMirror1: TypeMirror?, typeMirror2: TypeMirror?) =
    asType(MoreTypes.asElement(typeMirror1)).qualifiedName ==
            asType(MoreTypes.asElement(typeMirror2)).qualifiedName

fun additionsMatchingElement(
    elementTypeMirror: TypeMirror?,
    extractors: Collection<AdditionExtractor>
): List<AdditionModel> = mutableListOf<AdditionModel>().apply {
    extractors.forEach { additionExtractor ->
        // for each targets in those additions
        additionExtractor.targetTypeMirrors.forEach { typeMirror ->
            // find if that target is a target for the current component
            // happens only 1 time per loop
            if (areTypesEqual(elementTypeMirror, typeMirror)) {
                add(createAdditionModel(additionExtractor))
            }
        }
    }
}

private fun createAdditionModel(additionExtractor: AdditionExtractor): AdditionModel =
    AdditionModel(
        name = if (additionExtractor.providerMethodName != null) {
            additionExtractor.providerMethodName?.let {
                // try to remove "provide" or "provides" from name
                if (it.startsWith("provides")) {
                    it.removePrefix("provides")
                } else if (it.startsWith("provide")) {
                    it.removePrefix("provide")
                }
                it.decapitalize()
            }
        } else {
            additionExtractor.additionElement!!.simpleName.toString()
                .decapitalize()
        },
        additionElement = additionExtractor.additionElement!!,
        parameterizedTypeMirrors = additionExtractor.parameterizedTypeMirrors,
        qualifierAnnotation = additionExtractor.qualifierAnnotationMirror
    )

fun findQualifier(element: Element, errors: Errors.ElementErrors): AnnotationMirror? {
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

fun findAnnotatedAnnotation(
    element: Element,
    annotationCls: Class<out Annotation>
): List<AnnotationMirror> = mutableListOf<AnnotationMirror>().apply {
    element.annotationMirrors.forEach {
        val annotationElement = it.annotationType.asElement()
        if (annotationElement annotatedWith annotationCls) {
            add(it)
        }
    }
}

infix fun <T : Annotation> Element.annotatedWith(annotation: Class<T>): Boolean =
    isAnnotationPresent(this, annotation)

infix fun <T : Annotation> Element.notAnnotatedWith(annotation: Class<T>): Boolean =
    !isAnnotationPresent(this, annotation)

@Suppress("UNCHECKED_CAST")
fun <T> getValueFromAnnotation(
    element: Element,
    annotation: Class<out Annotation>,
    name: String
): T? {
    val annotationMirror =
        getAnnotationMirror(element, annotation)
    if (!annotationMirror.isPresent) {
        return null
    }

    return getAnnotationValue(annotationMirror.get(), name)?.value as T
}

private fun getAnnotationValue(annotationMirror: AnnotationMirror, key: String): AnnotationValue? {
    for ((key1, value) in annotationMirror.elementValues) {
        if (key1.simpleName.toString() == key) {
            return value
        }
    }
    return null
}

fun Element.findScope(elementErrors: Errors.ElementErrors): AnnotationMirror? {
    val annotationMirrors = findAnnotatedAnnotation(this, Scope::class.java)
    if (annotationMirrors.isEmpty()) {
        return null
    }

    if (annotationMirrors.size > 1) {
        elementErrors.parent.addInvalid(this, "Cannot have several scope (@Scope).")
        return null
    }

    return annotationMirrors[0]
}
