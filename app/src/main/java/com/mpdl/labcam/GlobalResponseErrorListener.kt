package com.mpdl.labcam

import android.content.Context
import android.net.ParseException
import android.widget.Toast
import com.mpdl.mvvm.globalsetting.IResponseErrorListener
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class GlobalResponseErrorListener(context: Context): IResponseErrorListener{

    var mContext = context

    override fun handleResponseError(t: Throwable) {
        Timber.tag("Catch-Error").w(t.message)
        var msg = "Unknown Error"
        if (t is UnknownHostException) {
            msg = "Unknown Host"
        } else if (t is SocketTimeoutException) {
            msg = "Time out"
        } else if (t is HttpException) {
            msg = convertStatusCode(t)
        } else if (t is JsonParseException || t is ParseException
            || t is JSONException || t is JsonIOException) {
            msg = "Json Parse/IO Exception"
        }
        Toast.makeText(mContext,msg,Toast.LENGTH_SHORT).show()
    }


    private fun convertStatusCode(httpException: HttpException): String {
        return when {
            httpException.code() == 500 -> "Internal Server Error"
            httpException.code() == 404 -> "Not Found"
            httpException.code() == 403 -> "Forbidden"
            httpException.code() == 307 -> "Temporary Redirect"
            else -> httpException.message()
        }
    }
}