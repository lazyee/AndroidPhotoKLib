package com.lazyee.klib.photo.extension

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
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

/**
 * 移动指定position到中间
 * @receiver RecyclerView
 * @param position Int
 * @param duration Int 滚动的毫秒数
 */
fun RecyclerView.scrollPositionToCenter(position:Int, duration:Int){
    try {
        val layoutManager = layoutManager as LinearLayoutManager
        val scroller =  CenterSmoothScroller(context,duration)
        scroller.targetPosition = if(position < 0) 0 else position
        if(scroller.isRunning)return
        layoutManager.startSmoothScroll(scroller)
    }catch (e:Exception){
        e.printStackTrace()
        scrollToPosition(position)
    }
}

private class CenterSmoothScroller(context: Context, private val duration:Int) : LinearSmoothScroller(context) {

    override fun calculateTimeForDeceleration(dx: Int): Int {
        return duration
    }

    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
    }

    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        return 50f / displayMetrics.densityDpi
    }
}
