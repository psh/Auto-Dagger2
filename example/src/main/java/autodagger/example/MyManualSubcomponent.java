package autodagger.example;

import dagger.Subcomponent;

/**
 * Created by lukasz on 08/12/15.
 */
@Subcomponent
@Deprecated
@DaggerScope(MyManualSubcomponent.class)
public interface MyManualSubcomponent {
}
