package autodagger.example;

import android.app.Activity;
import android.os.Bundle;

import autodagger.AutoInjector;

/**
 * Showcase: @AutoComponent from annotation
 *
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
@Deprecated
@StandardActivityComponent1
@AutoInjector
@DaggerScope(ThirdActivity.class)
public class ThirdActivity extends Activity {

    private ThirdActivityComponent component;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        component = DaggerThirdActivityComponent.builder()
                .myAppComponent(((MyApp) getApplication()).getComponent())
                .build();
        component.inject(this);
    }
}
