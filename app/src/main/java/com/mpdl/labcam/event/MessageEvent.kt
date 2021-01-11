package com.mpdl.labcam.event

class MessageEvent internal constructor(val type:String,val message: String) {
    internal fun getMessage(): String{
        return message
    }

    internal fun getType():String{
        return type
    }
}