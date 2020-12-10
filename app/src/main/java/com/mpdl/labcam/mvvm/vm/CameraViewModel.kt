package com.mpdl.labcam.mvvm.vm

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdl.labcam.mvvm.repository.LoginRepository
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.vm.uistate.CameraUiState
import com.mpdl.labcam.mvvm.vm.uistate.DirTreeViewDialogState
import com.mpdl.labcam.treeviewbase.TreeNode
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

    private var dirDialogState = MutableLiveData<DirTreeViewDialogState>()
    fun getDirDialogState(): MutableLiveData<DirTreeViewDialogState> = dirDialogState


    fun getRepos(){
        apply(object : ResultCallBack<List<KeeperDirItem>>{
            override suspend fun callBack(): BaseResult<List<KeeperDirItem>>
                    = mRepository.getRepos()
        },{
            dirDialogState.postValue(DirTreeViewDialogState(list = it))
        },{
            dirDialogState.postValue(DirTreeViewDialogState())
        })
    }
    fun getDir(node: TreeNode<KeeperDirItem>, dirItem: KeeperDirItem){
        apply(object : ResultCallBack<List<KeeperDirItem>>{
            override suspend fun callBack(): BaseResult<List<KeeperDirItem>>
                    = mRepository.getDir(dirItem.repoId,dirItem.path,"d")
        },{
            dirDialogState.postValue(DirTreeViewDialogState(node=node,list = it))
        },{
            dirDialogState.postValue(DirTreeViewDialogState(node=node,list = null))
        })
    }

    fun checkDirPath(item: KeeperDirItem){
        cameraUiState.postValue(CameraUiState(loading = true))
        apply(object : ResultCallBack<String>{
            override suspend fun callBack(): BaseResult<String>
                    = mRepository.getUploadLink(item.repoId,item.path)
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

    fun getUploadLink(item: KeeperDirItem){
        cameraUiState.postValue(CameraUiState(loading = true))
        apply(object : ResultCallBack<String>{
            override suspend fun callBack(): BaseResult<String>
                    = mRepository.getUploadLink(item.repoId,item.path)
        },{
            Timber.d("uploadUrl: $it")
            if (!TextUtils.isEmpty(it)){
                MainActivity.setUploadUrl(it)
                MainActivity.startUpload()
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