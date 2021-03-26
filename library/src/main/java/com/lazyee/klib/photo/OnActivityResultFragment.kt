package com.lazyee.klib.photo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.lazyee.klib.photo.corp.Crop

/**
 * @Author leeorz
 * @Date 3/12/21-3:07 PM
 * @Description:用来获取图片结果的fragment
 */
class OnActivityResultFragment : Fragment() {

    var intent:Intent? = null
    var photoHelper:PhotoHelper? = null
    var requestCode:Int ? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        intent?:return
        requestCode ?:return
        startActivityForResult(intent, requestCode!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.remove(this)
            ?.commitAllowingStateLoss()
        if(resultCode != Activity.RESULT_OK)return
        if(isHandleImage(requestCode)){
            photoHelper?.dealImage(requestCode, resultCode, data)
        }
    }

    /**
     * 是否是处理图片
     * @param requestCode
     * @return
     */
    private fun isHandleImage(requestCode: Int): Boolean {
        return requestCode == PhotoHelper.ALBUM
                || requestCode == PhotoHelper.CAMERA
                || requestCode == Crop.REQUEST_CROP
                || requestCode == Crop.REQUEST_PICK
    }

    companion object{
        val TAG = "OnActivityResultFragment"
    }
}