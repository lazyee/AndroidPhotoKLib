package com.lazyee.klib.photo

/**
 * @Author leeorz
 * @Date 3/12/21-5:47 PM
 * @Description:请求权限被拒绝
 */
interface OnRequestPermissionDeniedListener {
    fun onDenied(permissions: MutableList<String>?, never: Boolean)
}