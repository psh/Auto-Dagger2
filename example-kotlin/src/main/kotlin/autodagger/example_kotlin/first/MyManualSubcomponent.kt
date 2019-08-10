package autodagger.example_kotlin.first

import autodagger.example_kotlin.DaggerScope
import dagger.Subcomponent

@Subcomponent
@DaggerScope(MyManualSubcomponent::class)
interface MyManualSubcomponent