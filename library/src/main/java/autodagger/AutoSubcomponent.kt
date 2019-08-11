package autodagger

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.reflect.KClass

@Target(CLASS, FILE)
annotation class AutoSubcomponent(
    val modules: Array<KClass<*>> = [],
    val superinterfaces: Array<KClass<*>> = [],
    /**
     * Subcomponents to be declared inside this component
     */
    val subcomponents: Array<KClass<*>> = []
)