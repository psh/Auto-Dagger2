package autodagger.example.fourth

import android.app.Activity
import android.os.Bundle
import autodagger.example.DaggerScope
import autodagger.example.KotlinExampleApplication

/**
 * Showcase: @AutoComponent from annotation
 */
@StandardActivityComponent2
@DaggerScope(FifthActivity::class)
class FifthActivity : Activity() {

    private val component: FifthActivityComponent by lazy {
        DaggerFifthActivityComponent.builder()
            .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component.inject(this)
    }
}