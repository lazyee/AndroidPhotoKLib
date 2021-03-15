package com.lazyee.klib.photo.picker.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity

internal class PhotoPickerIntent : Intent {
    private constructor()
    private constructor(o: Intent) : super(o)
    private constructor(action: String) : super(action)
    private constructor(action: String, uri: Uri) : super(action, uri)
    private constructor(packageContext: Context, cls: Class<*>) : super(packageContext, cls)

    constructor(packageContext: Context?) : super(packageContext,
        PhotoPickerActivity::class.java) {
    }

    fun setPhotoCount(photoCount: Int) {
        this.putExtra(PhotoPickerActivity.EXTRA_MAX_COUNT, photoCount)
    }
}