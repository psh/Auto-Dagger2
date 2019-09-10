package autodagger.example.seventh

import autodagger.AutoBinds
import autodagger.example.KotlinExampleApplication

@AutoBinds(KotlinExampleApplication::class)
class MyConcreteClass : DirectlyImplementedInterface, OtherInterestingInterface
