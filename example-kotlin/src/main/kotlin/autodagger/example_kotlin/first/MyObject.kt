package autodagger.example_kotlin.first

import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.example_kotlin.DaggerScope
import javax.inject.Inject

@AutoInjector(FirstActivity::class)
@AutoExpose(FirstActivity::class)
@DaggerScope(FirstActivity::class)
class MyObject @Inject constructor() {
}