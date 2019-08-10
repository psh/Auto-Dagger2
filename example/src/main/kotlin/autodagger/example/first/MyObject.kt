package autodagger.example.first

import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.example.DaggerScope
import javax.inject.Inject

@AutoInjector(FirstActivity::class)
@AutoExpose(FirstActivity::class)
@DaggerScope(FirstActivity::class)
class MyObject @Inject constructor() {
}