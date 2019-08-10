package autodagger.example

import android.app.Application
import autodagger.AutoComponent
import autodagger.AutoExpose
import dagger.Provides

@AutoComponent(modules = [KotlinExampleApplication.Module::class])
@DaggerScope(KotlinExampleApplication::class)
class KotlinExampleApplication : Application() {

    val component: KotlinExampleApplicationComponent by lazy {
        DaggerKotlinExampleApplicationComponent.builder().build()
    }

    override fun onCreate() {
        super.onCreate()
    }

    @dagger.Module
    class Module {
        @Provides
        @AutoExpose(KotlinExampleApplication::class)
        @DaggerScope(KotlinExampleApplication::class)
        fun providesRestClient2(): RestClient2 {
            return RestClient2()
        }
    }
}