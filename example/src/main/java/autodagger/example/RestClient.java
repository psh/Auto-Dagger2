package autodagger.example;

import javax.inject.Inject;

import autodagger.AutoExpose;

/**
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
@AutoExpose(MyApp.class)
@DaggerScope(MyApp.class)
@Deprecated
public class RestClient {

    @Inject
    public RestClient() {
    }
}
