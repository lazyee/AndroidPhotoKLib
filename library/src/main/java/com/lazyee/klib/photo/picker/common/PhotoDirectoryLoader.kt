package com.lazyee.klib.photo.picker.common

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.loader.content.CursorLoader

/**
 * Created by 黄东鲁 on 15/6/28.
 */
internal class PhotoDirectoryLoader : CursorLoader {
    val IMAGE_PROJECTION = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED
    )

    constructor(context: Context?) : super(context!!) {
        projection = IMAGE_PROJECTION
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"
        selection =
            MediaStore.MediaColumns.MIME_TYPE + "=? or " + MediaStore.MediaColumns.MIME_TYPE + "=? or " + MediaStore.MediaColumns.MIME_TYPE + "=?"
        selectionArgs = arrayOf("image/jpeg", "image/png", "image/gif")
    }

    private constructor(
        context: Context, uri: Uri, projection: Array<String>, selection: String,
        selectionArgs: Array<String>, sortOrder: String
    ) : super(context, uri, projection, selection, selectionArgs, sortOrder) {
    }
}