package autodagger.example.second

import android.app.Activity
import android.os.Bundle
import autodagger.AutoInjector
import autodagger.example.DaggerScope
import autodagger.example.KotlinExampleApplication

/**
 * Showcase: @AutoComponent from annotation
 */
@StandardActivityComponent1
@AutoInjector
@DaggerScope(ThirdActivity::class)
class ThirdActivity : Activity() {

    private val component: ThirdActivityComponent by lazy {
        DaggerThirdActivityComponent.builder()
                .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
    }
}
