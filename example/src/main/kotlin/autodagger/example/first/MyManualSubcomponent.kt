package autodagger.example.first

import autodagger.example.DaggerScope
import dagger.Subcomponent

@Subcomponent
@DaggerScope(MyManualSubcomponent::class)
interface MyManualSubcomponent