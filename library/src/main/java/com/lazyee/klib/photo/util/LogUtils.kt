package com.lazyee.klib.photo.util

import android.text.TextUtils
import android.util.Log
import com.lazyee.klib.photo.BuildConfig

/**
 * @Author leeorz
 * @Date 2020/11/2-6:54 PM
 * @Description:
 */
internal object LogUtils {

    fun d(tag: String?, any: Any?) {
        if (!BuildConfig.DEBUG) return
        Log.d(getTag(tag), getMsg(any))
    }

    fun e(tag: String?, any: Any?) {
        Log.e(getTag(tag), getMsg(any))
    }

    fun i(tag: String?, any: Any?) {
        if (!BuildConfig.DEBUG) return
        Log.i(getTag(tag), getMsg(any))
    }

    fun w(tag: String?,any: Any?){
        if (!BuildConfig.DEBUG)return
        Log.w(getTag(tag), getMsg(any))
    }

    fun v(tag: String?,any: Any?){
        if (!BuildConfig.DEBUG)return
        Log.v(getTag(tag), getMsg(any))
    }

    private fun getTag(tag: String?):String{
        if (TextUtils.isEmpty(tag))return "[TAG]"
        return tag!!
    }

    private fun getMsg(any:Any?):String{
        if (any == null)return "[null]"
        if (any is String) return any
        return any.toString()
    }
}
