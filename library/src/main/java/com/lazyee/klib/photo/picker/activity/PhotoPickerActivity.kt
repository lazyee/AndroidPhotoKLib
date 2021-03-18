package com.lazyee.klib.photo.picker.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.lazyee.klib.photo.PhotoHelper
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.bean.Photo
import com.lazyee.klib.photo.databinding.ActivityPhotoPickerBinding
import com.lazyee.klib.photo.picker.adapter.listener.OnPhotoSelectListener
import com.lazyee.klib.photo.picker.fragment.PhotoPagerFragment
import com.lazyee.klib.photo.picker.fragment.PhotoPagerFragment.AddImagePagerFragment
import com.lazyee.klib.photo.picker.fragment.PhotoPickerFragment
import java.util.*

internal class PhotoPickerActivity : AppCompatActivity(), AddImagePagerFragment {
    private var pickerFragment: PhotoPickerFragment? = null
    private var imagePagerFragment: PhotoPagerFragment? = null
    private var maxCount = DEFAULT_MAX_COUNT
    private val binding by lazy { ActivityPhotoPickerBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).barColor(R.color.header_background).init()
        setContentView(binding.root)

        maxCount = intent.getIntExtra(EXTRA_MAX_COUNT, DEFAULT_MAX_COUNT)
        maxCount = if (maxCount < MIN_COUNT) MIN_COUNT else maxCount


        setDoneText(0)//默认一进来的时候是0个
        binding.pickerHeader.tvDone.setOnClickListener { clickDone() }
        binding.pickerHeader.ivClose.setOnClickListener { finish() }

        pickerFragment =
            supportFragmentManager.findFragmentById(R.id.photoPickerFragment) as PhotoPickerFragment?
        pickerFragment!!.setSelectSingle(maxCount == MIN_COUNT)
        pickerFragment!!.photoGridAdapter?.setOnPhotoSelectListener(object : OnPhotoSelectListener {
            override fun onPhotoSelect(
                position: Int,
                photo: Photo,
                isSelect: Boolean,
                selectedItemCount: Int
            ): Boolean {
                val total = selectedItemCount + if (isSelect) -1 else 1
                binding.pickerHeader.tvDone.isEnabled = total > 0
                if (total > maxCount) {
                    Toast.makeText(this@PhotoPickerActivity,
                        getString(R.string.over_max_count_tips, maxCount.toString()),
                        Toast.LENGTH_LONG).show()
                    return false
                }
                setDoneText(total)
                return true
            }
        })
    }

    private fun setDoneText(total:Int){
        binding.pickerHeader.tvDone.text = getString(R.string.done_with_count,
            total.toString(),
            maxCount.toString())
    }

    /**
     * Overriding this method allows us to run our exit animation first, then exiting
     * the activity when it complete.
     */
    override fun onBackPressed() {
        if (imagePagerFragment != null && imagePagerFragment!!.isVisible) {
            imagePagerFragment!!.runExitAnimation {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    ImmersionBar.with(this).barColor(R.color.header_background).init()
                    setDoneEnable(pickerFragment!!.photoGridAdapter!!.selectedPhotos.isNotEmpty())
                    supportFragmentManager.popBackStack()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun addImagePagerFragment(imagePagerFragment: PhotoPagerFragment?) {
        this.imagePagerFragment = imagePagerFragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, this.imagePagerFragment!!)
            .addToBackStack(null)
            .commit()
    }

    private fun setDoneEnable(isEnable: Boolean) {
        binding.pickerHeader.tvDone.isEnabled = isEnable
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            super.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        photoHelper = null
    }

    /**
     * 点击done按钮
     */
    private fun clickDone() {
        val intent = Intent()
        val selectedPhotos: ArrayList<String> =
            pickerFragment!!.photoGridAdapter!!.selectedPhotoPaths


        if (selectedPhotos.isEmpty()) {
            selectedPhotos.add(imagePagerFragment!!.paths[imagePagerFragment!!.getCurrentItem()])
        }

        if(selectedPhotos.isNotEmpty() && selectedPhotos.size == 1){
            if(photoHelper != null && photoHelper!!.isCrop){
                photoHelper!!.beginCrop(activity = this,path = selectedPhotos[0])
                return
            }
        }

        //获取当前的预览图中的图像并且关闭页面，将数据带回上一个页面
        intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, selectedPhotos)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != RESULT_OK)return
        photoHelper?.dealImage(requestCode,resultCode,data)
        finish()


    }

    companion object {
        const val EXTRA_MAX_COUNT = "MAX_COUNT"
        const val KEY_SELECTED_PHOTOS = "SELECTED_PHOTOS"
        const val DEFAULT_MAX_COUNT = 9
        private const val MIN_COUNT = 1

        var photoHelper:PhotoHelper? = null
    }
}