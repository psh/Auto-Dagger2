package autodagger.compiler.subcomponent

import autodagger.AutoSubcomponent
import autodagger.compiler.Errors
import autodagger.compiler.State
import autodagger.compiler.utils.*
import com.google.auto.common.MoreTypes
import dagger.Subcomponent
import javax.inject.Scope
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

class SubcomponentExtractor(override val element: Element, state: State) : DiagnosticsSource {
    private val errors: Errors.ElementErrors = Errors.ElementErrors(state.errors, element)

    var modulesTypeMirrors = mutableListOf<TypeMirror>()
    private var superinterfacesTypeMirrors = mutableListOf<TypeMirror>()
    private var subcomponentsTypeMirrors = mutableListOf<TypeMirror>()
    private var scopeAnnotationTypeMirror: AnnotationMirror? = null

    init {
        extract()
    }

    override fun toDiagnostics(): MutableMap<String, String?> = mutableMapOf(
        "scopeAnnotationTypeMirror" to scopeAnnotationTypeMirror?.toString(),
        "modulesTypeMirrors" to modulesTypeMirrors.toString(),
        "superinterfacesTypeMirrors" to superinterfacesTypeMirrors.toString(),
        "subcomponentsTypeMirrors" to subcomponentsTypeMirrors.toString()
    )

    fun buildModel(state: State) = SubcomponentModel(
        className = element,
        scopeAnnotation = scopeAnnotationTypeMirror,
        modulesTypeNames = modulesTypeMirrors,
        superinterfacesTypeNames = superinterfacesTypeMirrors,
        exposeModels = getAdditions(element, state.exposeExtractors.values.toList()),
        injectorModels = getAdditions(element, state.injectorExtractors.values.toList()),
        subcomponents = subcomponentsTypeMirrors
    )

    private fun extract() {
        modulesTypeMirrors = findTypeMirrors(element, ANNOTATION_MODULES)
        subcomponentsTypeMirrors = findTypeMirrors(element, ANNOTATION_SUBCOMPONENTS)
        if (AutoSubcomponent::class.java.isNotPresentOn(element)) {
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
                val addsTo = name == ANNOTATION_SUBCOMPONENTS
                for (value in it) {
                    if (!validateAnnotationValue(value, name)) {
                        continue
                    }

                    try {
                        val tm = value.value as TypeMirror
                        if (addsTo) {
                            val e = MoreTypes.asElement(tm)
                            if (AutoSubcomponent::class.java.isNotPresentOn(e)
                                && Subcomponent::class.java.isNotPresentOn(e)
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
