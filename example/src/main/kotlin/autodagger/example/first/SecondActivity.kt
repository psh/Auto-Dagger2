package autodagger.example.first

import android.app.Activity
import android.os.Bundle
import autodagger.AutoComponent
import autodagger.AutoInjector
import autodagger.example.DaggerScope
import dagger.Component
import dagger.Module
import dagger.Provides

@AutoComponent(
        modules = [SecondActivity.ModuleOne::class, SecondActivity.ModuleTwo::class],
        dependencies = [SecondActivity.SomeOtherComponent::class],
        superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
        subcomponents = [MySubObject1::class]
)
@AutoInjector
@DaggerScope(SecondActivity::class)
class SecondActivity : Activity() {

    private val component: SecondActivityComponent by lazy {
        DaggerSecondActivityComponent.builder()
                .someOtherComponent(DaggerSecondActivity_SomeOtherComponent.create())
                .moduleOne(ModuleOne())
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
    }

    @Module
    class ModuleOne {
        @Provides
        @DaggerScope(SecondActivity::class)
        fun providesMyObject2(): MyObject2<String, String> {
            return MyObject2()
        }
    }

    @Module
    class ModuleTwo

    @Component
    @DaggerScope(SomeOtherComponent::class)
    interface SomeOtherComponent
}
