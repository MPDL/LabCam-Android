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
    * OkHttpClient.Builder Config
    * */
    override fun configOkHttpClient(application: Application,
                                    builder: OkHttpClient.Builder): OkHttpClient.Builder {

        return builder
            .connectTimeout(3,TimeUnit.SECONDS)
            .writeTimeout(15,TimeUnit.SECONDS)
            .readTimeout(15,TimeUnit.SECONDS)
            .addInterceptor(CacheInterceptor())
            .addNetworkInterceptor(CacheInterceptor())
            .cache(Cache(File(application.filesDir,"labCamCache"),10 * 1024 * 1024))
    }

}