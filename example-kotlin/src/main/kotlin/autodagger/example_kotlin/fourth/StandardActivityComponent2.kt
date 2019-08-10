package autodagger.example_kotlin.fourth

import autodagger.AutoComponent
import autodagger.AutoInjector
import autodagger.example_kotlin.KotlinExampleApplication
import autodagger.example_kotlin.first.HasDependenciesOne
import autodagger.example_kotlin.first.HasDependenciesTwo
import autodagger.example_kotlin.second.StandardModule

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