/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatImageView

/*
 * Modified from original in AOSP.
 */
internal abstract class ImageViewTouchBase : AppCompatImageView {
    // This is the base transformation which is used to show the image
    // initially.  The current computation for this shows the image in
    // it's entirety, letterboxing as needed.  One could choose to
    // show the image as cropped instead.
    //
    // This matrix is recomputed when we go from the thumbnail image to
    // the full size image.
    protected var baseMatrix = Matrix()

    // This is the supplementary transformation which reflects what
    // the user has done in terms of zooming and panning.
    //
    // This matrix remains the same when we go from the thumbnail image
    // to the full size image.
    protected var suppMatrix = Matrix()

    // This is the final matrix which is computed as the concatentation
    // of the base matrix and the supplementary matrix.
    private val displayMatrix = Matrix()

    // Temporary buffer used for getting the values out of a matrix.
    private val matrixValues = FloatArray(9)

    // The current bitmap being displayed.
    protected val bitmapDisplayed = RotateBitmap(null, 0)
    var thisWidth = -1
    var thisHeight = -1
    var maxZoom = 0f
    private var onLayoutRunnable: Runnable? = null

    // ImageViewTouchBase will pass a Bitmap to the Recycler if it has finished
    // its use of that Bitmap
    interface Recycler {
        fun recycle(b: Bitmap?)
    }

    private var recycler: Recycler? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init()
    }

    fun setRecycler(recycler: Recycler?) {
        this.recycler = recycler
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        thisWidth = right - left
        thisHeight = bottom - top
        val r = onLayoutRunnable
        if (r != null) {
            onLayoutRunnable = null
            r.run()
        }
        if (bitmapDisplayed.bitmap != null) {
            getProperBaseMatrix(bitmapDisplayed, baseMatrix, true)
            imageMatrix = imageViewMatrix
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking && !event.isCanceled) {
            if (getScale() > 1.0f) {
                // If we're zoomed in, pressing Back jumps out to show the
                // entire image, otherwise Back returns the user to the gallery
                zoomTo(1.0f)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        setImageBitmap(bitmap, 0)
    }

    private fun setImageBitmap(bitmap: Bitmap?, rotation: Int) {
        super.setImageBitmap(bitmap)
        val d = drawable
        d?.setDither(true)
        val old = bitmapDisplayed.bitmap
        bitmapDisplayed.bitmap = bitmap
        bitmapDisplayed.rotation = rotation
        if (old != null && old != bitmap && recycler != null) {
            recycler!!.recycle(old)
        }
    }

    fun clear() {
        setImageBitmapResetBase(null, true)
    }

    // This function changes bitmap, reset base matrix according to the size
    // of the bitmap, and optionally reset the supplementary matrix
    fun setImageBitmapResetBase(bitmap: Bitmap?, resetSupp: Boolean) {
        setImageRotateBitmapResetBase(RotateBitmap(bitmap, 0), resetSupp)
    }

    fun setImageRotateBitmapResetBase(rotateBitmap: RotateBitmap, resetSupp: Boolean) {
        val viewWidth = width
        if (viewWidth <= 0) {
            onLayoutRunnable = Runnable { setImageRotateBitmapResetBase(rotateBitmap, resetSupp) }
            return
        }
        if (rotateBitmap.bitmap != null) {
            getProperBaseMatrix(rotateBitmap, baseMatrix, true)
            setImageBitmap(rotateBitmap.bitmap, rotateBitmap.rotation)
        } else {
            baseMatrix.reset()
            setImageBitmap(null)
        }
        if (resetSupp) {
            suppMatrix.reset()
        }
        imageMatrix = imageViewMatrix
        maxZoom = calculateMaxZoom()
    }

    // Center as much as possible in one or both axis.  Centering is defined as follows:
    // * If the image is scaled down below the view's dimensions then center it.
    // * If the image is scaled larger than the view and is translated out of view then translate it back into view.
    fun center() {
        val bitmap = bitmapDisplayed.bitmap ?: return
        val m = imageViewMatrix
        val rect = RectF(0f, 0f, bitmap.width
            .toFloat(), bitmap.height.toFloat())
        m.mapRect(rect)
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        deltaY = centerVertical(rect, height, deltaY)
        deltaX = centerHorizontal(rect, width, deltaX)
        postTranslate(deltaX, deltaY)
        imageMatrix = imageViewMatrix
    }

    private fun centerVertical(rect: RectF, height: Float, deltaY: Float): Float {
        var deltaY = deltaY
        val viewHeight = getHeight()
        if (height < viewHeight) {
            deltaY = (viewHeight - height) / 2 - rect.top
        } else if (rect.top > 0) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            deltaY = getHeight() - rect.bottom
        }
        return deltaY
    }

    private fun centerHorizontal(rect: RectF, width: Float, deltaX: Float): Float {
        var deltaX = deltaX
        val viewWidth = getWidth()
        if (width < viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left
        } else if (rect.left > 0) {
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
        }
        return deltaX
    }

    private fun init() {
        scaleType = ScaleType.MATRIX
    }

    protected fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(matrixValues)
        return matrixValues[whichValue]
    }

    // Get the scale factor out of the matrix.
    fun getScale(matrix: Matrix = suppMatrix): Float {
        return getValue(matrix, Matrix.MSCALE_X)
    }

