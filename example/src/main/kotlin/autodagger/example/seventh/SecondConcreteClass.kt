package autodagger.example.seventh

import autodagger.AutoBinds
import autodagger.example.KotlinExampleApplication
import javax.inject.Named

@AutoBinds(KotlinExampleApplication::class)
@Named("interesting interface")
class SecondConcreteClass : ExampleAbstractBaseClass(), OtherInterestingInterface