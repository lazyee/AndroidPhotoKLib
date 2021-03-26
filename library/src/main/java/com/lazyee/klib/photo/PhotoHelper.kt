package com.lazyee.klib.photo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lazyee.klib.photo.corp.Crop
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity
import com.lazyee.klib.photo.picker.common.PhotoPickerIntent
import com.lazyee.klib.photo.util.BitmapUtils
import com.lazyee.klib.photo.util.BitmapUtils.isPictureRotate
import com.lazyee.klib.photo.util.LogUtils
import java.io.File
import java.io.IOException
import java.lang.Exception

/**
 * 照片帮助类
 * @property mActivity Activity
 * @constructor
 */

/**
 * 请求权限失败
 */
//typealias OnRequestPermissionDenied = (MutableList<String>?, Boolean) -> Unit

class PhotoHelper(private val mActivity: FragmentActivity) {

    private val TAG = "[PhotoHelper]"
    internal var isCrop = false
    private var isFreeCrop = false
    private var imagePath: String? = null
    private var outputImagePath: String? = null
    private var aspectX = 1
    private var aspectY = 1
    private var maxWidth = 500
    private var maxHeight = 500
    private var handlePhotoCallback: HandlePhotoCallback? = null

    private var onActivityResultFragment: OnActivityResultFragment? = null
    private var onRequestPermissionDeniedListener:OnRequestPermissionDeniedListener? = null

    fun setCrop(isCrop: Boolean): PhotoHelper {
        this.isCrop = isCrop
        return this
    }

    fun setFreeCrop(freeCrop: Boolean): PhotoHelper {
        isFreeCrop = freeCrop
        isCrop = freeCrop
        return this
    }

    fun setMaxSize(maxWidth: Int, maxHeight: Int): PhotoHelper {
        this.maxWidth = maxWidth
        this.maxHeight = maxHeight
        return this
    }

    fun setCropAspect(aspectX: Int, aspectY: Int): PhotoHelper {
        this.aspectX = aspectX
        this.aspectY = aspectY
        return this
    }

