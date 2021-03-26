package com.lazyee.klib.photo.extension

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.lazyee.klib.photo.R
import java.io.File

/**
 * @Author leeorz
 * @Date 3/26/21-6:08 PM
 * @Description:View的拓展方法
 */

internal fun ImageView.loadThumbnail(path:String){
    Glide.with(context)
        .load(Uri.fromFile(File(path)))
        .error(R.drawable.ic_broken_image)
        .placeholder(R.drawable.shape_photo_bg)
        .centerCrop()
        .thumbnail(0.3f)
        .into(this)
}
