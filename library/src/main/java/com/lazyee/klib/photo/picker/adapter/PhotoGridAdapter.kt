package com.lazyee.klib.photo.picker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.Photo
import com.lazyee.klib.photo.databinding.TemplatePhotoPickerPhotoBinding
import com.lazyee.klib.photo.extension.loadThumbnail
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoClickListener
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoSelectListener

/**
 * 网格图片数据适配器
 */
class PhotoGridAdapter(val context: Context, val photos: List<Photo>) :
    RecyclerView.Adapter<PhotoViewHolder>() {

    private var onPhotoSelectListener: OnPhotoSelectListener? = null
    private var onPhotoClickListener: OnPhotoClickListener? = null

    private var isSelectSingle = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(TemplatePhotoPickerPhotoBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo: Photo = photos[position]
        holder.binding.ivPhoto.loadThumbnail(photo.path!!)
        var isSelected = PhotoPickerActivity.isSelected(photo)
        setSelected(holder, isSelected)
        holder.binding.ivPhoto.setOnClickListener { onPhotoClickListener?.onPhotoClick(it, position) }
        holder.binding.ivSelected.setOnClickListener {
            //单选情况下
            if(isSelectSingle ){
                if(PhotoPickerActivity.selectedPhotoList.size == 1 && PhotoPickerActivity.selectedPhotoList[0] != photo){
                    PhotoPickerActivity.clearAllSelected()
                    notifyDataSetChanged()
                }else if(PhotoPickerActivity.selectedPhotoList.size > 1 && isSelected){
                    PhotoPickerActivity.clearAllSelectionButOne(photo)
                    notifyDataSetChanged()
                }
            }

            val isEnable = onPhotoSelectListener?.onPhotoSelect(position, photo, isSelected,
                PhotoPickerActivity.selectedPhotoList.size) ?: true
            if (isEnable) {
                isSelected = PhotoPickerActivity.toggleSelection(photo)
                setSelected(holder, isSelected)
            }
        }
    }

    private fun setSelected(holder: PhotoViewHolder, isSelected: Boolean) {
        holder.binding.ivSelected.setImageResource(if (isSelected) R.drawable.ic_picker_checked else R.drawable.ic_picker_check)
        holder.binding.vSelectedMask.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    fun setOnPhotoSelectListener(listener: OnPhotoSelectListener?) {
        this.onPhotoSelectListener = listener
    }

    fun setOnPhotoClickListener(onPhotoClickListener: OnPhotoClickListener?) {
        this.onPhotoClickListener = onPhotoClickListener
    }

    /**
     * 是否是选择单个图片
     * @param selectSingle
     */
    fun setSelectSingle(selectSingle: Boolean) {
        isSelectSingle = selectSingle
    }
}

class PhotoViewHolder(val binding: TemplatePhotoPickerPhotoBinding) : RecyclerView.ViewHolder(binding.root)