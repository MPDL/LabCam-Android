package com.mpdl.labcam

import android.text.TextUtils
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

class CacheInterceptor:Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        //get request body
        var request = chain.request()
        //read cacheControl from @Headers
        var cacheControl = request.cacheControl.toString()
        if (TextUtils.isEmpty(cacheControl)){
            //add cacheControl if not exist
            return chain.proceed(request)
        }else{
            if (MainActivity.isNetworkConnected()){
                cacheControl = "public, max-age=" + 0
                return chain.proceed(request).newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl)
                    .build()

            }else{
                request = request.newBuilder()
                    //force cache
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build()
                return chain.proceed(request).newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl)
                    .build()
            }
        }
    }
}