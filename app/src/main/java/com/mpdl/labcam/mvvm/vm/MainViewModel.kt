package com.mpdl.labcam.mvvm.vm

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdl.labcam.mvvm.repository.MainRepository
import com.mpdl.labcam.mvvm.vm.uistate.MainUiState
import com.mpdl.mvvm.base.BaseViewModel
import com.mpdl.mvvm.globalsetting.IResponseErrorListener
import okhttp3.OkHttpClient

class MainViewModel(application: Application,
                    mainRepository: MainRepository,
                    responseErrorListener: IResponseErrorListener,
                    val okHttpClient: OkHttpClient):
    BaseViewModel<MainRepository, MainUiState>(application,mainRepository,responseErrorListener) {
    private var mainUiState = MutableLiveData<MainUiState>(MainUiState())
    override fun getUiState(): LiveData<MainUiState> = mainUiState




}