//    protected val scale: Float
//        protected get() = getScale(suppMatrix)

    // Setup the base matrix so that the image is centered and scaled properly.
    private fun getProperBaseMatrix(
        bitmap: RotateBitmap,
        matrix: Matrix,
        includeRotation: Boolean
    ) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        matrix.reset()

        // We limit up-scaling to 3x otherwise the result may look bad if it's a small icon
        val widthScale = Math.min(viewWidth / w, 3.0f)
        val heightScale = Math.min(viewHeight / h, 3.0f)
        val scale = Math.min(widthScale, heightScale)
        if (includeRotation) {
            matrix.postConcat(bitmap.rotateMatrix)
        }
        matrix.postScale(scale, scale)
        matrix.postTranslate((viewWidth - w * scale) / 2f, (viewHeight - h * scale) / 2f)
    }// The final matrix is computed as the concatentation of the base matrix

    // and the supplementary matrix
    // Combine the base matrix and the supp matrix to make the final matrix
    protected val imageViewMatrix: Matrix
        protected get() {
            // The final matrix is computed as the concatentation of the base matrix
            // and the supplementary matrix
            displayMatrix.set(baseMatrix)
            displayMatrix.postConcat(suppMatrix)
            return displayMatrix
        }
    val unrotatedMatrix: Matrix
        get() {
            val unrotated = Matrix()
            getProperBaseMatrix(bitmapDisplayed, unrotated, false)
            unrotated.postConcat(suppMatrix)
            return unrotated
        }

    protected fun calculateMaxZoom(): Float {
        if (bitmapDisplayed.bitmap == null) {
            return 1f
        }
        val fw = bitmapDisplayed.width.toFloat() / thisWidth.toFloat()
        val fh = bitmapDisplayed.height.toFloat() / thisHeight.toFloat()
        return Math.max(fw, fh) * 4 // 400%
    }

    protected open fun zoomTo(scale: Float, centerX: Float, centerY: Float) {
        var scale = scale
        if (scale > maxZoom) {
            scale = maxZoom
        }
        val oldScale = scale
        val deltaScale = scale / oldScale
        suppMatrix.postScale(deltaScale, deltaScale, centerX, centerY)
        imageMatrix = imageViewMatrix
        center()
    }

    protected fun zoomTo(
        scale: Float, centerX: Float,
        centerY: Float, durationMs: Float
    ) {
        val incrementPerMs = (scale - scale) / durationMs
        val startTime = System.currentTimeMillis()
        handler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val currentMs = Math.min(durationMs, (now - startTime).toFloat())
                val target = scale + incrementPerMs * currentMs
                zoomTo(target, centerX, centerY)
                if (currentMs < durationMs) {
                    handler.post(this)
                }
            }
        })
    }

    protected fun zoomTo(scale: Float) {
        val cx = width / 2f
        val cy = height / 2f
        zoomTo(scale, cx, cy)
    }

    protected open fun zoomIn() {
        zoomIn(SCALE_RATE)
    }

    protected open fun zoomOut() {
        zoomOut(SCALE_RATE)
    }

    protected fun zoomIn(rate: Float) {
        if (getScale() >= maxZoom) {
            return  // Don't let the user zoom into the molecular level
        }
        if (bitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f
        suppMatrix.postScale(rate, rate, cx, cy)
        imageMatrix = imageViewMatrix
    }

    protected fun zoomOut(rate: Float) {
        if (bitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f

        // Zoom out to at most 1x
        val tmp = Matrix(suppMatrix)
        tmp.postScale(1f / rate, 1f / rate, cx, cy)
        if (getScale(tmp) < 1f) {
            suppMatrix.setScale(1f, 1f, cx, cy)
        } else {
            suppMatrix.postScale(1f / rate, 1f / rate, cx, cy)
        }
        imageMatrix = imageViewMatrix
        center()
    }

    protected open fun postTranslate(dx: Float, dy: Float) {
        suppMatrix.postTranslate(dx, dy)
    }

    protected fun panBy(dx: Float, dy: Float) {
        postTranslate(dx, dy)
        imageMatrix = imageViewMatrix
    }

    companion object {
        private const val SCALE_RATE = 1.25f
    }
}