package com.lazyee.klib.photo.picker.adapter.listener

import android.view.View
import com.lazyee.klib.photo.bean.Photo

/**
 * 图片选择监听
 */
interface OnPhotoSelectListener {
    /***
     *
     * @param position 所选图片的位置
     * @param path     所选的图片
     * @param isSelect   是否选中
     * @param selectedItemCount  已选数量
     * @return enable check
     */
    fun onPhotoSelect(position: Int, path: Photo, isSelect: Boolean, selectedItemCount: Int): Boolean
}

/**
 * 图片点击监听
 */
interface OnPhotoClickListener {
    fun onPhotoClick(v: View, position: Int)
}