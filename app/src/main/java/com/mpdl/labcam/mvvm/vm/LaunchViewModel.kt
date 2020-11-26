package com.mpdl.labcam.mvvm.vm

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdl.labcam.mvvm.repository.MainRepository
import com.mpdl.mvvm.base.BaseUiState
import com.mpdl.mvvm.base.BaseViewModel
import com.mpdl.mvvm.globalsetting.IResponseErrorListener

class LaunchViewModel(application: Application,
                      mainRepository: MainRepository,
                      responseErrorListener: IResponseErrorListener):
    BaseViewModel<MainRepository, BaseUiState>(application,mainRepository,responseErrorListener) {
    private var launchUiState = MutableLiveData<BaseUiState>(BaseUiState())
    override fun getUiState(): LiveData<BaseUiState> = launchUiState




}