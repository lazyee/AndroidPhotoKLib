package com.lazyee.klib.photo.picker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.bean.Photo
import com.lazyee.klib.photo.bean.PhotoDirectory
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoSelectListener
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoClickListener
import java.io.File
import java.util.*


/**
 * 网格图片数据适配器
 * @property context Context
 * @property inflater LayoutInflater
 * @property onPhotoSelectListener OnPhotoSelectListener?
 * @property onPhotoClickListener OnPhotoClickListener?
 * @property isSelectSingle Boolean
 * @property selectedPhotoPaths ArrayList<String>
 * @constructor
 */
class PhotoGridAdapter(val context: Context, photoDirectories: MutableList<PhotoDirectory>) :
    SelectableAdapter<PhotoViewHolder?>(photoDirectories) {

    private val inflater: LayoutInflater by lazy { LayoutInflater.from(context) }
    private var onPhotoSelectListener: OnPhotoSelectListener? = null
    private var onPhotoClickListener: OnPhotoClickListener? = null

    private var isSelectSingle = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val itemView: View = inflater.inflate(R.layout.template_photo_picker_photo, parent, false)
        return PhotoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photos: List<Photo> = currentPhotos
        val photo: Photo = photos[position]
        Glide.with(context)
            .load(File(photo.path))
            .centerCrop()
            .placeholder(R.drawable.shape_photo_bg)
            .error(R.drawable.ic_broken_image)
            .thumbnail(0.3f)
            .into(holder.ivPhoto)

        var isSelected = isSelected(photo)

        setSelected(holder, isSelected)
        holder.ivPhoto.setOnClickListener { onPhotoClickListener?.onPhotoClick(it, position) }
        holder.ivSelected.setOnClickListener {
            //单选情况下
            if(isSelectSingle ){
                if(selectedPhotos.size == 1 && selectedPhotos[0] != photo){
                    clearSelection()
                    notifyDataSetChanged()
                }else if(selectedPhotos.size > 1 && isSelected){
                    clearAllSelectionButOne(photo)
                    notifyDataSetChanged()
                }
            }

            val isEnable = onPhotoSelectListener?.onPhotoSelect(position, photo, isSelected,
                selectedPhotos.size) ?: true
            if (isEnable) {
                isSelected = toggleSelection(photo)
                setSelected(holder, isSelected)
            }
        }
    }

    private fun setSelected(holder: PhotoViewHolder, isSelected: Boolean) {
        holder.ivSelected.setImageResource(if (isSelected) R.drawable.ic_picker_checked else R.drawable.ic_picker_check)
        holder.mask.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return if (photoDirectories.size == 0) 0 else currentPhotos.size
    }

    fun setOnPhotoSelectListener(listener: OnPhotoSelectListener?) {
        this.onPhotoSelectListener = listener
    }

    fun setOnPhotoClickListener(onPhotoClickListener: OnPhotoClickListener?) {
        this.onPhotoClickListener = onPhotoClickListener
    }

    val selectedPhotoPaths: ArrayList<String>
        get() {
            val selectedPhotoPaths = ArrayList<String>(selectedItemCount)
            for (photo in selectedPhotos) {
                selectedPhotoPaths.add(photo.path!!)
            }
            return selectedPhotoPaths
        }

    /**
     * 是否是选择单个图片
     * @param selectSingle
     */
    fun setSelectSingle(selectSingle: Boolean) {
        isSelectSingle = selectSingle
    }
}

class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ivPhoto: ImageView by lazy { itemView.findViewById<View>(R.id.iv_photo) as ImageView }
    val mask: View by lazy { itemView.findViewById(R.id.vSelectedMask) }
    val ivSelected: ImageView by lazy { itemView.findViewById(R.id.ivSelected) }

}