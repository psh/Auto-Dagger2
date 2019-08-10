package autodagger.example_kotlin.sixth

import android.content.Context
import android.widget.LinearLayout
import autodagger.AutoInjector
import autodagger.example_kotlin.first.FirstActivity

/**
 * Showcase: add injector method in some generated component
 */
@AutoInjector(FirstActivity::class)
class MyCustomView(context: Context) : LinearLayout(context)