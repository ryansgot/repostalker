# Repostalker

This app starts with the bypass github organization, displaying the list of users along with their profile photos. Clicking on a member will display a list of user names and profile images of who follows them alphabetically. This process will repeat itself for each newly-displayed user. At any point the user list is shown, you may search for users by name and filter the users to be shown. Part of the solution will be tests which demonstrate the correctness of the application's functionality. Furthermore, in order to support the desire to not hit the network all the time, offline mode/caching will also be developed. The user will be able to determine the cache invalidation strategy.

## The Design

The Views (any activities/fragments) will determine what to show and have two one-way communication channels with a single dependency, their Presenter. The first channel is a downward channel--that is, the view will tell the presenter about user actions that have occurred, giving the presenter all the data it needs to make the decision of what to do; the View will not wait upon a response. The presenter will then be responsible for performing the necessary behaviors in response to the user action (such as getting data). When the data is ready or the behavior is finished, it will send an event up the other one-way channel to the view. The view will operate entirely on the UI Thread. This fact is essential for understanding when you look at presenters so that you can be confident that subscription management is handled correctly.

Just as the View will not be responsible for determining what to do, the presenter will not be responsible for determining where to get data. For that logic, it will depend upon the Interactor. The Interactor will determine which data source to use, and send back observable objects created by the correct data source or data sources, but it will not have an opinion as to how the data is used when it returns. The presenter will make those decisions.

The interactor is a single object whose only purpose is to abstract away the source of events from any presenters. Although it sounds as though this may be quite a large class, its size will not be a problem because it is not handling state. Every reference it has is final and application scoped. The state is managed in the data sources. The following are the relevant data sources for this project:
1. The Database
2. The Network
3. The Android System
4. SharedPreferences
5. The User - by way of being told about events by the presenter

Data Sources are pretty uninteresting. They just provide access to data in a way that is easily consumable by the Presenters.

## What I'm attempting to show

* This design separates concerns, but not too much that it becomes difficult to explain where a particular piece of logic should exist. A developer, when given a diagram of the architectural pattern and a description of the responsibilities will know where to look when he/she needs to work on something.
* You can sufficiently automate the testing of each layer of the app as you write it. Supposing you wanted to start with the View layer. You could completely test the view layer before anything in the Presenter layer is developed. All that must be determined is the contract between layers in order to get started on any given layer.
* Various kinds of layouts/screen sizes can be facilitated by this approach because the Views work the same way regardless of where they appear in the app. This is facilitated by the fact that there are no dependencies of one view on another.
* Much of the code that you write can be shared across platforms. See the fact that the entirety of the Presenter and Interactor layers (and some of the data source layer) are written in the shared subproject (a java/kotlin project) 

## How I'm going to develop it

0. I will first come to a decision on the contract between the Views and Presenters of the application.
1. I will use this contract to start with the View layer for three reasons:
  1. It enables a good demonstration of the two one-way channels in the design
  2. It will help me think about all of the data that is necessary
  3. I can focus on what the requirements compel me to show
2. Then I will develop the data layer. This will help me determine two things:
  1. What the contract will be between the Interactor and the Data Sources.
  2. How business logic should operate because, at this point, I will understand how data will be provided back to the presenters and what the error cases are.
3. Then I will develop the Interactor layer. This will be pretty straightforward.
4. Then I will develop the Presenter layer.

At each step (besides step 0), I'll develop set of tests that demonstrate the correctness of the behaviors of the layer. How will I know I haven't missed anything? I'm pretty sure I'll miss some things as I go, but there will be a coverage report that will at least give me an indication. My actual target percentage is 80%.

# Possible Extensions

