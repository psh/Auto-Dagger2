package autodagger.example.first

import autodagger.AutoInjector
import autodagger.AutoSubcomponent
import autodagger.example.DaggerScope

@AutoSubcomponent
@DaggerScope(MySubObject1::class)
@AutoInjector
class MySubObject1