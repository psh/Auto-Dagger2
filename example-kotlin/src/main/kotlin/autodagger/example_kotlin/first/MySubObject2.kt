package autodagger.example_kotlin.first

import autodagger.AutoInjector
import autodagger.AutoSubcomponent
import autodagger.example_kotlin.DaggerScope

@AutoSubcomponent(
        modules = [MySubObject2.Module::class, MySubObject2.ModuleTwo::class],
        superinterfaces = [MySubObject2.MyInterface::class],
        subcomponents = [MySubObject1::class]
)
@AutoInjector
@DaggerScope(MySubObject2::class)
class MySubObject2 {

    @dagger.Module
    class Module(private val string: String)

    @dagger.Module
    class ModuleTwo

    interface MyInterface
}
