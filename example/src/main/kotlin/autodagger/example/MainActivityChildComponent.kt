package autodagger.example

import autodagger.AutoComponent

@AutoComponent(dependencies = arrayOf(MainActivity::class))
annotation class MainActivityChildComponent