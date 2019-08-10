package autodagger.example_kotlin.first

import android.app.Activity
import android.os.Bundle
import autodagger.AutoComponent
import autodagger.AutoExpose
import autodagger.AutoInjector
import autodagger.example_kotlin.DaggerScope
import autodagger.example_kotlin.KotlinExampleApplication
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Named

/**
 * Showcase: @AutoComponent
 */
@AutoComponent(
        modules = [FirstActivity.ModuleOne::class, FirstActivity.ModuleTwo::class],
        dependencies = [KotlinExampleApplication::class],
        superinterfaces = [HasDependenciesOne::class, HasDependenciesTwo::class],
        subcomponents = [MySubObject1::class, MySubObject2::class, MyManualSubcomponent::class])
@AutoInjector
@DaggerScope(FirstActivity::class)
class FirstActivity : Activity() {

    private val component: FirstActivityComponent by lazy {
        DaggerFirstActivityComponent.builder()
                .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
                .moduleOne(ModuleOne())
                .moduleTwo(ModuleTwo())
                .build()
    }

    var mySubObject1: MySubObject1? = null
        @Inject set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component.inject(this)

        component.plusMySubObject2Component(MySubObject2.Module(""), MySubObject2.ModuleTwo())
                .plusMySubObject1Component()
                .inject(mySubObject1)
    }

    @Module
    class ModuleOne {

        @Provides
        @DaggerScope(FirstActivity::class)
        fun providesMyObject2(): MyObject2<String, String> {
            return MyObject2()
        }

        @Provides
        @DaggerScope(FirstActivity::class)
        fun providesMyObject3(): MyObject3<String, String> {
            return MyObject3<String, String>()
        }

        @Provides
        @DaggerScope(FirstActivity::class)
        @Named("1")
        @AutoExpose(FirstActivity::class)
        fun providesMyObject4Qualifier1(): MyObject4 {
            return MyObject4()
        }

        @Provides
        @DaggerScope(FirstActivity::class)
        @Named("2")
        @AutoExpose(FirstActivity::class)
        fun providesMyObject4Qualifier2(): MyObject4 {
            return MyObject4()
        }

        @Provides
        @DaggerScope(FirstActivity::class)
        fun providesMySubObject1(): MySubObject1 {
            return MySubObject1()
        }
    }

    @Module
    class ModuleTwo
}

interface HasDependenciesTwo