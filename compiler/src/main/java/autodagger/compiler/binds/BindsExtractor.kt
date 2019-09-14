package autodagger.compiler.binds

import autodagger.compiler.Errors
import autodagger.compiler.State
import autodagger.compiler.utils.areTypesEqual
import autodagger.compiler.utils.findQualifier
import autodagger.compiler.utils.getValueFromAnnotation
import com.google.auto.common.MoreElements.getPackage
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeMirror

class BindsExtractor(
    private val additionAnnotation: Class<out Annotation>,
    val element: Element,
    val implementedInterface: TypeMirror,
    state: State
) {
    private val errors: Errors.ElementErrors = Errors.ElementErrors(state.errors, element)
    var qualifierAnnotationMirror: AnnotationMirror? = null
    var targetTypeMirrors = mutableListOf<TypeMirror>()
    val bindingMethodName by lazy { "autoBind${MoreTypes.asElement(implementedInterface).simpleName}To${element.simpleName}" }

    init {
        extract()
    }

    private fun extract() {
        targetTypeMirrors = getTypeMirrors()
        qualifierAnnotationMirror = findQualifier(element, errors)
    }

    private fun getTypeMirrors(): MutableList<TypeMirror> {
        val values =
            getValueFromAnnotation<List<AnnotationValue>>(element, additionAnnotation, "value")

        if (values == null || values.isEmpty()) {
            return mutableListOf()
        }

        return mutableListOf<TypeMirror>().apply {
            values.forEach {
                try {
                    add(it.value as TypeMirror)
                } catch (e: Exception) {
                    errors.addInvalid("Cannot extract member %s because %s", "value", e.message)
                }
            }
        }
    }
}

fun extractorsMatchingElement(
    elementTypeMirror: TypeMirror?,
    extractors: Collection<BindsExtractor>
): List<BindsExtractor> = mutableListOf<BindsExtractor>().apply {
    extractors.forEach { extractor ->
        // for each targets in those additions
        extractor.targetTypeMirrors.forEach { typeMirror ->
            // find if that target is a target for the current component
            // happens only 1 time per loop
            if (areTypesEqual(elementTypeMirror, typeMirror)) {
                add(extractor)
            }
        }
    }
}

fun Element.getBindingModuleName(): ClassName {
    var e = this
    var name = e.simpleName.toString()

    if (e.enclosingElement.kind == ElementKind.CLASS) {
        e = e.enclosingElement
        name = "${e.simpleName}.$name"
    }

    return ClassName.get(
        getPackage(e).qualifiedName.toString(),
        when {
            name.endsWith("Component") -> "${name}AutoBindings"
            else -> "${name}ComponentAutoBindings"
        }
    )
}