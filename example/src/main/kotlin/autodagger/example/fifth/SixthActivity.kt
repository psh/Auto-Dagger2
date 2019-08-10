package autodagger.example.fifth

import android.app.Activity
import android.os.Bundle
import autodagger.AutoComponent
import autodagger.AutoInjector
import autodagger.example.DaggerScope
import autodagger.example.KotlinExampleApplication
import autodagger.example.second.StandardActivityComponent1

/**
 * Showcase: @AutoComponent includes
 */
@AutoComponent(
        modules = [SixthActivity.Module::class],
        includes = StandardActivityComponent1::class
)
@DaggerScope(SixthActivity::class)
@AutoInjector
class SixthActivity : Activity() {

    private val component: SixthActivityComponent by lazy {
        DaggerSixthActivityComponent.builder()
                .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component.inject(this)
    }

    @dagger.Module
    class Module
}