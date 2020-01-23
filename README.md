# Android Arch annotation processor

### What?
This library aims to reduce boilerplate in projects based upon our architecture.
The project consists of annotations, used to mark injectable activities and
fragments, as well as viewmodels.

The annotation processor will generate a dagger module, which can be included in
your app component.

### Usage

The following annotations can be used to be processed:
- `AndroidInjected`
- `GenerateViewModelInjector`

Classes annotated with `AndroidInjected` will be processed into a `GeneratedAndroidInjectedModule` module. Each class will be provided by a method in the module annotated with a `@ContributesAndroidInjector` annotation.

Classes annotated with `GenerateViewModelInjector` will be processed into a `GeneratedViewModelModule`. Each class will be provided and bound into a map with a `ViewModelKey`, as seen in our [template project](https://github.com/icapps/android-template-kotlin-viewmodel).