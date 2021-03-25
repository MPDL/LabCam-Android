package com.mpdl.labcam

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.TextUtils
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.mvvm.globalsetting.IGlobalHttpInterceptor
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class GlobalHttpInterceptor : IGlobalHttpInterceptor {

    override fun onHttpRequestBefore(chain: Interceptor.Chain, request: Request): Request {
        if(!TextUtils.isEmpty(MainActivity.getToken())){
            return chain.request().newBuilder()
                .header("Authorization","Token ${MainActivity.getToken()}")
                .build()
        }
        return request
    }

    override fun onHttpResultResponse(chain: Interceptor.Chain, response: Response): Response {
        return response
    }


}