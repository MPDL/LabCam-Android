package com.mpdl.labcam.mvvm.vm

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.repository.LoginRepository
import com.mpdl.labcam.mvvm.repository.bean.ChannelBean
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean
import com.mpdl.labcam.mvvm.repository.bean.LoginResponse
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.vm.uistate.LoginUiState
import com.mpdl.mvvm.base.BaseResult
import com.mpdl.mvvm.base.BaseViewModel
import com.mpdl.mvvm.globalsetting.IResponseErrorListener
import timber.log.Timber

class LoginViewModel(application: Application,
                     mainRepository: LoginRepository,
                     responseErrorListener: IResponseErrorListener):
    BaseViewModel<LoginRepository, LoginUiState>(application,mainRepository,responseErrorListener) {
    private var loginUiState = MutableLiveData<LoginUiState>(LoginUiState())
    override fun getUiState(): LiveData<LoginUiState> = loginUiState

    private var directoryData = MutableLiveData<List<KeeperDirectoryBean>>()
    fun getDirectoryData(): LiveData<List<KeeperDirectoryBean>> = directoryData

    private var uploadFileUrl = MutableLiveData<String>()
    fun getUploadFileUrl(): LiveData<String> = uploadFileUrl

    fun cleanUiState(){
        loginUiState.postValue(LoginUiState())
    }

    fun getChannels():List<ChannelBean>{
        var channels = mutableListOf<ChannelBean>()
        channels.add(ChannelBean("KEEPER","https://keeper.mpdl.mpg.de"))
        channels.add(ChannelBean("SeaCloud.cc","https://seacloud.cc"))
        channels.add(ChannelBean("Others","https://"))
        return channels
    }

    fun login(username: String, password: String){
        loginUiState.postValue(LoginUiState(loading = true))
        apply(object : ResultCallBack<LoginResponse>{
            override suspend fun callBack(): BaseResult<LoginResponse>{
                val result = mRepository.login(username,password)
                loginUiState.postValue(LoginUiState(loading = false))
                return result
            }
        },{
            if (!TextUtils.isEmpty(it.token)){
                MainActivity.setToken(it.token)
                loginUiState.postValue(LoginUiState(loginSuccess = true))
            }else{
                loginUiState.postValue(LoginUiState(showToastMsg = mApplication.getString(R.string.login_fail)))
            }
        },{
            loginUiState.postValue(LoginUiState(showToastMsg = mApplication.getString(R.string.login_fail)))
        })
    }

    fun getRepos(){
        apply(object : ResultCallBack<List<KeeperDirectoryBean>>{
            override suspend fun callBack(): BaseResult<List<KeeperDirectoryBean>>
                    = mRepository.getRepos()
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

    fun getUploadLink(saveDirectoryBean: SaveDirectoryBean){
        loginUiState.postValue(LoginUiState(loading = true))
        apply(object : ResultCallBack<String>{
            override suspend fun callBack(): BaseResult<String>
                    = mRepository.getUploadLink(saveDirectoryBean.repoId,saveDirectoryBean.path)
        },{
            Timber.d("uploadUrl: $it")
            if (!TextUtils.isEmpty(it)){
                MainActivity.setUploadUrl(it)
                uploadFileUrl.postValue(it)
                loginUiState.postValue(LoginUiState())
            }else{
                loginUiState.postValue(LoginUiState(showFileSelectorDialog = true))
            }
        },{
            loginUiState.postValue(LoginUiState(showFileSelectorDialog = true))
        })
    }


}