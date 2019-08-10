package autodagger.example_kotlin.first

import autodagger.AutoInjector
import autodagger.AutoSubcomponent
import autodagger.example_kotlin.DaggerScope

@AutoSubcomponent
@DaggerScope(MySubObject1::class)
@AutoInjector
class MySubObject1