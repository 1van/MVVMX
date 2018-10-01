package io.ivan.mvvmx.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.ivan.mvvmx.DataBinding;
import io.ivan.mvvmx.MVVMX;
import io.ivan.mvvmx.ViewModel;
import io.ivan.mvvmx.sample.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @ViewModel
    MainViewModel viewModel;

    @DataBinding(value = R.layout.activity_main, BR = 1)
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MVVMX.bind(this);
    }

}
