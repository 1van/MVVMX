# MVVMX
[![](https://jitpack.io/v/1van/MVVMX.svg)](https://jitpack.io/#1van/MVVMX)

A set of MVVM extensions for Android app development in the future.

## Setup

Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependency:
```gradle
dependencies {
    implementation "com.github.1van.MVVMX:mvvmx-runtime:$mvvmx_version"
    kapt "com.github.1van.MVVMX:mvvmx-compiler:$mvvmx_version"
}

```

## Usage
### Activity(Java)
```java
    @ViewModel
    MainViewModel viewModel;
    @DataBinding(value = R.layout.activity_main, BR = 1)
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MVVMX.bind(this);
//        binding.setViewModel(viewModel);
    }
```
### Fragment(Kotlin)
```kotlin
    @ViewModel
    lateinit var viewModel: MainViewModel
    @DataBinding(R.layout.main_fragment, BR = 1)
    lateinit var binding: MainFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        MVVMX.bind(this, inflater, container)
//        binding.viewModel = viewModel
        return binding.root
    }
```
