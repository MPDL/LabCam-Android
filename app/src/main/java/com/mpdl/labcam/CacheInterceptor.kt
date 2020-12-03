package com.mpdl.labcam

import android.text.TextUtils
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

class CacheInterceptor:Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        //拿到请求体
        var request = chain.request()
        //读接口上的@Headers里的注解配置
        var cacheControl = request.cacheControl.toString()
        if (TextUtils.isEmpty(cacheControl)){
            //如果没有添加cache 注解
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
                    //强制使用缓存
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