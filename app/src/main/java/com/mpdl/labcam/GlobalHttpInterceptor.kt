package com.mpdl.labcam

import android.text.TextUtils
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.mvvm.globalsetting.IGlobalHttpInterceptor
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class GlobalHttpInterceptor : IGlobalHttpInterceptor {

    override fun onHttpRequestBefore(chain: Interceptor.Chain, request: Request): Request {
        /*
        * 这里可以在请求服务器之前拿到 {@link Request},做一些操作
        * 比如给 {@link Request} 统一添加 token 或者 header 以及参数加密等操作
        */
        if(!TextUtils.isEmpty(MainActivity.getToken())){
            return chain.request().newBuilder()
                .header("Authorization","Token ${MainActivity.getToken()}")
                .build()
        }
        return request
    }

    override fun onHttpResultResponse(chain: Interceptor.Chain, response: Response): Response {
        /*
        * 这里可以先客户端一步拿到每一次 Http 请求的结果, 可以先解析成 Json, 再做一些操作,
        * 如检测到 token 过期后重新请求 token, 并重新执行请求
        */
        return response
    }
}