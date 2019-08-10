package autodagger.example_kotlin.first

import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.example_kotlin.DaggerScope

@AutoInjector(value = [FirstActivity::class], parameterizedTypes = [String::class, String::class])
@AutoExpose(value = [FirstActivity::class], parameterizedTypes = [String::class, String::class])
@DaggerScope(FirstActivity::class)
class MyObject3<T, E> {
    val t: T? = null
    val e: E? = null
}