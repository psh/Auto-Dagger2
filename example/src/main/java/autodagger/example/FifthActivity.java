package autodagger.example;

import android.app.Activity;
import android.os.Bundle;

/**
 * Showcase: @AutoComponent from annotation
 *
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
@Deprecated
@StandardActivityComponent2
@DaggerScope(FifthActivity.class)
public class FifthActivity extends Activity {

    private FifthActivityComponent component;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        component = DaggerFifthActivityComponent.builder()
                .myAppComponent(((MyApp) getApplication()).getComponent())
                .build();
        component.inject(this);
    }
}