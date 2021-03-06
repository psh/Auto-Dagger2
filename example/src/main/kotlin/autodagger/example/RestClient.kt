package autodagger.example

import autodagger.AutoExpose
import javax.inject.Inject

@AutoExpose(KotlinExampleApplication::class)
@DaggerScope(KotlinExampleApplication::class)
class RestClient @Inject constructor() {
}
