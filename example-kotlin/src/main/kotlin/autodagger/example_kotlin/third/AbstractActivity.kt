package autodagger.example_kotlin.third

import android.app.Activity
import android.os.Bundle
import autodagger.AutoInjector
import autodagger.example_kotlin.DaggerScope
import autodagger.example_kotlin.KotlinExampleApplication
import autodagger.example_kotlin.second.StandardActivityComponent1

@StandardActivityComponent1
@AutoInjector
@DaggerScope(AbstractActivity::class)
abstract class AbstractActivity : Activity() {

    protected lateinit var component: AbstractActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component = DaggerAbstractActivityComponent.builder()
                .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
                .build()
        component.inject(this)
    }
}