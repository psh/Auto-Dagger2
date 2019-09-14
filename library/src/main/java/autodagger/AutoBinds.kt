package autodagger

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Target(CLASS)
annotation class AutoBinds(
    vararg val value: KClass<*> = [Unit::class]
)
