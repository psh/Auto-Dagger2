package autodagger.example.third

import android.os.Bundle
import autodagger.example.KotlinExampleApplication

/**
 * Showcase: extends from base class annotated with @AutoComponent
 */
class FourthActivity : AbstractActivity() {

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // do this here, or in the base class
        component = DaggerAbstractActivityComponent.builder()
            .kotlinExampleApplicationComponent((application as KotlinExampleApplication).component)
            .build()
        component.inject(this)
    }
}