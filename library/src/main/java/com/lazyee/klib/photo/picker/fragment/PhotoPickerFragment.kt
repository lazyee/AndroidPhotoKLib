package com.lazyee.klib.photo.picker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.lazyee.klib.photo.PhotoDirectory
import com.lazyee.klib.photo.databinding.FragmentPhotoPickerBinding
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity
import com.lazyee.klib.photo.picker.adapter.PhotoGridAdapter
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoClickListener

internal class PhotoPickerFragment : Fragment() {
    var photoGridAdapter: PhotoGridAdapter? = null
        private set
    private var isSelectSingle = false //默认可以选择多张
    private lateinit var binding: FragmentPhotoPickerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        retainInstance = true
        binding = FragmentPhotoPickerBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPhotos.itemAnimator = DefaultItemAnimator()
    }

    fun notifyDataSetChanged(){
        photoGridAdapter?.notifyDataSetChanged()
    }

    fun setData(directory: PhotoDirectory){
        photoGridAdapter = PhotoGridAdapter(activity!!, directory.photos)
        photoGridAdapter!!.setSelectSingle(isSelectSingle)
        val layoutManager = StaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        binding.rvPhotos.layoutManager = layoutManager
        binding.rvPhotos.adapter = photoGridAdapter

        photoGridAdapter!!.setOnPhotoClickListener(object : OnPhotoClickListener {
            override fun onPhotoClick(v: View, position: Int) {

                PhotoPreviewFragment.photoList = directory.photos
                PhotoPreviewFragment.isPreviewSelected = false

                val imagePagerFragment = PhotoPreviewFragment.newInstance( position)
                (activity as PhotoPickerActivity?)?.addImagePagerFragment(imagePagerFragment)
            }
        })
    }

    /**
     * 是否选择单张图片
     * @param isSelectSingle
     */
    fun setSelectSingle(isSelectSingle: Boolean) {
        this.isSelectSingle = isSelectSingle
        if (photoGridAdapter != null) {
            photoGridAdapter!!.setSelectSingle(this.isSelectSingle)
        }
    }
}