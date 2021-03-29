package com.lazyee.klib.photo.picker.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.lazyee.klib.photo.Photo
import com.lazyee.klib.photo.PhotoDirectory
import com.lazyee.klib.photo.PhotoHelper
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.databinding.ActivityPhotoPickerBinding
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoSelectListener
import com.lazyee.klib.photo.picker.common.MediaStoreHelper
import com.lazyee.klib.photo.picker.common.PhotoResultCallback
import com.lazyee.klib.photo.picker.fragment.PhotoPickerFragment
import com.lazyee.klib.photo.picker.fragment.PhotoPreviewFragment
import com.lazyee.klib.photo.picker.popup.DirectoryPopupWindow
import java.util.*

internal class PhotoPickerActivity : AppCompatActivity(),OnPhotoSelectListener {
    private var pickerFragment: PhotoPickerFragment? = null
    private var imagePagerFragment: PhotoPreviewFragment? = null
    private var maxCount = DEFAULT_MAX_COUNT
    private val binding by lazy { ActivityPhotoPickerBinding.inflate(layoutInflater) }
    private val directories = mutableListOf<PhotoDirectory>()
    private var directoryPopupWindow :DirectoryPopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).barColor(R.color.header_background).init()
        setContentView(binding.root)

        if(savedInstanceState != null){
            val fragment = supportFragmentManager.findFragmentByTag(PhotoPickerFragment::class.java.name)
            if(fragment != null){
                pickerFragment = fragment as PhotoPickerFragment
            }
        }

        addPhotoPickerFragment()
        maxCount = intent.getIntExtra(EXTRA_MAX_COUNT, DEFAULT_MAX_COUNT)
        maxCount = if (maxCount < MIN_COUNT) MIN_COUNT else maxCount


        setSelectedCountText(0)//默认一进来的时候是0个
        binding.tvDone.setOnClickListener { clickDone() }
        binding.tvPreview.setOnClickListener { clickPreview() }
        binding.pickerHeader.ivClose.setOnClickListener { finish() }

        pickerFragment!!.setSelectSingle(maxCount == MIN_COUNT)

        if(directories.isEmpty()){
            MediaStoreHelper.loadPhoto(this,
                object : PhotoResultCallback {
                    override fun onResultCallback(directories: List<PhotoDirectory>?) {
                        if(directories != null && directories.isNotEmpty()){
                            this@PhotoPickerActivity.directories.addAll(directories)
                            binding.tvDirectoryName.text = directories[0].name
                            pickerFragment?.setData(directories[0])
                            pickerFragment?.photoGridAdapter?.setOnPhotoSelectListener(this@PhotoPickerActivity)
                        }
                    }
                })
        }

        binding.llDirectoryOptions.setOnClickListener {
            if(directoryPopupWindow == null){
                directoryPopupWindow = DirectoryPopupWindow(this)
                directoryPopupWindow?.run{
                    setData(directories)
                    setOnSelectPhotoDirectory {
                        binding.tvDirectoryName.text = it.name
                        pickerFragment?.setData(it)
                        pickerFragment?.photoGridAdapter?.setOnPhotoSelectListener(this@PhotoPickerActivity)
                    }
                }
            }
            directoryPopupWindow?.showAsDropDown(binding.rlHeader)
        }
    }

    private fun addPhotoPickerFragment(){
        if(pickerFragment == null){
            pickerFragment = PhotoPickerFragment()
        }

        if(!pickerFragment!!.isAdded){
            supportFragmentManager.beginTransaction()
                .add(R.id.flContent,pickerFragment!!,PhotoPickerFragment::class.java.name)
                .commitAllowingStateLoss()
        }
    }

    private fun setSelectedCountText(total:Int){
        binding.tvDone.isEnabled = total > 0
        binding.tvPreview.isEnabled = total > 0

        binding.tvPreview.text = getString(R.string.Preview,
            total.toString())
        binding.tvDone.text = getString(R.string.done,total.toString(),maxCount.toString())
    }

    /**
     * Overriding this method allows us to run our exit animation first, then exiting
     * the activity when it complete.
     */
    override fun onBackPressed() {
        if (imagePagerFragment != null && imagePagerFragment!!.isVisible) {
//            supportFragmentManager.popBackStack()
            selectedPhotoList.removeAll(imagePagerFragment!!.getRemovePhotoList())
            notifyDataSetChanged()

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.anim_preview_fragment_in,R.anim.anim_preview_fragment_out)
                .remove(imagePagerFragment!!)
                .commit()
        } else {
            super.onBackPressed()
        }
    }

    fun addImagePagerFragment(imagePagerFragment: PhotoPreviewFragment) {
        this.imagePagerFragment = imagePagerFragment
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.anim_preview_fragment_in,R.anim.anim_preview_fragment_out)
            .replace(R.id.container, this.imagePagerFragment!!)
            .addToBackStack(null)
            .commit()
    }

    fun notifyDataSetChanged(){
        pickerFragment?.notifyDataSetChanged()
        setSelectedCountText(selectedPhotoList.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        photoHelper = null
        selectedPhotoList.clear()
        PhotoPreviewFragment.photoList = null
    }

    /**
     * 点击done按钮
     */
    internal fun clickDone() {
        val intent = Intent()

        val selectedPhotoPathList = arrayListOf<String>()
        selectedPhotoList.forEach { selectedPhotoPathList.add(it.path!!) }

        if(selectedPhotoPathList.isNotEmpty() && selectedPhotoPathList.size == 1){
            if(photoHelper != null && photoHelper!!.isCrop){
                photoHelper!!.beginCrop(activity = this,path = selectedPhotoPathList[0])
                return
            }
        }

        //获取当前的预览图中的图像并且关闭页面，将数据带回上一个页面
        intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, selectedPhotoPathList)
        setResult(RESULT_OK, intent)
        finish()
    }

    //点击预览
    private fun clickPreview(){
        PhotoPreviewFragment.maxCount = maxCount
        PhotoPreviewFragment.isPreviewSelected = true
        PhotoPreviewFragment.photoList = selectedPhotoList

        val imagePagerFragment = PhotoPreviewFragment.newInstance(0)
        addImagePagerFragment(imagePagerFragment)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != RESULT_OK)return
        photoHelper?.dealImage(requestCode,resultCode,data)
        finish()
    }

    override fun onPhotoSelect(
        position: Int,
        photo: Photo,
        isSelect: Boolean,
        selectedItemCount: Int
    ): Boolean {
        val total = selectedItemCount + if (isSelect) -1 else 1


        if (total > maxCount) {
            Toast.makeText(this@PhotoPickerActivity,
                getString(R.string.over_max_count_tips, maxCount.toString()),
                Toast.LENGTH_LONG).show()
            return false
        }
        setSelectedCountText(total)
        return true
    }

    companion object {
        const val EXTRA_MAX_COUNT = "MAX_COUNT"
        const val KEY_SELECTED_PHOTOS = "SELECTED_PHOTOS"
        const val DEFAULT_MAX_COUNT = 9
        private const val MIN_COUNT = 1

        var photoHelper:PhotoHelper? = null

        //选中的图片集合
        var selectedPhotoList = mutableListOf<Photo>()


        /**
         * Indicates if the item at position position is selected
         *
         * @param photo Photo of the item to check
         * @return true if the item is selected, false otherwise
         */
        fun isSelected(photo: Photo): Boolean {
            return selectedPhotoList.contains(photo)
        }

        /**
         * 切换选择
         * @param photo Photo
         * @return Boolean true 为选中，false 为移除选中
         */
        fun toggleSelection(photo: Photo): Boolean {
            if (selectedPhotoList.contains(photo)) {
                selectedPhotoList.remove(photo)
                return false
            }
            selectedPhotoList.add(photo)
            return true
        }

        /**
         * Clear the selection status for all items
         */
        fun clearAllSelected() {
            selectedPhotoList.clear()
        }

        fun clearAllSelectionButOne(photo: Photo){
            clearAllSelected()
            selectedPhotoList.add(photo)
        }

    }
}