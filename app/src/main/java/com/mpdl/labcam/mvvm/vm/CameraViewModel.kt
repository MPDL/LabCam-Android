package com.mpdl.labcam.mvvm.vm

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdl.labcam.mvvm.repository.LoginRepository
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.vm.uistate.CameraUiState
import com.mpdl.mvvm.base.BaseResult
import com.mpdl.mvvm.base.BaseViewModel
import com.mpdl.mvvm.globalsetting.IResponseErrorListener
import timber.log.Timber

class CameraViewModel(application: Application,
                      repository: LoginRepository,
                      responseErrorListener: IResponseErrorListener):
    BaseViewModel<LoginRepository,CameraUiState>(application, repository, responseErrorListener) {
    private var cameraUiState = MutableLiveData<CameraUiState>(CameraUiState())
    override fun getUiState(): LiveData<CameraUiState> = cameraUiState
    private var directoryData = MutableLiveData<List<KeeperDirectoryBean>>()
    fun getDirectoryData() = directoryData

    fun getRepos(){
        apply(object : ResultCallBack<List<KeeperDirectoryBean>>{
            override suspend fun callBack(): BaseResult<List<KeeperDirectoryBean>>
                    = mRepository.getRepos("mine")
        },{
            directoryData.postValue(it)
        },{
            directoryData.postValue(null)
        })
    }
    fun getDir(saveDirectoryBean: SaveDirectoryBean){
        apply(object : ResultCallBack<List<KeeperDirectoryBean>>{
            override suspend fun callBack(): BaseResult<List<KeeperDirectoryBean>>
                    = mRepository.getDir(saveDirectoryBean.repoId,saveDirectoryBean.path,"d")
        },{
            directoryData.postValue(it)
        },{
            directoryData.postValue(null)
        })
    }

    fun checkDirPath(saveDirectoryBean: SaveDirectoryBean){
        cameraUiState.postValue(CameraUiState(loading = true))
        apply(object : ResultCallBack<String>{
            override suspend fun callBack(): BaseResult<String>
                    = mRepository.getUploadLink(saveDirectoryBean.repoId,saveDirectoryBean.path)
        },{
            Timber.d("uploadUrl: $it")
            if (!TextUtils.isEmpty(it)){
                MainActivity.setUploadUrl(it)
                cameraUiState.postValue(CameraUiState(checkoutDirPathSuc = true))
            }else{
                cameraUiState.postValue(CameraUiState(showFileDirDialog = true))
            }
        },{
            cameraUiState.postValue(CameraUiState(showFileDirDialog = true))
        })
    }

    fun getUploadLink(saveDirectoryBean: SaveDirectoryBean){
        cameraUiState.postValue(CameraUiState(loading = true))
        apply(object : ResultCallBack<String>{
            override suspend fun callBack(): BaseResult<String>
                    = mRepository.getUploadLink(saveDirectoryBean.repoId,saveDirectoryBean.path)
        },{
            Timber.d("uploadUrl: $it")
            if (!TextUtils.isEmpty(it)){
                MainActivity.setUploadUrl(it)
                cameraUiState.postValue(CameraUiState(uploadUrlSuc = true))
            }else{
                cameraUiState.postValue(CameraUiState(showToastMsg = "get upload link fail"))
            }
        },{
            cameraUiState.postValue(CameraUiState(showToastMsg = "get upload link fail ${it.message}"))
        })
    }

    fun cleanUiState(){
        cameraUiState.postValue(CameraUiState())
    }


}