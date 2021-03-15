package com.lazyee.klib.photo.picker.adapter

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.picker.widget.TouchImageView
import java.io.File

/**
 * 大图的viewpager适配器
 * @property mContext Context
 * @property paths List<String>
 * @property mLayoutInflater LayoutInflater
 * @constructor
 */
class PhotoPagerAdapter(private val mContext: Context, val paths: List<String>) : PagerAdapter() {
    private val mLayoutInflater: LayoutInflater by lazy {LayoutInflater.from(mContext)}
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView =
            mLayoutInflater.inflate(R.layout.template_photo_picker_pager, container, false)
        val path = paths[position]
        val uri: Uri = if (path.startsWith("http")) Uri.parse(path) else Uri.fromFile(File(path))

        val imageView = itemView.findViewById<TouchImageView>(R.id.iv_pager)
        Glide.with(mContext)
            .load(uri)
            .fitCenter()
            .placeholder(R.drawable.shape_photo_bg)
            .error(R.drawable.ic_broken_image)
            .into(imageView)

        imageView.setOnClickListener {
            try {
                if (!(mContext as Activity).isFinishing) {
                    mContext.onBackPressed()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        container.addView(itemView)
        return itemView
    }

    override fun getCount(): Int {
        return paths.size
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