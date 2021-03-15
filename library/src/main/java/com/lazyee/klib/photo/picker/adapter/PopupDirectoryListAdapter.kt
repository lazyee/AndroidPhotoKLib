package com.lazyee.klib.photo.picker.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.bean.PhotoDirectory
import java.io.File
import java.util.*

/**
 * 文件夹弹窗列表的数据适配器
 * @property context Context
 * @property directories List<PhotoDirectory>
 * @property mLayoutInflater LayoutInflater
 * @constructor
 */
class PopupDirectoryListAdapter(private val context: Context,val directories: List<PhotoDirectory>) :
    BaseAdapter() {
    private val mLayoutInflater: LayoutInflater by lazy { LayoutInflater.from(context) }

    override fun getCount(): Int {
        return directories.size
    }

    override fun getItem(position: Int): PhotoDirectory {
        return directories[position]
    }

    override fun getItemId(position: Int): Long {
        return directories[position].hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView:View? = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView =
                mLayoutInflater.inflate(R.layout.template_photo_picker_directory, parent, false)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.bindData(directories[position])
        return convertView!!
    }

    private inner class ViewHolder(rootView: View) {
        var ivCover: ImageView = rootView.findViewById(R.id.iv_dir_cover)
        var tvName: TextView = rootView.findViewById(R.id.tv_dir_name)
        fun bindData(directory: PhotoDirectory) {
            val uri: Uri = if (directory.coverPath!!.startsWith("http")) {
                Uri.parse(directory.coverPath)
            } else {
                Uri.fromFile(File(directory.coverPath))
            }
            Glide.with(context)
                .load(uri)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .thumbnail(0.3f)
                .into(ivCover)
            tvName.text = directory.name
        }

    }
}