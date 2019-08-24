package autodagger.example.third

import autodagger.AutoComponent
import autodagger.example.KotlinExampleApplication
import autodagger.example.first.HasDependenciesOne
import autodagger.example.first.HasDependenciesTwo
import dagger.Module

@AutoComponent(
    dependencies = [KotlinExampleApplication::class],
    superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
    modules = [StandardModule3::class]
)
annotation class StandardActivityComponent3

@Module
class StandardModule3