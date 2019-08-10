package autodagger.example;

import autodagger.AutoComponent;

/**
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
@Deprecated
@AutoComponent(
        dependencies = MyApp.class,
        superinterfaces = {HasDependenciesOne.class, HasDependenciesTwo.class},
        modules = StandardModule.class
)
public @interface StandardActivityComponent1 {
}
