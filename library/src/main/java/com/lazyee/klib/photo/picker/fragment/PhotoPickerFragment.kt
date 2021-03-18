package com.lazyee.klib.photo.picker.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.bean.PhotoDirectory
import com.lazyee.klib.photo.databinding.FragmentPhotoPickerBinding
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity
import com.lazyee.klib.photo.picker.adapter.PhotoGridAdapter
import com.lazyee.klib.photo.picker.adapter.PopupDirectoryListAdapter
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoClickListener
import com.lazyee.klib.photo.picker.common.MediaStoreHelper
import com.lazyee.klib.photo.picker.common.PhotoResultCallback
import java.util.*

internal class PhotoPickerFragment : Fragment() {

    var photoGridAdapter: PhotoGridAdapter? = null
        private set
    private var isSelectSingle = false //默认可以选择多张
    private var listAdapter: PopupDirectoryListAdapter? = null
    private var directories: MutableList<PhotoDirectory>? = null

    private lateinit var binding: FragmentPhotoPickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        directories = ArrayList()

        MediaStoreHelper.loadPhoto(activity!!,
            object :PhotoResultCallback{
                override fun onResultCallback(directories: List<PhotoDirectory>?) {
                    this@PhotoPickerFragment.directories?.clear()
                    this@PhotoPickerFragment.directories?.addAll(directories!!)
                    photoGridAdapter?.notifyDataSetChanged()
                    listAdapter?.notifyDataSetChanged()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        retainInstance = true
        binding = FragmentPhotoPickerBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGridAdapter = PhotoGridAdapter(activity!!, directories!!)
        photoGridAdapter!!.setSelectSingle(isSelectSingle)
        listAdapter = PopupDirectoryListAdapter(activity!!, directories!!)

        val layoutManager = StaggeredGridLayoutManager(4, OrientationHelper.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        binding.rvPhotos.layoutManager = layoutManager
        binding.rvPhotos.adapter = photoGridAdapter
        binding.rvPhotos.itemAnimator = DefaultItemAnimator()


        val listPopupWindow = ListPopupWindow(
            activity!!)
        listPopupWindow.width = ListPopupWindow.MATCH_PARENT
        listPopupWindow.anchorView = binding.tvDir
        listPopupWindow.setAdapter(listAdapter)
        listPopupWindow.isModal = true
        listPopupWindow.setDropDownGravity(Gravity.BOTTOM)
        listPopupWindow.animationStyle = R.style.Animation_AppCompat_DropDownUp
        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            listPopupWindow.dismiss()
            val (_, _, name) = directories!![position]
            binding.tvDir.text = name
            photoGridAdapter!!.setDirectoryIndex(position)
            photoGridAdapter!!.notifyDataSetChanged()
        }
        photoGridAdapter!!.setOnPhotoClickListener(object : OnPhotoClickListener {
            override fun onPhotoClick(v: View, position: Int) {
                val photos = photoGridAdapter!!.currentPhotoPaths
                val screenLocation = IntArray(2)
                v.getLocationOnScreen(screenLocation)
                val imagePagerFragment =
                    PhotoPagerFragment.newInstance(photos, position, screenLocation,
                        v.width, v.height)
                (activity as PhotoPickerActivity?)?.addImagePagerFragment(imagePagerFragment)
            }
        })

        binding.llAllImage.setOnClickListener {
            if (listPopupWindow.isShowing) {
                listPopupWindow.dismiss()
            } else if (!activity!!.isFinishing) {

                listPopupWindow.height = (binding.root.height* 0.8f).toInt()
                listPopupWindow.show()
            }
        }
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