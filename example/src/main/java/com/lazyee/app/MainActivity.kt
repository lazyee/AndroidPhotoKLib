package com.lazyee.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lazyee.app.databinding.ActivityMainBinding
import com.lazyee.klib.photo.HandlePhotoCallback
import com.lazyee.klib.photo.OnRequestPermissionDeniedListener
import com.lazyee.klib.photo.PhotoHelper
import com.lazyee.klib.photo.bean.Photo

/**
 * @Author leeorz
 * @Date 3/19/21-9:27 AM
 * @Description:
 */
class MainActivity : AppCompatActivity(),OnRequestPermissionDeniedListener,HandlePhotoCallback {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        PhotoHelper.init(BuildConfig.APPLICATION_ID)

        binding.btnOpenCamera.setOnClickListener {
            val photoHelper = PhotoHelper(this)
            photoHelper.setFreeCrop(binding.cbCrop.isChecked)
            photoHelper.setOnRequestPermissionDeniedListener(this)
            photoHelper.setHandlePhotoCallback(this)
            photoHelper.openCamera()
        }

        binding.btnOpenAlbum.setOnClickListener {
            val photoHelper = PhotoHelper(this)
            photoHelper.setFreeCrop(binding.cbCrop.isChecked)
            photoHelper.setOnRequestPermissionDeniedListener(this)
            photoHelper.setHandlePhotoCallback(this)
            photoHelper.openAlbum(1)
        }

        binding.btnOpenAlbumAndGetMultiPhoto.setOnClickListener {
            val photoHelper = PhotoHelper(this)
            photoHelper.setFreeCrop(binding.cbCrop.isChecked)
            photoHelper.setOnRequestPermissionDeniedListener(this)
            photoHelper.setHandlePhotoCallback(this)
            photoHelper.openAlbum(9)
        }

    }

    override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
        Toast.makeText(this,"no permission",Toast.LENGTH_SHORT).show()
    }

    override fun onComplete(photos: MutableList<Photo>) {

        var imagePathListStr = ""
        binding.llImages.removeAllViews()

        photos.forEach {
            imagePathListStr += "${it.path}\n\r"

            val width = resources.displayMetrics.widthPixels - 20
            val imageView = ImageView(this)
            val lp = LinearLayout.LayoutParams(width,width)
            lp.setMargins(10,10,10,10)
            imageView.layoutParams = lp
            Glide.with(this).load(it.path).into(imageView)
            binding.llImages.addView(imageView)
        }

        binding.tvResult.text = imagePathListStr




    }
}