The presenter and interactor will not need to know the platform on which they are operating (besides that they're running on the JVM). As such it would not be difficult to develop a companion desktop or command-line app that utilizes:
* All of the presenter code
* All of the interactor code
* Much of the data source code including:
  * The Database
  * The Network

If I have time, then I'll develop both.

# Development

## Tools used
* Kotlin
* (RxJava2)[https://github.com/ReactiveX/RxJava/blob/2.x/README.md]
* (Dagger2)[https://google.github.io/dagger/users-guide]
* (Moshi and Moshi Codegen)[https://github.com/square/moshi]
* (Retrofit2)[https://square.github.io/retrofit/] with (RxJava call adapter factory)[https://github.com/square/retrofit/tree/master/retrofit-adapters/rxjava2]
* (AutoDsl)[https://github.com/juanchosaravia/autodsl/blob/master/README.md]
* (forsuredb)[http://forsuredb.org/]

## Layout

The project is broken up mostly between the app module and the shared module. The app module contains all of the dependency injection code as well as all of the android-specific code--all of the view layer code and some of the data source layer code. The shared module contains code that is not specific to android--all of the presenter and interactor layer and some of the data source layer. It was done this way for two main reasons:
1. To demonstrate the independence of the majority of the application's business logic from the android framework and enforce the strict independence
2. To share code with two sibling modules that would be a (TornadoFX)[https://tornadofx.io/] GUI application and a CLI application using (CliKt)[https://ajalt.github.io/clikt/]. Unfortunately these two applications were not written, but the point remains that we could write them completely reusing much of the code. Even the database access code could be regenerated using (forsuredb-jdbc)[https://github.com/ryansgot/forsuredbcompiler/tree/master/forsuredbjdbc].

# Testing

The ViPID pattern opts for a very simple dependency structure. As such, injecting dependencies into the objects you're testing is so easy to do that the framework is not necessary when testing.

## DI Technique and UI Testing
The [Components interface](app/src/main/java/com/fsryan/repostalker/Components.kt) is implemented both in the main source set ([DepInjector](app/src/main/java/com/fsryan/repostalker/DepInjector.kt)) and in the androidTest source set ([FakeComponents](app/src/androidTest/java/com/fsryan/repostalker/FakeComponents.kt)). The components are loaded into the application by way of the components loader. There are different implementations of the components loader in the debug and release source sets:
* [debug ComponentsLoader](app/src/debug/java/com/fsryan/repostalker/ComponentsLoader.kt) 
* [release ComponentsLoader](app/src/release/java/com/fsryan/repostalker/ComponentsLoader.kt) 

The above is the only difference between the release and the debug source sets. It is done this way so that the debug version can check whether it is running in a test environment, and if so, returns [FakeComponents](app/src/androidTest/java/com/fsryan/repostalker/FakeComponents.kt). If not, the debug version returns the production [DepInjector](app/src/main/java/com/fsryan/repostalker/DepInjector.kt). The release version only returns [DepInjector](app/src/main/java/com/fsryan/repostalker/DepInjector.kt).

[FakeComponents](app/src/androidTest/java/com/fsryan/repostalker/FakeComponents.kt) mirrors the Dagger 2 component model for doing injection, but it implements all of the component interfaces itself. It has an additional injection registry that allows the test classes to register the objects they would like to inject. You can see this in [MainActivityTest](app/src/androidTest/java/com/fsryan/repostalker/main/MainActivityTest.kt) in that the `Main.Presenter` object is registered to be injected into the `MainActivity`. This `Main.Presenter` instance is instantiated and configured within the test class itself. This means that one test may inject one version of a dependency and another test inject another version of a dependency.

This aids in injecting mocks into your views so that you can inject events and make verifications.

## Tools used

* (Espresso)[https://developer.android.com/training/testing/espresso/]
* (ActivityTestRule)[https://developer.android.com/reference/android/support/test/rule/ActivityTestRule]
* (mockk)[https://mockk.io] and (mockk-android)[https://mockk.io/ANDROID.html]
* (JUnit5)[https://junit.org/junit5/] for jvm testing
* (JUnit4)[https://junit.org/junit4/] for android testing
  * (ParameterizedTest)[https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests]
  * (MockKExtension)[https://mockk.io/#junit5]
* (RxJavaPlugins)[http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/plugins/RxJavaPlugins.html] (and RxAndroidPlugins)
* (RxJava TestObserver)[http://reactivex.io/RxJava/javadoc/io/reactivex/observers/TestObserver.html]
* (JaCoCo)[https://www.eclemma.org/jacoco/] for code coverage

## Data Source testing
Tests of the shared preferences and sqlite databases occur on the android device. They output their state to the /sdcard/test-repostalker-out/ directory via the following two JUnit4 rules:
* [DBTestRule](app/src/androidTest/java/com/fsryan/repostalker/data/db/DBTestRule.kt)
* [PrefsTestRule](app/src/androidTest/java/com/fsryan/repostalker/data/prefs/PrefsTestRule.kt)

Data sources are tested mostly on the android device. For example, shared preferences and SQLite database access are tested there. Navigator, on the other hand, is tested on the JVM because there is nothing specific about the core of the Navigator (a stack) that is supplied by the Android Framework.