package io.ivan.mvvmx.sample;

import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableField;

public class MainViewModel extends ViewModel {

    public final ObservableField<String> welcomeText = new ObservableField<>("Hello MVVMX!");

}
