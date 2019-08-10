package autodagger.example_kotlin.second

import autodagger.AutoComponent
import autodagger.example_kotlin.KotlinExampleApplication
import autodagger.example_kotlin.first.HasDependenciesOne
import autodagger.example_kotlin.first.HasDependenciesTwo
import dagger.Module

@AutoComponent(
        dependencies = [KotlinExampleApplication::class],
        superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
        modules = [StandardModule::class]
)
annotation class StandardActivityComponent1

@Module
class StandardModule