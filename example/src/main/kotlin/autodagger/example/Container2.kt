package autodagger.example

import autodagger.AutoComponent

@AutoComponent(includes = MainActivityChildComponent::class)
@DaggerScope(Container2::class)
class Container2 {
}