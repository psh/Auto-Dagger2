package autodagger.example.fourth

import autodagger.AutoComponent
import autodagger.AutoInjector
import autodagger.example.KotlinExampleApplication
import autodagger.example.first.HasDependenciesOne
import autodagger.example.first.HasDependenciesTwo
import autodagger.example.second.StandardModule

/**
 * Difference with StandardActivityComponent1 is that the @AutoInjector is applied
 * here
 */
@AutoComponent(
        dependencies = [KotlinExampleApplication::class],
        superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
        modules = [StandardModule::class]
)
@AutoInjector
annotation class StandardActivityComponent2