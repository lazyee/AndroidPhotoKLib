package com.lazyee.klib.photo.picker.adapter

import androidx.recyclerview.widget.RecyclerView
import com.lazyee.klib.photo.bean.Photo
import com.lazyee.klib.photo.bean.PhotoDirectory
import java.util.*

abstract class SelectableAdapter<VH : RecyclerView.ViewHolder?>(var photoDirectories: MutableList<PhotoDirectory>) :
    RecyclerView.Adapter<VH>() {

    var selectedPhotos = mutableListOf<Photo>()
    private var currentDirectoryIndex: Int = 0

//    override val selectedPhotos: List<Photo?>? = mutableListOf<PhotoDirectory>()

    /**
     * Indicates if the item at position position is selected
     *
     * @param photo Photo of the item to check
     * @return true if the item is selected, false otherwise
     */
    fun isSelected(photo: Photo): Boolean {
        return selectedPhotos.contains(photo)
    }

    /**
     * 切换选择
     * @param photo Photo
     * @return Boolean true 为选中，false 为移除选中
     */
    fun toggleSelection(photo: Photo): Boolean {
        if (selectedPhotos.contains(photo)) {
            selectedPhotos.remove(photo)
            return false
        }

        selectedPhotos.add(photo)

        return true
    }

    /**
     * Clear the selection status for all items
     */
    fun clearSelection() {
        selectedPhotos.clear()
    }

    fun clearAllSelectionButOne(photo:Photo){
        clearSelection()
        selectedPhotos.add(photo)
    }

    /**
     * Count the selected items
     *
     * @return Selected items count
     */
    val selectedItemCount: Int
        get() = selectedPhotos.size

    fun setDirectoryIndex(index: Int) {
        if (index < photoDirectories.size) {
            this.currentDirectoryIndex = index
        }
    }

    val currentPhotos: List<Photo>
        get() = photoDirectories[currentDirectoryIndex].photos

    val currentPhotoPaths: List<String>
        get() {
            val currentPhotoPaths: MutableList<String> = ArrayList()
            for (photo in currentPhotos) {
                currentPhotoPaths.add(photo.path!!)
            }
            return currentPhotoPaths
        }


}