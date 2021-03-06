package com.mpdl.labcam.mvvm.repository.api

import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean
import com.mpdl.labcam.mvvm.repository.bean.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface LabCamApi {
    @FormUrlEncoded
    @POST("api2/auth-token/")
    suspend fun login(@Field("username") username: String,
                      @Field("password") password: String):Response<LoginResponse>


    @Headers("Cache-Control: public, max-age=" + 3)
    @GET("api2/repos/")
    suspend fun getRepos():Response<List<KeeperDirItem>>

    @Headers("Cache-Control: public, max-age=" + 3)
    @GET("api2/repos/{repoId}/dir/")
    suspend fun getDir(
        @Path("repoId") repoId:String,
        @Query("p") path:String,
        @Query("t") t:String):Response<List<KeeperDirItem>>



    @GET("/api2/repos/{repoId}/upload-link/")
    suspend fun getUploadLink(@Path("repoId") repoId:String,
                              @Query("p") path:String):Response<String>

    @Multipart
    @POST
    fun uploadFile(@Url url: String,
                   @Part("parent_dir") parent_dir: RequestBody,
                   @Part("replace") replace: RequestBody,
                   @Part file: MultipartBody.Part): Call<String>

}