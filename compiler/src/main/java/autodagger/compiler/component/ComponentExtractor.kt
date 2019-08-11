package autodagger.compiler.component

import autodagger.AutoComponent
import autodagger.AutoSubcomponent
import autodagger.compiler.utils.*
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import dagger.Subcomponent
import processorworkflow.AbstractExtractor
import processorworkflow.Errors
import processorworkflow.getValueFromAnnotation
import java.util.*
import javax.inject.Scope
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * The component element represented by @AutoComponent
 * It's either the element itself, or the element of an annotation if the @AutoComponent
 * is applied on the annotation
 */
class ComponentExtractor(
    val componentElement: Element,
    element: Element,
    types: Types,
    elements: Elements,
    errors: Errors
) : AbstractExtractor(element, types, elements, errors) {

    var targetTypeMirror: TypeMirror? = null
    var scopeAnnotationTypeMirror: AnnotationMirror? = null
    var dependenciesTypeMirrors = mutableListOf<TypeMirror>()
    var modulesTypeMirrors = mutableListOf<TypeMirror>()
    var superinterfacesTypeMirrors = mutableListOf<TypeMirror>()
    var subcomponentsTypeMirrors = mutableListOf<TypeMirror>()

    init {
        extract()
    }

    override fun extract() {
        targetTypeMirror = getValueFromAnnotation<TypeMirror>(
            element,
            AutoComponent::class.java,
            ANNOTATION_TARGET
        )
        if (targetTypeMirror == null) {
            targetTypeMirror = componentElement.asType()
        }

        dependenciesTypeMirrors = findTypeMirrors(element, ANNOTATION_DEPENDENCIES)
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES)
        superinterfacesTypeMirrors = findTypeMirrors(element, ANNOTATION_SUPERINTERFACES)
        subcomponentsTypeMirrors = findTypeMirrors(element, ANNOTATION_SUBCOMPONENTS)

        var includesExtractor: ComponentExtractor? = null
        val includesTypeMirror = getValueFromAnnotation<TypeMirror>(
            element,
            AutoComponent::class.java,
            ANNOTATION_INCLUDES
        )
        if (includesTypeMirror != null) {
            val includesElement = MoreTypes.asElement(includesTypeMirror)
            if (!MoreElements.isAnnotationPresent(includesElement, AutoComponent::class.java)) {
                errors.parent.addInvalid(
                    includesElement,
                    "Included element must be annotated with @AutoComponent"
                )
                return
            }

            if (element == includesElement) {
                errors.addInvalid(
                    "Auto component %s cannot include himself",
                    element.simpleName.toString()
                )
                return
            }

            includesExtractor = ComponentExtractor(
                includesElement, includesElement, types, elements, errors.parent
            )

            if (errors.parent.hasErrors()) {
                return
            }
        }

        if (includesExtractor != null) {
            dependenciesTypeMirrors.addAll(includesExtractor.dependenciesTypeMirrors)
            modulesTypeMirrors.addAll(includesExtractor.modulesTypeMirrors)
            superinterfacesTypeMirrors.addAll(includesExtractor.superinterfacesTypeMirrors)
            subcomponentsTypeMirrors.addAll(includesExtractor.subcomponentsTypeMirrors)
        }

        scopeAnnotationTypeMirror = findScope()
    }

    private fun findTypeMirrors(element: Element, name: String): MutableList<TypeMirror> {
        val addsTo = name == ANNOTATION_SUBCOMPONENTS
        val typeMirrors = ArrayList<TypeMirror>()
        val values =
            getValueFromAnnotation<List<AnnotationValue>>(element, AutoComponent::class.java, name)
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
        // first look on the @AutoComponent annotated element
        var annotationMirror = findScope(element)
        if (annotationMirror == null && element !== componentElement) {
            // look also on the real component element, if @AutoComponent is itself on
            // an another annotation
            annotationMirror = findScope(componentElement)
        }

        return annotationMirror
    }

    private fun findScope(element: Element): AnnotationMirror? {
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
