package io.ivan.mvvmx.sample

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField

class MainViewModel : ViewModel() {

    val message = ObservableField("Hello MVVMX!")

}
