package com.mpdl.mvvm.common

sealed class Errors : Throwable() {
    data class NetworkError(val code: Int = -1, val desc: String = "") : Errors()
}