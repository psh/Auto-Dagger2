package autodagger.example.documentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import autodagger.AutoComponent
import dagger.Module
import javax.inject.Singleton

@AutoComponent(
    modules = [MainActivity.ModuleOne::class],
    dependencies = [ExampleApplication::class],
    superinterfaces = [ExampleApplication::class, GlobalComponent::class]
)
@Singleton
class MainActivity : AppCompatActivity() {

    @Module
    class ModuleOne {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
