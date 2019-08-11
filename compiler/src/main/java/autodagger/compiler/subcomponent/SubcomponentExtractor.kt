package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.utils.ANNOTATION_MODULES
import autodagger.compiler.utils.ANNOTATION_SUBCOMPONENTS
import autodagger.compiler.utils.ANNOTATION_SUPERINTERFACES
import autodagger.compiler.utils.findAnnotatedAnnotation
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import dagger.Subcomponent
import processorworkflow.AbstractExtractor
import processorworkflow.Errors
import processorworkflow.getValueFromAnnotation
import javax.inject.Scope
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class SubcomponentExtractor(element: Element, types: Types, elements: Elements, errors: Errors) :
    AbstractExtractor(element, types, elements, errors) {

    var modulesTypeMirrors = mutableListOf<TypeMirror>()
    var superinterfacesTypeMirrors = mutableListOf<TypeMirror>()
    var subcomponentsTypeMirrors = mutableListOf<TypeMirror>()
    var scopeAnnotationTypeMirror: AnnotationMirror? = null

    init {
        extract()
    }

    override fun extract() {
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES)
        subcomponentsTypeMirrors = findTypeMirrors(element, ANNOTATION_SUBCOMPONENTS)
        if (!MoreElements.isAnnotationPresent(element, AutoSubcomponent::class.java)) {
            return
        }

        superinterfacesTypeMirrors = findTypeMirrors(element, ANNOTATION_SUPERINTERFACES)
        scopeAnnotationTypeMirror = findScope()
    }

    private fun findTypeMirrors(element: Element, name: String): MutableList<TypeMirror> {
        val addsTo = name == ANNOTATION_SUBCOMPONENTS

        val typeMirrors = mutableListOf<TypeMirror>()
        val values = getValueFromAnnotation<List<AnnotationValue>>(
            element,
            AutoSubcomponent::class.java,
            name
        )
        if (values != null) {
            for (value in values) {
                if (!validateAnnotationValue(value, name)) {
                    continue
                }

                try {
                    val tm = value.value as TypeMirror
                    if (addsTo) {
                        val e = MoreTypes.asElement(tm)
                        if (!MoreElements.isAnnotationPresent(
                                e,
                                AutoSubcomponent::class.java
                            ) && !MoreElements.isAnnotationPresent(e, Subcomponent::class.java)
                        ) {
                            errors.addInvalid(
                                "@AutoComponent cannot declare a subcomponent that is not annotated with @Subcomponent or @AutoSubcomponent: %s",
                                e.simpleName.toString()
                            )
                            continue
                        }
                    }
                    typeMirrors.add(tm)
                } catch (e: Exception) {
                    errors.addInvalid(e.message ?: e.javaClass.simpleName)
                    break
                }

            }
        }

        return typeMirrors
    }

    /**
     * Find annotation that is itself annoted with @Scope
     * If there is one, it will be later applied on the generated component
     * Otherwise the component will be unscoped
     * Throw error if more than one scope annotation found
     */
    private fun findScope(): AnnotationMirror? {
        val annotationMirrors = findAnnotatedAnnotation(element, Scope::class.java)
        if (annotationMirrors.isEmpty()) {
            return null
        }

        if (annotationMirrors.size > 1) {
            errors.parent.addInvalid(element, "Cannot have several scope (@Scope).")
            return null
        }

        return annotationMirrors[0]
    }

    private fun validateAnnotationValue(value: AnnotationValue, member: String): Boolean {
        if (value.value !is TypeMirror) {
            errors.addInvalid(
                "%s cannot reference generated class. Use the class that applies the @AutoComponent annotation",
                member
            )
            return false
        }

        return true
    }
}
