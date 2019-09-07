package autodagger.compiler.subcomponent

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

class SubcomponentExtractor(val element: Element, state: State) {
    private val errors: Errors.ElementErrors = Errors.ElementErrors(state.errors, element)

    var modulesTypeMirrors = mutableListOf<TypeMirror>()
    private var superinterfacesTypeMirrors = mutableListOf<TypeMirror>()
    private var subcomponentsTypeMirrors = mutableListOf<TypeMirror>()
    private var scopeAnnotationTypeMirror: AnnotationMirror? = null

    init {
        extract()
    }

    fun buildModel(state: State) = SubcomponentModel(
        className = element,
        scopeAnnotation = scopeAnnotationTypeMirror,
        modulesTypeNames = modulesTypeMirrors,
        superinterfacesTypeNames = superinterfacesTypeMirrors,
        exposeModels = additionsMatchingElement(element.asType(), state.exposeExtractors.values),
        injectorModels = additionsMatchingElement(element.asType(), state.injectorExtractors.values),
        subcomponents = subcomponentsTypeMirrors
    )

    private fun extract() {
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES)
        subcomponentsTypeMirrors = findTypeMirrors(element, ANNOTATION_SUBCOMPONENTS)
        if (element notAnnotatedWith AutoSubcomponent::class.java) {
            return
        }

        superinterfacesTypeMirrors = findTypeMirrors(element, ANNOTATION_SUPERINTERFACES)
        scopeAnnotationTypeMirror = findScope()
    }

    private fun findTypeMirrors(element: Element, name: String) =
        mutableListOf<TypeMirror>().apply {
            getValueFromAnnotation<List<AnnotationValue>>(
                element,
                AutoSubcomponent::class.java,
                name
            )?.let {
                for (value in it) {
                    if (!validateAnnotationValue(value, name)) {
                        continue
                    }

                    try {
                        val tm = value.value as TypeMirror
                        if (name == ANNOTATION_SUBCOMPONENTS) {
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
     * Find annotation that is itself annotated with @Scope
     * If there is one, it will be later applied on the generated component
     * Otherwise the component will be unscoped
     * Throw error if more than one scope annotation found
     */
    private fun findScope(): AnnotationMirror? = element.findScope(errors)

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
