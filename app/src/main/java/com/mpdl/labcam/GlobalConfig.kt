package com.mpdl.labcam

import android.app.Application
import com.mpdl.mvvm.globalsetting.IGlobalConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class GlobalConfig: IGlobalConfig{

    override fun configBaseUrl(): String = BuildConfig.API_BASE_URL

    /*
    * 添加 OkHttpClient.Builder 的设置
    * */
    override fun configOkHttpClient(application: Application,
                                    builder: OkHttpClient.Builder): OkHttpClient.Builder {

        return builder
            .writeTimeout(3000,TimeUnit.MILLISECONDS)
            .connectTimeout(3000,TimeUnit.MILLISECONDS)
            .readTimeout(3000,TimeUnit.MILLISECONDS)
            .addInterceptor(CacheInterceptor())
            .addNetworkInterceptor(CacheInterceptor())
            .cache(Cache(File(application.filesDir,"labCamCache"),10 * 1024 * 1024))
    }

}