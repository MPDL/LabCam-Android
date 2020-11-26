package com.mpdl.labcam.di

import com.mpdl.mvvm.globalsetting.IGlobalConfig
import com.mpdl.mvvm.globalsetting.IGlobalHttpInterceptor
import com.mpdl.mvvm.globalsetting.IResponseErrorListener
import com.mpdl.labcam.GlobalConfig
import com.mpdl.labcam.GlobalHttpInterceptor
import com.mpdl.labcam.GlobalResponseErrorListener
import com.mpdl.labcam.mvvm.repository.LoginRepository
import com.mpdl.labcam.mvvm.repository.MainRepository
import com.mpdl.labcam.mvvm.vm.CameraViewModel
import com.mpdl.labcam.mvvm.vm.LaunchViewModel
import com.mpdl.labcam.mvvm.vm.LoginViewModel
import com.mpdl.labcam.mvvm.vm.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val mAppModule = module {

    single<IGlobalConfig>{ GlobalConfig() }

    single<IResponseErrorListener> { GlobalResponseErrorListener(androidContext()) }

    single<IGlobalHttpInterceptor> { GlobalHttpInterceptor() }

}


val mViewModelModule = module {
    viewModel { MainViewModel(get(),MainRepository(get()),get(),get())}
    viewModel { LaunchViewModel(get(),MainRepository(get()),get())}
    viewModel { CameraViewModel(get(),LoginRepository(get()),get())}
    viewModel { LoginViewModel(get(), LoginRepository(get()),get())}
}