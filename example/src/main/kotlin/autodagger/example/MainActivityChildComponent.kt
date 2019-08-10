package autodagger.example

import autodagger.AutoComponent

@AutoComponent(dependencies = [MainActivity::class])
annotation class MainActivityChildComponent