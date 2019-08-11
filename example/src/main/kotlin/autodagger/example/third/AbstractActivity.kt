package autodagger.example.third

import android.app.Activity
import android.os.Bundle
import autodagger.AutoInjector
import autodagger.example.DaggerScope
import autodagger.example.KotlinExampleApplication
import autodagger.example.second.StandardActivityComponent1

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