    /**
     * 打开摄像头
     */
    fun openCamera() {
        if(handlePhotoCallback == null){
            LogUtils.e(TAG,"handlePhotoCallback== null,请调用setHandlePhotoCallback(HandlePhotoCallback),否则无法打开摄像头")
            return
        }
        XXPermissions.with(mActivity)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    // 下面这句指定调用相机拍照后的照片存储的路径
                    val fileUri = obtainNewImageUri()
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                    intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, "portrait")
                    val resInfoList = mActivity.packageManager
                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        mActivity.grantUriPermission(packageName,
                            fileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    onActivityResultFragment = obtainOnActivityResultFragment(intent, CAMERA)
                    addFragmentToFragmentActivity(onActivityResultFragment!!)
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    onRequestPermissionDeniedListener?.onDenied(permissions, never)
                }
            })
    }

    private fun obtainOnActivityResultFragment(
        intent: Intent,
        requestCode: Int,
    ): OnActivityResultFragment {
        removeOnActivityResultFragment()
        val fragment = OnActivityResultFragment()
        fragment.intent = intent
        fragment.photoHelper = this
        fragment.requestCode = requestCode
        return fragment
    }

    private fun addFragmentToFragmentActivity(fragment: OnActivityResultFragment) {
        try {
            mActivity.supportFragmentManager.beginTransaction()
                    .add(fragment, OnActivityResultFragment.TAG)
                    .commitAllowingStateLoss()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun removeOnActivityResultFragment() {
        try {
            if (onActivityResultFragment != null ){
                if(onActivityResultFragment!!.isAdded) {
                    mActivity.supportFragmentManager.beginTransaction()
                        .remove(onActivityResultFragment!!).commitAllowingStateLoss()
                }
            }else{
                onActivityResultFragment = mActivity.supportFragmentManager.findFragmentByTag(OnActivityResultFragment.TAG) as OnActivityResultFragment?
                if(onActivityResultFragment != null){
                    mActivity.supportFragmentManager.beginTransaction()
                        .remove(onActivityResultFragment!!).commitAllowingStateLoss()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }finally {
            onActivityResultFragment = null
        }
    }


    fun setHandlePhotoCallback(handlePhotoCallback: HandlePhotoCallback?) {
        this.handlePhotoCallback = handlePhotoCallback
    }

    fun setOnRequestPermissionDeniedListener(listener:OnRequestPermissionDeniedListener){
        this.onRequestPermissionDeniedListener = listener
    }

    /**
     * 打开相册
     * @param photoCount Int
     * @param onPermissionDenied Function2<MutableList<String>?, Boolean, Unit>?
     */
    fun openAlbum(photoCount: Int) {
        if(handlePhotoCallback == null){
            LogUtils.e(TAG,"handlePhotoCallback== null,请调用setHandlePhotoCallback(HandlePhotoCallback),否则无法打开图库")
            return
        }
        XXPermissions.with(mActivity)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    val intent = PhotoPickerIntent(mActivity)
                    intent.setPhotoCount(photoCount)

                    onActivityResultFragment = obtainOnActivityResultFragment(intent, ALBUM)
                    PhotoPickerActivity.photoHelper = this@PhotoHelper
                    addFragmentToFragmentActivity(onActivityResultFragment!!)
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    onRequestPermissionDeniedListener?.onDenied(permissions, never)
                }
            })
    }

    /**
     * 处理图片
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    internal fun dealImage(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == CAMERA) {
            imagePath = getAbsolutePath(imagePath)
            imagePath = BitmapUtils.rotateImageAndSave(
                BitmapUtils.readPictureDegree(imagePath),
                getAbsolutePath(imagePath))
            if (isCrop) {
                beginCrop(fragment = onActivityResultFragment,path = imagePath)
            } else {
                if(TextUtils.isEmpty(imagePath)){
                    removeOnActivityResultFragment()
                    return
                }
                val absolutePath = getAbsolutePath(imagePath)
                if(TextUtils.isEmpty(absolutePath)){
                    removeOnActivityResultFragment()
                    return
                }
                removeOnActivityResultFragment()
                handlePhotoCallback?.onComplete(createPhotoList(absolutePath!!,
                    getImageSize(imagePath!!)))
            }
        } else if (requestCode == ALBUM) {
            getPickPhoto(data)
        } else if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            beginCrop(fragment = onActivityResultFragment,path = data!!.data!!.path)
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data)
        }
    }

    private fun getPickPhoto(data: Intent?) {
        data ?: return
        val photoPathList = data.getStringArrayListExtra(PhotoPickerActivity.KEY_SELECTED_PHOTOS)

        photoPathList ?: return
//        if (photoPathList.size == 1 && isCrop) {
//            beginCrop(photoPathList[0])
//        } else {
            if(handlePhotoCallback == null){
                removeOnActivityResultFragment()
                return
            }
            val photos = mutableListOf<Photo>()
            photoPathList.forEach {
                val size = getImageSize(it)
                photos.add(Photo(it, size[0], size[1]))
            }
            removeOnActivityResultFragment()
            handlePhotoCallback!!.onComplete(photos)

//        }
    }

    internal fun beginCrop(fragment:Fragment? = null,activity: Activity? = null,path: String?) {
        path ?: return
        val absolutePath = getAbsolutePath(path)
        absolutePath ?: return
        val file = File(absolutePath)
        var source: Uri? = null

        source = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(mActivity, FILE_PROVIDER, file)
        } else {
//            24版本以下的直接获取Uri即可
            Uri.fromFile(file)
        }
        val outputPath = File(mActivity.filesDir, "images/")
        if (!outputPath.exists()) {
            outputPath.mkdirs()
        }
        val output = File(mActivity.filesDir, "images/" + System.currentTimeMillis() + ".jpg")

        try {
            output.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        outputImagePath = output.absolutePath
        val destination: Uri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(mActivity, FILE_PROVIDER, output)
        } else {
            Uri.fromFile(output)
        }
        val crop: Crop = Crop.of(source, destination)
        crop.withMaxSize(maxWidth, maxHeight)
        if (!isFreeCrop) {
            crop.withAspect(aspectX, aspectY)
        }
        if(fragment != null){
            crop.start(fragment)
            return
        }

        if(activity != null){
            crop.start(activity)
        }

    }

    private fun handleCrop(resultCode: Int, result: Intent?) {
        removeOnActivityResultFragment()
        if (resultCode == Activity.RESULT_OK && result != null) {
            outputImagePath ?: return
            handlePhotoCallback?.onComplete(createPhotoList(outputImagePath!!,
                getImageSize(outputImagePath!!)))

        } else if (resultCode == Crop.RESULT_ERROR) {

            Toast.makeText(mActivity, Crop.getError(result).message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取图片的实体集合
     * @param path
     * @param size int array 0:w ,h:1
     * @return
     */
    private fun createPhotoList(path: String, size: IntArray): MutableList<Photo> {
        val photos = mutableListOf<Photo>()
        photos.add(Photo(path, size[0], size[1]))
        return photos
    }

    // 创建目录
    private fun obtainNewImageUri(): Uri {

        val path = "images/default.jpg"
        val dir = File(mActivity.filesDir.absoluteFile.toString() + File.separator + "images")
        if (!dir.exists()) { // 创建目录
            dir.mkdirs()
        }
        val file = File(mActivity.filesDir, path)
        outputImagePath = file.absolutePath
        val uri = FileProvider.getUriForFile(mActivity, FILE_PROVIDER, file)
        imagePath = path
        return uri
    }

    /**
     * 获取目标图片的宽高
     *
     * @return
     */
    private fun getImageSize(imagePath: String): IntArray {
        val absolutePath = getAbsolutePath(imagePath)
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(absolutePath, opts)
        LogUtils.e(TAG, "imagePath:$imagePath")
        val rotate = isPictureRotate(absolutePath)
        val width = if (rotate) opts.outHeight else opts.outWidth
        val height = if (rotate) opts.outWidth else opts.outHeight
        return intArrayOf(width, height)
    }

    /**
     * 获取真实路径
     * @param path
     * @return
     */
    private fun getAbsolutePath(path: String?): String? {
        path ?: return null
        var file = File(path)
        if (!file.exists()) {
            file = File(mActivity.filesDir, path)
            LogUtils.e(TAG, "file:${file.absoluteFile},${file.exists()}")

            if (file.exists()) {
                return file.absolutePath
            }
        }
        return path
    }

    companion object {
        private var FILE_PROVIDER = ""
        const val CAMERA = 10001
        const val ALBUM = 10002
        fun init(applicationId: String) {
            FILE_PROVIDER = "$applicationId.fileprovider"
        }


    }
}