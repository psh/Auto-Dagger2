package autodagger.compiler.processorworkflow

import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element

@Suppress("UNCHECKED_CAST")
fun <T> getValueFromAnnotation(
    element: Element,
    annotation: Class<out Annotation>,
    name: String
): T? {
    val annotationMirror = MoreElements.getAnnotationMirror(element, annotation)
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

