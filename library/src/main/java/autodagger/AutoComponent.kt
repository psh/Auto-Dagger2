package autodagger

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.reflect.KClass

@Target(CLASS, FILE)
annotation class AutoComponent(
        val modules: Array<KClass<*>> = [],
        val dependencies: Array<KClass<*>> = [],
        val superinterfaces: Array<KClass<*>> = [],
        /**
         * Default is the class on which the @AutoComponent annotation is applied
         */
        val target: KClass<*> = Unit::class,
        /**
         * Includes modules, dependencies and superinterfaces from an annotation that is
         * itself annotated with @AutoComponent
         */
        val includes: KClass<out Annotation> = Annotation::class,
        /**
         * Subcomponents to be declared inside this component
         */
        val subcomponents: Array<KClass<*>> = []
)
