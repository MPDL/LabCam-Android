package com.mpdl.labcam.mvvm.repository

import com.mpdl.labcam.mvvm.repository.api.LabCamApi
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean
import com.mpdl.labcam.mvvm.repository.bean.LoginResponse
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.mvvm.base.BaseRepository
import com.mpdl.mvvm.base.BaseResult
import com.mpdl.mvvm.globalsetting.IRepositoryManager

class LoginRepository(repositoryManager: IRepositoryManager):
    BaseRepository(repositoryManager) {

    suspend fun login(username:String, password: String): BaseResult<LoginResponse> {
        return safeApiResponse(call = {
            MainActivity.getRetrofit()!!.create(LabCamApi::class.java)
                .login(username,password)
        })
    }

    suspend fun getRepos(type:String): BaseResult<List<KeeperDirectoryBean>>{
        return safeApiResponse(call = {
            MainActivity.getRetrofit()!!.create(LabCamApi::class.java)
                .getRepos(type)
        },retry = 2)
    }

    suspend fun getDir(repoId:String, path:String, t:String): BaseResult<List<KeeperDirectoryBean>>{
        return safeApiResponse(call = {
            MainActivity.getRetrofit()!!.create(LabCamApi::class.java)
                .getDir(repoId,path,t)
        },retry = 2)
    }

    suspend fun getUploadLink(repoId:String, path:String): BaseResult<String>{
        return safeApiResponse(call = {
            MainActivity.getRetrofit()!!.create(LabCamApi::class.java)
                .getUploadLink(repoId,path)
        },retry = 2)
    }

}