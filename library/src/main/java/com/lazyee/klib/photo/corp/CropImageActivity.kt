/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lazyee.klib.photo.corp

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.opengl.GLES10
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import com.gyf.immersionbar.ImmersionBar

import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.databinding.ActivityCropBinding
import com.lazyee.klib.photo.util.LogUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch

/*
 * Modified from original in AOSP.
 */
internal class CropImageActivity : MonitoredActivity() {
    private val handler = Handler()
    private var aspectX = 0
    private var aspectY = 0

    // Output image
    private var maxX = 0
    private var maxY = 0
    private var exifRotation = 0
    private var sourceUri: Uri? = null
    private var saveUri: Uri? = null
    var isSaving = false
    private var sampleSize = 0
    private var rotateBitmap: RotateBitmap? = null
    private var cropView: HighlightView? = null
    private val binding by lazy { ActivityCropBinding.inflate(layoutInflater) }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        //        setupWindowFlags();
        setupViews()
        loadInput()
        if (rotateBitmap == null) {
            finish()
            return
        }
        startCrop()
    }

    private fun setupViews() {
        setContentView(R.layout.activity_crop)
        ImmersionBar.with(this).titleBar(R.id.llTitle).barColor(R.color.header_background).init()
        binding.cropImageView.setRecycler(object : ImageViewTouchBase.Recycler {
            override fun recycle(b: Bitmap?) {
                b?.recycle()
                System.gc()
            }
        })

        binding.pickerHeader.tvTitle.setText(R.string.crop_crop)

        binding.pickerHeader.ivClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.pickerHeader.tvDone.isEnabled = true
        binding.pickerHeader.tvDone.setOnClickListener { onSaveClicked() }
    }

    private fun loadInput() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            aspectX = extras.getInt(Crop.Extra.ASPECT_X)
            aspectY = extras.getInt(Crop.Extra.ASPECT_Y)
            maxX = extras.getInt(Crop.Extra.MAX_X)
            maxY = extras.getInt(Crop.Extra.MAX_Y)
            saveUri = extras.getParcelable(MediaStore.EXTRA_OUTPUT)
        }
        sourceUri = intent.data
        if (sourceUri != null) {
            exifRotation = CropHelper.getExifRotation(CropHelper.getFromMediaUri(this,
                contentResolver,
                sourceUri))
            var `is`: InputStream? = null
            try {
                sampleSize = calculateBitmapSampleSize(sourceUri!!)
                `is` = contentResolver.openInputStream(sourceUri!!)
                val option = BitmapFactory.Options()
                option.inSampleSize = sampleSize
                rotateBitmap =
                    RotateBitmap(BitmapFactory.decodeStream(`is`, null, option), exifRotation)
            } catch (e: IOException) {
                LogUtils.e("Error reading image: " + e.message, e)
                setResultException(e)
            } catch (e: OutOfMemoryError) {
                LogUtils.e("OOM reading image: " + e.message, e)
                setResultException(e)
            } finally {
                CropHelper.closeSilently(`is`)
            }
        }
    }

    @Throws(IOException::class)
    private fun calculateBitmapSampleSize(bitmapUri: Uri): Int {
        var `is`: InputStream? = null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            `is` = contentResolver.openInputStream(bitmapUri)
            BitmapFactory.decodeStream(`is`, null, options) // Just get image size
        } finally {
            CropHelper.closeSilently(`is`)
        }
        val maxSize = maxImageSize
        var sampleSize = 1
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize shl 1
        }
        return sampleSize
    }

    private val maxImageSize: Int
        private get() {
            val textureLimit = maxTextureSize
            return if (textureLimit == 0) {
                SIZE_DEFAULT
            } else {
                Math.min(textureLimit, SIZE_LIMIT)
            }
        }

    // The OpenGL texture size is the maximum size that can be drawn in an ImageView
    private val maxTextureSize: Int
        private get() {
            // The OpenGL texture size is the maximum size that can be drawn in an ImageView
            val maxSize = IntArray(1)
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0)
            return maxSize[0]
        }

    private fun startCrop() {
        if (isFinishing) {
            return
        }
        rotateBitmap?:return
        binding.cropImageView.setImageRotateBitmapResetBase(rotateBitmap!!, true)
        CropHelper.startBackgroundJob(this, null, resources.getString(R.string.crop_wait),
            Runnable {
                val latch = CountDownLatch(1)
                handler.post {
                    if (binding.cropImageView.getScale() == 1f) {
                        binding.cropImageView.center()
                    }
                    latch.countDown()
                }
                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
                Cropper().crop()
            }, handler
        )
    }

    private inner class Cropper {
        private fun makeDefault() {
            rotateBitmap?:return
            val hv = HighlightView(binding.cropImageView)
            val width: Int = rotateBitmap!!.width
            val height: Int = rotateBitmap!!.height
            val imageRect = Rect(0, 0, width, height)

            // Make the default size about 4/5 of the width or height
            var cropWidth = Math.min(width, height) * 4 / 5
            var cropHeight = cropWidth
            if (aspectX != 0 && aspectY != 0) {
                if (aspectX > aspectY) {
                    cropHeight = cropWidth * aspectY / aspectX
                } else {
                    cropWidth = cropHeight * aspectX / aspectY
                }
            }
            val x = (width - cropWidth) / 2
            val y = (height - cropHeight) / 2
            val cropRect = RectF(x.toFloat(),
                y.toFloat(),
                (x + cropWidth).toFloat(),
                (y + cropHeight).toFloat())
            hv.setup(binding.cropImageView.unrotatedMatrix,
                imageRect,
                cropRect,
                aspectX != 0 && aspectY != 0)
            binding.cropImageView.add(hv)
        }

        fun crop() {
            handler.post {
                makeDefault()
                binding.cropImageView.invalidate()
                if (binding.cropImageView.highlightViews.size == 1) {
                    cropView = binding.cropImageView.highlightViews[0]
                    cropView?.setFocus(true)
                }
            }
        }
    }

    private fun onSaveClicked() {
        if (cropView == null || isSaving) {
            return
        }
        isSaving = true
        val croppedImage: Bitmap?
        val r: Rect = cropView!!.getScaledCropRect(sampleSize.toFloat())
        val width = r.width()
        val height = r.height()
        var outWidth = width
        var outHeight = height
        if (maxX > 0 && maxY > 0 && (width > maxX || height > maxY)) {
            val ratio = width.toFloat() / height.toFloat()
            if (maxX.toFloat() / maxY.toFloat() > ratio) {
                outHeight = maxY
                outWidth = (maxY.toFloat() * ratio + .5f).toInt()
            } else {
                outWidth = maxX
                outHeight = (maxX.toFloat() / ratio + .5f).toInt()
            }
        }
        croppedImage = try {
            decodeRegionCrop(r, outWidth, outHeight)
        } catch (e: IllegalArgumentException) {
            setResultException(e)
            finish()
            return
        }
        if (croppedImage != null) {
            binding.cropImageView.setImageRotateBitmapResetBase(RotateBitmap(croppedImage, exifRotation), true)
            binding.cropImageView.center()
            binding.cropImageView.highlightViews.clear()
        }
        saveImage(croppedImage)
    }

    private fun saveImage(croppedImage: Bitmap?) {
        if (croppedImage != null) {
            val b: Bitmap = croppedImage
            CropHelper.startBackgroundJob(this, null, resources.getString(R.string.crop_saving),
                Runnable { saveOutput(b) }, handler
            )
        } else {
            finish()
        }
    }

    private fun decodeRegionCrop(rect: Rect, outWidth: Int, outHeight: Int): Bitmap? {
        // Release memory now
        var rect = rect
        clearImageView()
        var `is`: InputStream? = null
        var croppedImage: Bitmap? = null
        try {
            `is` = contentResolver.openInputStream(sourceUri!!)
            val decoder = BitmapRegionDecoder.newInstance(`is`, false)
            val width = decoder.width
            val height = decoder.height
            if (exifRotation != 0) {
                // Adjust crop area to account for image rotation
                val matrix = Matrix()
                matrix.setRotate(-exifRotation.toFloat())
                val adjusted = RectF()
                matrix.mapRect(adjusted, RectF(rect))

                // Adjust to account for origin at 0,0
                adjusted.offset(if (adjusted.left < 0) width.toFloat() else 0.toFloat(),
                    if (adjusted.top < 0) height.toFloat() else 0.toFloat())
                rect = Rect(adjusted.left.toInt(),
                    adjusted.top.toInt(),
                    adjusted.right.toInt(),
                    adjusted.bottom.toInt())
            }
            try {
                croppedImage = decoder.decodeRegion(rect, BitmapFactory.Options())
                if (rect.width() > outWidth || rect.height() > outHeight) {
                    val matrix = Matrix()
                    matrix.postScale(outWidth.toFloat() / rect.width(),
                        outHeight.toFloat() / rect.height())
                    croppedImage = Bitmap.createBitmap(croppedImage,
                        0,
                        0,
                        croppedImage.width,
                        croppedImage.height,
                        matrix,
                        true)
                }
            } catch (e: IllegalArgumentException) {
                // Rethrow with some extra information
                throw IllegalArgumentException("Rectangle " + rect + " is outside of the image ("
                        + width + "," + height + "," + exifRotation + ")", e)
            }
        } catch (e: IOException) {
            LogUtils.e("Error cropping image: " + e.message, e)
            setResultException(e)
        } catch (e: OutOfMemoryError) {
            LogUtils.e("OOM cropping image: " + e.message, e)
            setResultException(e)
        } finally {
            CropHelper.closeSilently(`is`)
        }
        return croppedImage
    }

    private fun clearImageView() {
        binding.cropImageView.clear()
        rotateBitmap?.recycle()
        System.gc()
    }

    private fun saveOutput(croppedImage: Bitmap) {
        if (saveUri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = contentResolver.openOutputStream(saveUri!!)
                if (outputStream != null) {
                    croppedImage.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                }
            } catch (e: IOException) {
                setResultException(e)
                LogUtils.e("Cannot open file: $saveUri", e)
            } finally {
                CropHelper.closeSilently(outputStream)
            }
            CropHelper.copyExifRotation(
                CropHelper.getFromMediaUri(this, contentResolver, sourceUri),
                CropHelper.getFromMediaUri(this, contentResolver, saveUri)
            )
            setResultUri(saveUri!!)
        }
        handler.post {
            binding.cropImageView.clear()
            croppedImage.recycle()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        rotateBitmap?.recycle()
    }

    override fun onSearchRequested(): Boolean {
        return false
    }

    private fun setResultUri(uri: Uri) {
        setResult(RESULT_OK, Intent().putExtra(MediaStore.EXTRA_OUTPUT, uri))
    }

    private fun setResultException(throwable: Throwable) {
        setResult(Crop.RESULT_ERROR, Intent().putExtra(Crop.Extra.ERROR, throwable))
    }

    companion object {
        private const val SIZE_DEFAULT = 2048
        private const val SIZE_LIMIT = 4096
    }
}