package autodagger.example.first

import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.example.DaggerScope

@AutoInjector(value = [FirstActivity::class], parameterizedTypes = [String::class, String::class])
@AutoExpose(value = [FirstActivity::class], parameterizedTypes = [String::class, String::class])
@DaggerScope(FirstActivity::class)
class MyObject3<T, E> {
    val t: T? = null
    val e: E? = null
}