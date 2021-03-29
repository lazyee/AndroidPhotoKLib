package com.lazyee.klib.photo.corp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lazyee.klib.photo.R

/**
 * Builder for crop Intents and utils for handling result
 */
class Crop private constructor(source: Uri, destination: Uri) {

    internal interface Extra {
        companion object {
            const val ASPECT_X = "aspect_x"
            const val ASPECT_Y = "aspect_y"
            const val MAX_X = "max_x"
            const val MAX_Y = "max_y"
            const val ERROR = "error"
        }
    }

    private val cropIntent: Intent

    /**
     * Set fixed aspect ratio for crop area
     *
     * @param x Aspect X
     * @param y Aspect Y
     */
    fun withAspect(x: Int, y: Int): Crop {
        cropIntent.putExtra(Extra.ASPECT_X, x)
        cropIntent.putExtra(Extra.ASPECT_Y, y)
        return this
    }

    /**
     * Crop area with fixed 1:1 aspect ratio
     */
    fun asSquare(): Crop {
        cropIntent.putExtra(Extra.ASPECT_X, 1)
        cropIntent.putExtra(Extra.ASPECT_Y, 1)
        return this
    }

    /**
     * Set maximum crop size
     *
     * @param width  Max width
     * @param height Max height
     */
    fun withMaxSize(width: Int, height: Int): Crop {
        cropIntent.putExtra(Extra.MAX_X, width)
        cropIntent.putExtra(Extra.MAX_Y, height)
        return this
    }


    fun start(fragment: Fragment?, requestCode: Int = REQUEST_CROP) {
        fragment?.startActivityForResult(getIntent(fragment.context), requestCode)
    }

    fun start(activity: Activity?, requestCode: Int = REQUEST_CROP) {
        activity?.startActivityForResult(getIntent(activity), requestCode)
    }

    /**
     * Send the crop Intent from a Fragment
     *
     * @param context  Context
     * @param fragment Fragment to receive result
     */
    fun start(context: Context?, fragment: Fragment) {
        start(context, fragment, REQUEST_CROP)
    }

    /**
     * Send the crop Intent with a custom request code
     *
     * @param context     Context
     * @param fragment    Fragment to receive result
     * @param requestCode requestCode for result
     */
    /**
     * Send the crop Intent from a support library Fragment
     *
     * @param context  Context
     * @param fragment Fragment to receive result
     */
    fun start(
        context: Context?,
        fragment: Fragment,
        requestCode: Int = REQUEST_CROP
    ) {
        fragment.startActivityForResult(getIntent(context), requestCode)
    }

    /**
     * Get Intent to start crop Activity
     *
     * @param context Context
     * @return Intent for CropImageActivity
     */
    fun getIntent(context: Context?): Intent {
        cropIntent.setClass(context!!, CropImageActivity::class.java)
        return cropIntent
    }

    companion object {
        const val REQUEST_CROP = 6709
        const val REQUEST_PICK = 9162
        const val RESULT_ERROR = 404

        /**
         * Create a crop Intent builder with source and destination image Uris
         *
         * @param source      Uri for image to crop
         * @param destination Uri for saving the cropped image
         */
        fun of(source: Uri, destination: Uri): Crop {
            return Crop(source, destination)
        }

        /**
         * Retrieve URI for cropped image, as set in the Intent builder
         *
         * @param result Output Image URI
         */
        fun getOutput(result: Intent): Uri {
            return result.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        }

        /**
         * Retrieve error that caused crop to fail
         *
         * @param result Result Intent
         * @return Throwable handled in CropImageActivity
         */
        fun getError(result: Intent?): Throwable {
            result?:return NullPointerException()
            return result.getSerializableExtra(Extra.ERROR) as Throwable
        }

        /**
         * Pick image from a Fragment
         *
         * @param context  Context
         * @param fragment Fragment to receive result
         */
        fun pickImage(context: Context?, fragment: Fragment) {
            pickImage(context, fragment, REQUEST_PICK)
        }
        /**
         * Pick image from an Activity with a custom request code
         *
         * @param activity    Activity to receive result
         * @param requestCode requestCode for result
         */
        /**
         * Pick image from an Activity
         *
         * @param activity Activity to receive result
         */
        @JvmOverloads
        fun pickImage(activity: Activity, requestCode: Int = REQUEST_PICK) {
            try {
                activity.startActivityForResult(imagePicker, requestCode)
            } catch (e: ActivityNotFoundException) {
                showImagePickerError(activity)
            }
        }


        /**
         * Pick image from a support library Fragment with a custom request code
         *
         * @param context     Context
         * @param fragment    Fragment to receive result
         * @param requestCode requestCode for result
         */
        /**
         * Pick image from a support library Fragment
         *
         * @param context  Context
         * @param fragment Fragment to receive result
         */
        fun pickImage(
            context: Context?,
            fragment: Fragment,
            requestCode: Int = REQUEST_PICK
        ) {
            try {
                fragment.startActivityForResult(imagePicker, requestCode)
            } catch (e: ActivityNotFoundException) {
                showImagePickerError(context)
            }
        }

        private val imagePicker: Intent
            private get() = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")

        private fun showImagePickerError(context: Context?) {
            Toast.makeText(context, R.string.crop_pick_error, Toast.LENGTH_SHORT).show()
        }
    }

    init {
        cropIntent = Intent()
        cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        cropIntent.data = source
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, destination)
    }
}