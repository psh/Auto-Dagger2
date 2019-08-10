package autodagger.example

import autodagger.AutoComponent

@AutoComponent(dependencies = [MainActivity::class])
@DaggerScope(Container1::class)
class Container1 {
}