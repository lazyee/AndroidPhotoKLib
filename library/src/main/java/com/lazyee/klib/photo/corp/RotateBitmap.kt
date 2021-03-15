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

import android.graphics.Bitmap
import android.graphics.Matrix

/*
 * Modified from original in AOSP.
 */
internal class RotateBitmap(var bitmap: Bitmap?, rotation: Int) {
    var rotation: Int = rotation % 360

    // rectangle will be changed after rotation, so the delta values
    // are based on old & new width/height respectively.
    // By default this is an identity matrix
    val rotateMatrix: Matrix
        get() {
            // By default this is an identity matrix
            val matrix = Matrix()
            if (bitmap != null && rotation != 0) {
                // We want to do the rotation at origin, but since the bounding
                // rectangle will be changed after rotation, so the delta values
                // are based on old & new width/height respectively.
                val cx = bitmap!!.width / 2
                val cy = bitmap!!.height / 2
                matrix.preTranslate(-cx.toFloat(), -cy.toFloat())
                matrix.postRotate(rotation.toFloat())
                matrix.postTranslate((width / 2).toFloat(), (height / 2).toFloat())
            }
            return matrix
        }
    private val isOrientationChanged: Boolean
        get() = rotation / 90 % 2 != 0
    val height: Int
        get() {
            if (bitmap == null) return 0
            return if (isOrientationChanged) {
                bitmap!!.width
            } else {
                bitmap!!.height
            }
        }
    val width: Int
        get() {
            if (bitmap == null) return 0
            return if (isOrientationChanged) {
                bitmap!!.height
            } else {
                bitmap!!.width
            }
        }

    fun recycle() {
        if (bitmap != null) {
            bitmap!!.recycle()
            bitmap = null
        }
    }

}