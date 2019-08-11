# Auto Dagger2

[![](https://jitpack.io/v/matfax/auto-dagger2.svg)](https://jitpack.io/#matfax/auto-dagger2)
[![Build Status](https://travis-ci.com/matfax/auto-dagger2.svg?branch=master)](https://travis-ci.com/matfax/auto-dagger2)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8ff996f65cb94130bd4f95f4df57c522)](https://www.codacy.com/app/matfax/auto-dagger2?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matfax/auto-dagger2&amp;utm_campaign=Badge_Grade)
![GitHub License](https://img.shields.io/github/license/matfax/auto-dagger2.svg)
![GitHub last commit](https://img.shields.io/github/last-commit/matfax/auto-dagger2.svg)
![Libraries.io for GitHub](https://img.shields.io/librariesio/github/matfax/auto-dagger2.svg)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=matfax/auto-dagger2)](https://dependabot.com)

Auto Dagger2 is an annotation processor built on top of the Dagger2 annotation processor that takes the work
out of maintaining components, subcomponents and modules.

The goal is to reduce the boilerplate code required by Dagger2 when you have "empty" or simple 
components. It is usually the case in Android development.  

You can also mix manually written components with the ones generated by Auto Dagger2. Auto 
Dagger2 produces the human-readable code you would (hopefully) write yourself.


## Getting started

```kotlin
@AutoComponent
@Singleton
class ExampleApplication : Application() {
}
```

It generates `ExampleApplicationComponent`

```java
@Component
@Singleton
public interface ExampleApplicationComponent { 
}
```

As you can see, the `@Singleton` annotation is applied to the generated component as well.


## API

### @AutoComponent

Annotate a class with `@AutoComponent` to generated an associated Component.
On the component, you can add dependencies, modules and superinterfaces.

```kotlin
@AutoComponent(
    modules = [MainActivity.ModuleOne::class],
    dependencies = [ExampleApplication::class],
    superinterfaces = [ExampleApplication::class, GlobalComponent::class],
)
@Singleton
class MainActivity : Activity() {
}
```

It generates `MainActivityComponent`

```java
@Component(
    dependencies = ExampleApplicationComponent.class,
    modules = MainActivity.Module.class
)
@Singleton
public interface MainActivityComponent extends ExampleApplicationComponent, GlobalComponent {
}
```


### @AutoInjector

`@AutoInjector` allows to add injector methods into a generated component.  

```kotlin
@AutoInjector(FirstActivity::class)
class ObjectA {
}
```

It updates the `MainActivityComponent` by adding the following method:

```java
@Component(
    dependencies = ExampleApplicationComponent.class,
    modules = MainActivity.Module.class
)
@Singleton
public interface MainActivityComponent extends ExampleApplicationComponent, GlobalComponent {
  void inject(ObjectA objectA);
}
```

If you apply the `@AutoInjector` on the same class that has the `@AutoComponent` annotation, you can skip the value member:

```kotlin
@AutoInjector
@AutoComponent(
    modules = [MainActivity.ModuleOne::class],
    dependencies = [ExampleApplication::class],
    superinterfaces = [ExampleApplication::class, GlobalComponent::class],
)
@Singleton
class MainActivity : Activity() {
}
```

If your class have parameterized type, you can also specify it:

```kotlin
@AutoInjector(value = [MainActivity::class], parameterizedTypes = [String::class, String::class])
class MyObject3<T, E> {
    val t: T? = null
    val e: E? = null
}
```


### @AutoExpose

`@AutoExpose` allows to expose a dependency within a generated component.  

```kotlin
@AutoExpose(MainActivity::class)
class SomeObject @Inject constructor() {
}
```

It updates the `MainActivityComponent` by adding the following method:

```java
@Component(
    dependencies = ExampleApplicationComponent.class,
    modules = MainActivity.Module.class
)
@Singleton
public interface MainActivityComponent extends ExampleApplicationComponent, GlobalComponent {
  SomeObject someObject();
}
```

If you apply the `@AutoExpose` on the same class that has the `@AutoComponent` annotation, you can skip the value member:

```kotlin
@AutoInjector
@AutoComponent(
    modules = [MainActivity.ModuleOne::class],
    dependencies = [ExampleApplication::class],
    superinterfaces = [ExampleApplication::class, GlobalComponent::class],
)
@AutoExpose
@Singleton
class MainActivity : Activity() {
}
```

`@AutoExpose` can also expose dependency from a module's provider method:

```kotlin
@Provides
@DaggerScope(FirstActivity::class)
@Named("1")
@AutoExpose(FirstActivity::class)
fun providesMyObject4Qualifier1(): MyObject4 {
    return MyObject4()
}
```

If your class have parameterized type, you can also specify it:

```java
@AutoExpose(value = MainActivity.class, parameterizedTypes = {String.class, String.class})
@Singleton
public class MyObject3<T, E> {
    private T t;
    private E e;

    @Inject
    public MyObject3() {
    }
}
```


## Reuse `@AutoComponent`

You can reuse `@AutoComponent` by creating an annotation that is itself annotated with `@AutoComponent`.

```java
@AutoComponent(
        dependencies = MyApp.class,
        superinterfaces = {HasDependenciesOne.class, HasDependenciesTwo.class},
        modules = StandardModule.class
)
public @interface StandardActivityComponent { }
```

You can then create an auto component that reuse directly that annotation.  
It will adds to the already defined dependencies, modules and superinterfaces.

```java
@AutoComponent(
        modules = SixthActivity.Module.class,
        includes = StandardActivityComponent.class)
@Singleton
public class SixthActivity extends Activity { }
```

You can also directly annotate the class:

```java
@StandardActivityComponent
@Singleton
public class SixthActivity extends Activity { }
```


## Scope

Whenever you use `@AutoComponent`, you also need to annotate the class with a dagger scope annotation (an annotation that is itself annotated with `@Scope`).
Auto Dagger2 will detect this annotation, and will apply it on the generated component.

If you don't provide scope annotation, the generated component will be unscoped.


## Installation

Beware that the groupId changed to **com.github.lukaspili.autodagger2**

```groovy
apply plugin: 'com.android.application'

dependencies {
    annotationProcessor 'com.github.matfax.auto-dagger2:compiler:1.2.0'
    implementation 'com.github.matfax.auto-dagger2:library:1.2.0'

    annotationProcessor 'com.google.dagger:dagger-compiler:2.17'
    implementation 'com.google.dagger:dagger:2.17'
    compileOnly 'javax.annotation:javax.annotation-api:1.3.2' // Android only
}
```


## Status

Stable API.  

Auto Dagger2 was extracted from Auto Mortar to work as a standalone library.  
You can find more about Auto Mortar here:
[https://github.com/lukaspili/Auto-Mortar](https://github.com/lukaspili/Auto-Mortar)


## Author

- Lukasz Piliszczuk ([@lukaspili](https://twitter.com/lukaspili))


## License

Auto Dagger2 is released under the MIT license. See the LICENSE file for details.
