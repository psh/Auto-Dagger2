package autodagger.example.second

import autodagger.AutoComponent
import autodagger.example.KotlinExampleApplication
import autodagger.example.first.HasDependenciesOne
import autodagger.example.first.HasDependenciesTwo
import dagger.Module

@AutoComponent(
    dependencies = [KotlinExampleApplication::class],
    superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
    modules = [StandardModule::class]
)
annotation class StandardActivityComponent1

@Module
class StandardModule