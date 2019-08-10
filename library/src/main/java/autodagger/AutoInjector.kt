package autodagger

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.reflect.KClass

@Target(CLASS, FILE)
annotation class AutoInjector(
        vararg val value: KClass<*> = [Unit::class],
        val parameterizedTypes: Array<KClass<*>> = [Unit::class]
)
