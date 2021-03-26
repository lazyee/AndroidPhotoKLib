package com.lazyee.klib.photo.picker.popup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.PhotoDirectory
import com.lazyee.klib.photo.databinding.PopupDirectoryBinding
import com.lazyee.klib.photo.databinding.TemplatePhotoPickerDirectoryBinding
import com.lazyee.klib.photo.extension.loadThumbnail


/**
 * @Author leeorz
 * @Date 3/25/21-5:44 PM
 * @Description:文件夹下拉弹窗
 */
@SuppressLint("ClickableViewAccessibility")
class DirectoryPopupWindow(private val mContext: Context) : PopupWindow() {

    private var directoryList: List<PhotoDirectory>? = null
    private var isAnimPlaying = false
    var onSelectPhotoDirectoryBlock: ((directory: PhotoDirectory) -> Unit)? = null
    private val binding by lazy {
        PopupDirectoryBinding.bind(
            LayoutInflater.from(mContext).inflate(
                R.layout.popup_directory,
                null
            )
        )
    }
    private val animIn by lazy {
        AnimationUtils.loadAnimation(mContext, R.anim.anim_popup_directory_list_in).also {
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    isAnimPlaying = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    isAnimPlaying = false
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }

            })
        }
    }
    private val animOut by lazy {
        AnimationUtils.loadAnimation(mContext, R.anim.anim_popup_directory_list_out).also {
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    isAnimPlaying = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    dismissThis()
                    isAnimPlaying = false
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }
    }

    init {
        contentView = binding.root
        width = LinearLayout.LayoutParams.MATCH_PARENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        setBackgroundDrawable(ColorDrawable())
        isOutsideTouchable = true
        isFocusable = true
        animationStyle = 0

        binding.root.setOnTouchListener { v, e ->
            dismiss()
            true
        }
    }

    fun setData(data: List<PhotoDirectory>): DirectoryPopupWindow {
        directoryList = data
        binding.rvDirectory.adapter = DirectoryAdapter(this, mContext, data)
        return this
    }


    fun setOnSelectPhotoDirectory(block: (directory: PhotoDirectory) -> Unit): DirectoryPopupWindow {
        onSelectPhotoDirectoryBlock = block
        return this
    }

    override fun showAsDropDown(anchor: View?) {
        if (isAnimPlaying) return
        super.showAsDropDown(anchor)
        binding.rvDirectory.startAnimation(animIn)
    }

    override fun dismiss() {
//        super.dismiss()
        if (isAnimPlaying) return
        binding.rvDirectory.startAnimation(animOut)
    }

    private fun dismissThis() {
        super.dismiss()
    }


    inner class DirectoryAdapter(
        private val popupWindow: DirectoryPopupWindow,
        private val context: Context,
        private val directoryList: List<PhotoDirectory>
    ) : RecyclerView.Adapter<DirectoryViewHolder>() {

        private var selectedIndex = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
            return DirectoryViewHolder(
                TemplatePhotoPickerDirectoryBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
            val directory = directoryList[position]
            holder.binding.tvDirName.text = "${directory.name}"
            holder.binding.tvPhotoCount.text = "(${directory.photos.size})"
            holder.binding.root.setOnClickListener {
                onSelectPhotoDirectoryBlock?.invoke(directory)
                selectedIndex = position
                notifyDataSetChanged()
                popupWindow.dismiss()

            }

            holder.binding.ivDirCover.loadThumbnail(directory.coverPath!!)

            holder.binding.line.visibility =
                if (position == itemCount - 1) View.GONE else View.VISIBLE

            holder.binding.ivSelected.visibility =
                if (selectedIndex == position) View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int {
            return directoryList.size
        }

    }

    inner class DirectoryViewHolder(val binding: TemplatePhotoPickerDirectoryBinding) :
        RecyclerView.ViewHolder(binding.root)
}