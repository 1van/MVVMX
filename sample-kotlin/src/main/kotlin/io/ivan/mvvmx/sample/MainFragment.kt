package io.ivan.mvvmx.sample

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.ivan.mvvmx.DataBinding
import io.ivan.mvvmx.MVVMX
import io.ivan.mvvmx.ViewModel
import io.ivan.mvvmx.sample.databinding.MainFragmentBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    @ViewModel
    lateinit var viewModel: MainViewModel

    @DataBinding(R.layout.main_fragment)
    lateinit var binding: MainFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        MVVMX.bind(this, inflater, container)
        binding.viewModel = viewModel
        return binding.root
    }

}
