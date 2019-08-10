package autodagger.example

import android.app.Activity
import autodagger.AutoComponent

@AutoComponent(dependencies = [KotlinExampleApplication::class])
@DaggerScope(MainActivity::class)
class MainActivity : Activity()