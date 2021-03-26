package com.lazyee.klib.photo.picker.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.lazyee.klib.photo.Photo
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.picker.widget.TouchImageView
import java.io.File

/**
 * 大图的viewpager适配器
 */
class PhotoPagerAdapter(private val mContext: Context, val photos: List<Photo>) : PagerAdapter() {
    private val mLayoutInflater: LayoutInflater by lazy {LayoutInflater.from(mContext)}
    var onBigImageClickListener:OnBigImageClickListener? = null
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView =
            mLayoutInflater.inflate(R.layout.template_photo_picker_pager, container, false)
        val photo = photos[position]

        val imageView = itemView.findViewById<TouchImageView>(R.id.iv_pager)
        Glide.with(mContext)
            .load(Uri.fromFile(File(photo.path!!)))
            .fitCenter()
            .placeholder(R.drawable.shape_photo_bg)
            .error(R.drawable.ic_broken_image)
            .into(imageView)

        imageView.setOnClickListener {
            onBigImageClickListener?.onClickBigImage()
        }

        container.addView(itemView)
        return itemView
    }

    override fun getCount(): Int {
        return photos.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }
}

interface OnBigImageClickListener{
    fun onClickBigImage()
}