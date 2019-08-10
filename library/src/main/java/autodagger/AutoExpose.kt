package autodagger

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@Target(CLASS, FILE, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class AutoExpose(
        vararg val value: KClass<*> = [Unit::class],
        val parameterizedTypes: Array<KClass<*>> = [Unit::class]
)
