package com.lazyee.klib.photo.picker.common

import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.TextUtils
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.PhotoDirectory
import java.io.File
import java.util.*

/**
 * 读取图片帮助类
 */
internal object MediaStoreHelper {
    const val INDEX_ALL_PHOTOS = 0
    fun loadPhoto(activity: FragmentActivity, resultCallback: PhotoResultCallback?) {
        LoaderManager.getInstance(activity)
            .initLoader(0, null, PhotoLoaderCallback(activity, resultCallback))

    }
}

interface PhotoResultCallback {
    fun onResultCallback(directories: List<PhotoDirectory>?)
}

internal class PhotoLoaderCallback(
    var activity: FragmentActivity,
    var resultCallback: PhotoResultCallback?
) : LoaderManager.LoaderCallbacks<Cursor?> {
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        return PhotoDirectoryLoader(activity)
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        if (data == null) return
        data.moveToFirst()
        val directories: MutableList<PhotoDirectory> = ArrayList()
        val photoDirectoryAll = PhotoDirectory(name = activity.getString(R.string.all_image),id = "ALL")

        while (data.moveToNext()) {
            val imageId = data.getInt(data.getColumnIndexOrThrow(BaseColumns._ID))
            val bucketId =
                data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID))
            val name =
                data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME))
            val path = data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            if (!File(path).exists()) {
                continue
            }
            if (TextUtils.isEmpty(name)) {
                continue
            }
            val photoDirectory = PhotoDirectory(id = bucketId,name = name)

            if (!directories.contains(photoDirectory)) {
                photoDirectory.coverPath = path
                photoDirectory.addPhoto(imageId, path)
                photoDirectory.addedDate = data.getLong(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                directories.add(photoDirectory)
            } else {
                directories[directories.indexOf(photoDirectory)].addPhoto(imageId, path)
            }
            photoDirectoryAll.addPhoto(imageId, path)
        }
        if (photoDirectoryAll.photos.size > 0) {
            photoDirectoryAll.coverPath = photoDirectoryAll.photos[0].path
        }
        directories.add(MediaStoreHelper.INDEX_ALL_PHOTOS, photoDirectoryAll)
        if (resultCallback != null) {
            resultCallback!!.onResultCallback(directories)
        }

        LoaderManager.getInstance(activity).destroyLoader(0)
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {}
}