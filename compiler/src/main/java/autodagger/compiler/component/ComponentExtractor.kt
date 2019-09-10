package autodagger.compiler.component

import autodagger.AutoComponent
import autodagger.AutoSubcomponent
import autodagger.compiler.Errors
import autodagger.compiler.State
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import dagger.Subcomponent
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * The component element represented by @AutoComponent
 * It's either the element itself, or the element of an annotation if the @AutoComponent
 * is applied on the annotation
 */
class ComponentExtractor(
    val componentElement: Element,
    val element: Element,
    private val state: State
) {
    private val errors: Errors.ElementErrors = Errors.ElementErrors(state.errors, element)

    var targetTypeMirror: TypeMirror? = null
    private var scopeAnnotationTypeMirror: AnnotationMirror? = null
    private var dependenciesTypeMirrors = mutableListOf<TypeMirror>()
    private var modulesTypeMirrors = mutableListOf<TypeMirror>()
    private var superinterfacesTypeMirrors = mutableListOf<TypeMirror>()
    private var subcomponentsTypeMirrors = mutableListOf<TypeMirror>()

    init {
        extract()
    }

    fun buildModel(extractors: Set<ComponentExtractor>) = ComponentModel(
        className = componentElement,
        targetTypeName = targetTypeMirror,
        scopeAnnotation = scopeAnnotationTypeMirror,
        dependenciesTypeNames = dependencies(extractors, state),
        dependenciesTypeMirrors = dependenciesTypeMirrors,
        superinterfacesTypeNames = superinterfacesTypeMirrors,
        modulesTypeNames = modulesTypeMirrors,
        subcomponentsTypeMirrors = subcomponentsTypeMirrors,
        extractor = this@ComponentExtractor
    )

    // check if dependency type mirror references an @AutoComponent target
    // if so, build the TypeName that matches the target component
    // ignore self
    private fun dependencies(extractors: Set<ComponentExtractor>, state: State): List<Element> =
        mutableListOf<Element>().apply {
            mainLoop@ for (typeMirror in dependenciesTypeMirrors) {
                for (componentExtractor in extractors) {
                    if (componentExtractor === this@ComponentExtractor) {
                        continue
                    }

                    if (areTypesEqual(componentExtractor.targetTypeMirror, typeMirror)) {
                        add(componentExtractor.componentElement)
                        continue@mainLoop
                    }
                }
                add(state.processingEnv.typeUtils.asElement(typeMirror))
            }
        }

    private fun extract() {
        targetTypeMirror = getValueFromAnnotation<TypeMirror>(
            element,
            AutoComponent::class.java,
            ANNOTATION_TARGET
        ) ?: componentElement.asType()

        dependenciesTypeMirrors = findTypeMirrors(element, ANNOTATION_DEPENDENCIES)
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES)
        superinterfacesTypeMirrors = findTypeMirrors(element, ANNOTATION_SUPERINTERFACES)
        subcomponentsTypeMirrors = findTypeMirrors(element, ANNOTATION_SUBCOMPONENTS)

        var includesExtractor: ComponentExtractor? = null
        getValueFromAnnotation<TypeMirror>(
            element,
            AutoComponent::class.java,
            ANNOTATION_INCLUDES
        )?.let {
            val includesElement = MoreTypes.asElement(it)
            if (includesElement notAnnotatedWith AutoComponent::class.java) {
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

            includesExtractor = ComponentExtractor(includesElement, includesElement, state)

            if (errors.parent.hasErrors()) {
                return
            }
        }

        includesExtractor?.let {
            dependenciesTypeMirrors.addAll(it.dependenciesTypeMirrors)
            modulesTypeMirrors.addAll(it.modulesTypeMirrors)
            superinterfacesTypeMirrors.addAll(it.superinterfacesTypeMirrors)
            subcomponentsTypeMirrors.addAll(it.subcomponentsTypeMirrors)
        }

        scopeAnnotationTypeMirror = findScope()
    }

    private fun findTypeMirrors(element: Element, name: String): MutableList<TypeMirror> =
        mutableListOf<TypeMirror>().apply {
            val addsTo = name == ANNOTATION_SUBCOMPONENTS
            getValueFromAnnotation<List<AnnotationValue>>(
                element, AutoComponent::class.java, name
            )?.let {
                for (value in it) {
                    if (!validateAnnotationValue(value, name)) {
                        continue
                    }

                    try {
                        val tm = value.value as TypeMirror
                        if (addsTo) {
                            val e = MoreTypes.asElement(tm)
                            if (e notAnnotatedWith AutoSubcomponent::class.java
                                && e notAnnotatedWith Subcomponent::class.java
                            ) {
                                errors.addInvalid(
                                    "@AutoComponent cannot declare a subcomponent that is not annotated with @Subcomponent or @AutoSubcomponent: %s",
                                    e.simpleName.toString()
                                )
                                continue
                            }
                        }
                        add(tm)
                    } catch (e: Exception) {
                        errors.addInvalid(e.message ?: e.javaClass.simpleName)
                        break
                    }

                }
            }
        }

    /**
     * Find annotation that is itself annoted with @Scope
     * If there is one, it will be later applied on the generated component
     * Otherwise the component will be unscoped
     * Throw error if more than one scope annotation found
     */
    private fun findScope(): AnnotationMirror? {
        // first look on the @AutoComponent annotated element
        var annotationMirror = element.findScope(errors)
        if (annotationMirror == null && element !== componentElement) {
            // look also on the real component element, if @AutoComponent is itself on
            // an another annotation
            annotationMirror = componentElement.findScope(errors)
        }

        return annotationMirror
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

