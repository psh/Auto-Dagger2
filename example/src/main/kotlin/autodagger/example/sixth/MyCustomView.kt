package autodagger.example.sixth

import android.content.Context
import android.widget.LinearLayout
import autodagger.AutoInjector
import autodagger.example.first.FirstActivity

/**
 * Showcase: add injector method in some generated component
 */
@AutoInjector(FirstActivity::class)
class MyCustomView(context: Context) : LinearLayout(context)