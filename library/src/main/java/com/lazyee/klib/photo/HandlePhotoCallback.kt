package com.lazyee.klib.photo

import com.lazyee.klib.photo.bean.Photo

/**
 * @Author leeorz
 * @Date 3/11/21-3:53 PM
 * @Description:
 */
interface HandlePhotoCallback {
    fun onComplete(photos:MutableList<Photo>)
}