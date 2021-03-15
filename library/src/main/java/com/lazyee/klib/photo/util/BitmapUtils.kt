package com.lazyee.klib.photo.util

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.BitmapFactory.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import java.io.*
import java.nio.channels.FileChannel

/**
 * bitmap帮助类
 */
internal object BitmapUtils {
    private const val DES_WIDTH = 50

    /**
     * @param context 上下文
     * @param rid     缩放的本地资源id
     * @return
     */
    fun scaleBitmap(context: Context, rid: Int): Bitmap {
        var bitmap: Bitmap
        val options = Options()
        //这里为ture，则下面bitmap为空（节省内存），并把bitmap对应资源的宽和高分别保存在options.outWidth和options.outHeight中
        options.inJustDecodeBounds = true
        options.inPurgeable = true
        bitmap = decodeResource(context.resources, rid, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight
        if (outWidth >= DES_WIDTH) {
            options.inSampleSize = outWidth / DES_WIDTH //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        }
        //注意：设置好缩放后，前面的true需要设置为false，不然bitmap一直为空
        options.inJustDecodeBounds = false
        bitmap = decodeResource(context.resources, rid, options)
        bitmap.recycle()
        return bitmap
    }

    /**
     * @param context 上下文
     * @param rid     缩放的本地资源id
     * @return
     */
    fun scaleBitmap(context: Context, rid: Int, dip: Int): Bitmap {
        var bitmap: Bitmap
        val options = Options()
        //这里为ture，则下面bitmap为空（节省内存），并把bitmap对应资源的宽和高分别保存在options.outWidth和options.outHeight中
        options.inJustDecodeBounds = true
        bitmap = decodeResource(context.resources, rid, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight
        //获取屏幕的宽高
        val dm = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(dm)
        val width = dm.widthPixels //得到宽度
        val height = dm.heightPixels //得到高度
        if (dip in 1..outWidth) {
            options.inSampleSize = outWidth / dip //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        } else {
            options.inSampleSize = outWidth / width //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        }
        //注意：设置好缩放后，前面的true需要设置为false，不然bitmap一直为空
        options.inJustDecodeBounds = false
        bitmap = decodeResource(context.getResources(), rid, options)
        return bitmap
    }

    /**
     * @param context  上下文
     * @param imageUri 图片路径
     * @return
     */
    fun scaleBitmap(context: Context?, imageUri: String?): Bitmap {
        var bitmap: Bitmap
        val options = Options()
        //这里为ture，则下面bitmap为空（节省内存），并把bitmap对应资源的宽和高分别保存在options.outWidth和options.outHeight中
        options.inJustDecodeBounds = true
        bitmap = decodeFile(imageUri, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight
        if (outWidth >= DES_WIDTH) {
            options.inSampleSize = outWidth / DES_WIDTH //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        }
        //注意：设置好缩放后，前面的true需要设置为false，不然bitmap一直为空
        options.inJustDecodeBounds = false
        bitmap = decodeFile(imageUri, options)
        return bitmap
    }

    /**
     * @param context  上下文
     * @param imageUri 图片路径
     * @return
     */
    fun scaleBitmapDefault(context: Context?, imageUri: String?): Bitmap {
        val bitmap: Bitmap
        val options = Options()
        //这里为ture，则下面bitmap为空（节省内存），并把bitmap对应资源的宽和高分别保存在options.outWidth和options.outHeight中
//		options.inJustDecodeBounds = true;
//		bitmap = BitmapFactory.decodeFile(imageUri, options);
//		int outWidth = options.outWidth;
//		int outHeight = options.outHeight;
//		if(outWidth >= DES_WIDTH){
//			options.inSampleSize = outWidth/DES_WIDTH;//注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
//		}
        //注意：设置好缩放后，前面的true需要设置为false，不然bitmap一直为空
//		options.inJustDecodeBounds = false;
        bitmap = decodeFile(imageUri, options)
        return bitmap
    }

    /**
     * @param context  上下文
     * @param imageUri 图片路径
     * @return
     */
    fun scaleBitmap(context: Context, imageUri: String?, dip: Int): Bitmap {
        var bitmap: Bitmap
        val options = Options()
        //这里为ture，则下面bitmap为空（节省内存），并把bitmap对应资源的宽和高分别保存在options.outWidth和options.outHeight中
        options.inJustDecodeBounds = true
        bitmap = decodeFile(imageUri, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight

        //获取屏幕的宽高
        val dm = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(dm)
        val width = dm.widthPixels //得到宽度
        val height = dm.heightPixels //得到高度
        if (dip > 0 && outWidth >= dip) {
            options.inSampleSize = outWidth / dip //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        } else {
            options.inSampleSize = outWidth / width //注意：这里的缩放等级必须是2的倍数（不够会就近取2的倍数值）
        }
        //注意：设置好缩放后，前面的true需要设置为false，不然bitmap一直为空
        options.inJustDecodeBounds = false
        bitmap = decodeFile(imageUri, options)
        return bitmap
    }

    /**
     * @param imageFilePath      图片文件的路径
     * @param imageUpperLimitPix 返回的bitmap宽或高的最高像素
     * @return
     */
    fun decodeFile(imageFilePath: String?, imageUpperLimitPix: Int): Bitmap? {
        try {
            val o = Options()
            o.inJustDecodeBounds = true
            decodeFile(imageFilePath, o)
            var widthTmp = o.outWidth
            var heightTmp = o.outHeight
            var scale = 1
            while (true) {
                if (widthTmp / 2 < imageUpperLimitPix && heightTmp / 2 < imageUpperLimitPix) break
                widthTmp /= 2
                heightTmp /= 2
                scale *= 2
            }
            val o2 = Options()
            o2.inSampleSize = scale
            return decodeFile(imageFilePath, o2)
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * 获取bitmap宽高像素值
     *
     * @param path
     * @return
     */
    fun getBitmapPxSize(path: String?): IntArray {
        val opts = Options()
        opts.inJustDecodeBounds = true
        decodeFile(path, opts)
        val width = opts.outWidth
        val height = opts.outHeight
        return intArrayOf(width, height)
    }

    /**
     * @param path
     * @param targetWidth
     * @return
     */
    fun getFullScreenBitmapSize(path: String?, targetWidth: Int): IntArray {
        val options = Options()
        options.inJustDecodeBounds = true
        decodeFile(path, options)
        var tempheight = options.outHeight
        var tempWidth = options.outWidth

        val d = tempWidth / (targetWidth * 1.0)
        tempWidth = targetWidth
        tempheight = (tempheight / d).toInt()

        return intArrayOf(tempWidth, tempheight)
    }

    /**
     * Drawable转bitmap
     *
     * @param d
     * @return
     */
    fun drawable2bitmap(d: Drawable?): Bitmap? {
        return if (d != null) (d as BitmapDrawable).bitmap else null
    }

    fun layerDrawable2bitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        val config =
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        bitmap = Bitmap.createBitmap(w, h, config)
        // 注意，下面三行代码要用到，否在在View或者surfaceview里的canvas.drawBitmap会看不到图
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * bitmap转drawable
     *
     * @param b
     * @return
     */
    fun bitmap2Drawable(b: Bitmap?): Drawable? {
        return if (b != null) BitmapDrawable(b) else null
    }

    /**
     * 创建带倒影的Bitmap
     *
     * @param originalImage 原bitmap
     * @param instanceColor 原图与倒影之间的颜色值，传null时默认为白色
     * @return bitmap
     * @author leeib
     */
    fun createReflectedBitmap(originalImage: Bitmap, instanceColor: Int?): Bitmap {
        val reflectionGap = 4
        val width = originalImage.width
        val height = originalImage.height
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        val reflectionImage =
            Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false)
        val bitmapWithReflection =
            Bitmap.createBitmap(width, height + height / 5, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithReflection)
        canvas.drawBitmap(originalImage, 0f, 0f, null)
        val defaultPaint = Paint()
        if (instanceColor != null) {
            defaultPaint.color = instanceColor
        } else {
            // 绘制的正方形为白色
            defaultPaint.color = Color.WHITE
        }
        canvas.drawRect(0f,
            height.toFloat(),
            width.toFloat(),
            (height + reflectionGap).toFloat(),
            defaultPaint)
        canvas.drawBitmap(reflectionImage, 0f, (height + reflectionGap).toFloat(), null)
        val paint = Paint()
        val shader: LinearGradient = LinearGradient(0f,
            originalImage.height.toFloat(),
            0f,
            bitmapWithReflection.height.toFloat() + reflectionGap,
            0x70ffffff,
            0x00ffffff,
            Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawRect(0f,
            height.toFloat(),
            width.toFloat(),
            (bitmapWithReflection.height + reflectionGap).toFloat(),
            paint)
        return bitmapWithReflection
    }

    /**
     * 创建带圆角的Bitmap
     *
     * @param bitmap 要转换的Bitmap
     * @param pixels 圆角的大小
     * @return
     */
    fun createRoundCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    /**
     * 图片与边框组合
     *
     * @param bm  原图片
     * @param res 边框资源，size为8，分别对应左上、左、左下、下、右下、右、右上、上，其中四角传0表示忽略对应的边框绘制
     * @return
     */
    fun createFrameBitmap(bm: Bitmap, res: IntArray, context: Context): Bitmap {
        val bmp = decodeBitmap(res[1], context)
        val bmp1 = decodeBitmap(res[3], context)
        // 边框的宽高
        val smallW = bmp.width
        val smallH = bmp1.height

        // 原图片的宽高
        val bigW = bm.width
        val bigH = bm.height
        val wCount = Math.ceil(bigW * 1.0 / smallW).toInt()
        val hCount = Math.ceil(bigH * 1.0 / smallH).toInt()

        // 组合后图片的宽高
        val newW = bigW + 2
        val newH = bigH + 2

        // 重新定义大小
        val newBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val p = Paint()
        p.color = Color.TRANSPARENT
        canvas.drawRect(Rect(0, 0, newW, newH), p)
        val rect = Rect(smallW, smallH, newW - smallW, newH - smallH)
        val paint = Paint()
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)

        // 绘原图
        canvas.drawBitmap(bm,
            ((newW - bigW - 2 * smallW) / 2 + smallW).toFloat(),
            ((newH - bigH - 2 * smallH) / 2 + smallH).toFloat(),
            null)
        // 绘边框
        // 绘四个角
        val startW = newW - smallW
        val startH = newH - smallH
        if (res[0] != 0) {
            var leftTopBm: Bitmap? = decodeBitmap(res[0], context) // 左上角
            canvas.drawBitmap(leftTopBm!!, 0f, 0f, null)
            leftTopBm.recycle()
            leftTopBm = null
        }
        if (res[2] != 0) {
            var leftBottomBm: Bitmap? = decodeBitmap(res[2], context) // 左下角
            canvas.drawBitmap(leftBottomBm!!, 0f, startH.toFloat(), null)
            leftBottomBm.recycle()
            leftBottomBm = null
        }
        if (res[4] != 0) {
            var rightBottomBm: Bitmap? = decodeBitmap(res[4], context) // 右下角
            canvas.drawBitmap(rightBottomBm!!, startW.toFloat(), startH.toFloat(), null)
            rightBottomBm.recycle()
            rightBottomBm = null
        }
        if (res[6] != 0) {
            var rightTopBm: Bitmap? = decodeBitmap(res[6], context) // 右上角
            canvas.drawBitmap(rightTopBm!!, startW.toFloat(), 0f, null)
            rightTopBm.recycle()
            rightTopBm = null
        }

        // 绘左右边框
        var leftBm: Bitmap? = decodeBitmap(res[1], context)
        var rightBm: Bitmap? = decodeBitmap(res[5], context)
        run {
            var i = 0
            while (i < hCount) {
                val h = smallH * (i + 1)
                canvas.drawBitmap(leftBm!!, 0f, h.toFloat(), null)
                canvas.drawBitmap(rightBm!!, startW.toFloat(), h.toFloat(), null)
                i++
            }
        }
        leftBm!!.recycle()
        leftBm = null
        rightBm!!.recycle()
        rightBm = null

        // 绘上下边框
        var bottomBm: Bitmap? = decodeBitmap(res[3], context)
        var topBm: Bitmap? = decodeBitmap(res[7], context)
        var i = 0
        while (i < wCount) {
            val w = smallW * (i + 1)
            canvas.drawBitmap(bottomBm!!, w.toFloat(), startH.toFloat(), null)
            canvas.drawBitmap(topBm!!, w.toFloat(), 0f, null)
            i++
        }
        bottomBm!!.recycle()
        bottomBm = null
        topBm!!.recycle()
        topBm = null
        canvas.save()
        canvas.restore()
        return newBitmap
    }

    fun decodeBitmap(res: Int, context: Context): Bitmap {
        return decodeResource(context.resources, res)
    }

    /**
     * 截取图片的中间的width * height的区域
     *
     * @param bm
     * @return
     */
    fun cropCenter(bm: Bitmap?, width: Int, height: Int): Bitmap? {
        if (bm == null) {
            return bm
        }
        val startWidth = (bm.width - width) / 2
        val startHeight = (bm.height - height) / 2
        val src = Rect(startWidth, startHeight, startWidth + width, startHeight + height)
        return cropBitmap(bm, src)
    }

    /**
     * 剪切图片
     *
     * @param bmp 被剪切的图片
     * @param src 剪切的位置
     * @return 剪切后的图片
     */
    fun cropBitmap(bmp: Bitmap?, src: Rect): Bitmap {
        val width = src.width()
        val height = src.height()
        val des = Rect(0, 0, width, height)
        val croppedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(croppedImage)
        canvas.drawBitmap(bmp!!, src, des, null)
        return croppedImage
    }

    /**
     * 调整Bitmap的大小,对变形没有保证
     *
     * @param bm
     * @param newHeight
     * @param newWidth
     * @param context
     * @return
     */
    fun getResizedBitmap(bm: Bitmap, newHeight: Int, newWidth: Int, context: Context?): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        // create a matrix for the manipulation
        val matrix = Matrix()

        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight)

        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
    }

    /**
     * 图片保存
     *
     * @param bmp
     */
    fun saveImage(bmp: Bitmap, filepath: String?): String {
        val file = File(filepath)
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 10, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    /**
     * 在控件中获取bitmap
     *
     * @param view
     * @return
     */
    fun convertViewToBitmap(view: View): Bitmap {
        view.buildDrawingCache()
        val bitmap = view.drawingCache
        view.isDrawingCacheEnabled = false
        return bitmap
    }

    /**
     * 获取options，用于获取宽度
     *
     * @param path
     * @return
     */
    fun getBitmapOptions(path: String?): Options {
        val options = Options()
        options.inJustDecodeBounds = true
        decodeFile(path, options) // 此时返回的bitmap为null
        return options
    }

    /**
     * 获取options
     *
     * @param context
     * @param resId
     * @return
     */
    fun getBitmapOptions(context: Context, resId: Int): Options {
        val options = Options()
        options.inJustDecodeBounds = true
        decodeResource(context.resources, resId, options)
        return options
    }

    /**
     * 计算simpleSize
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    fun calculateInSampleSize(options: Options, reqWidth: Int, reqHeight: Int): Int {
        // 源图片的高度和宽度
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    fun compressImage(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos) //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        val options = 100
        if (baos.toByteArray().size / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset() //重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 60, baos) //这里压缩options%，把压缩后的数据存放到baos中
        }
        val isBm = ByteArrayInputStream(baos.toByteArray()) //把压缩后的数据baos存放到ByteArrayInputStream中
        return decodeStream(isBm, null, null)
    }

    /**
     * 比例压缩
     *
     * @param srcPath
     * @return
     */
    fun scaleCompress(srcPath: String?): Bitmap {
        val options = getBitmapOptions(srcPath)
        options.inSampleSize = calculateInSampleSize(options, 720, 960) //设置缩放比例，最大需要2.63mb
        return decodeFile(srcPath, options)
    }

    /**
     * 使用文件通道的方式复制文件
     *
     * @param s 源文件
     * @param t 复制到的新文件
     */
    fun fileChannelCopy(s: File?, t: File?) {
        var fi: FileInputStream? = null
        var fo: FileOutputStream? = null
        var `in`: FileChannel? = null
        var out: FileChannel? = null
        try {
            fi = FileInputStream(s)
            fo = FileOutputStream(t)
            `in` = fi.channel //得到对应的文件通道
            out = fo.channel //得到对应的文件通道
            `in`.transferTo(0, `in`.size(), out) //连接两个通道，并且从in通道读取，然后写入out通道
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fi!!.close()
                `in`!!.close()
                fo!!.close()
                out!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 读取照片exif信息中的旋转角度
     *
     * @param path 照片路径
     * @return角度
     */
    fun readPictureDegree(path: String?): Int {
        var degree = 0
        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }

    /**
     * 判断图片是否旋转
     *
     * @param path
     * @return
     */
    @JvmStatic
    fun isPictureRotate(path: String?): Boolean {
        val degree = readPictureDegree(path)
        return degree == 90 || degree == 270
    }

    /**
     * @param angle
     * @param bitmap
     * @return
     * @desc <pre>旋转图片</pre>
     * @author Weiliang Hu
     * @date 2013-9-18
     */
    fun rotateImage(angle: Int, bitmap: Bitmap): Bitmap {
        // 旋转图片 动作
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())

        // 创建新的图片
        val resizedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return resizedBitmap
    }

    /**
     * 旋转图片并保存
     *
     * @param angle
     * @param filePath
     * @return
     */
    fun rotateImageAndSave(angle: Int, filePath: String?): String? {

        if (filePath != null) {
            var bitmap = decodeFile(filePath) //根据Path读取资源图片
            if (angle != 0) {
                // 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
                val m = Matrix()
                val width = bitmap.width
                val height = bitmap.height
                m.setRotate(angle.toFloat()) // 旋转angle度
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                    m, true) // 从新生成图片
                return saveImage(bitmap, filePath)
            }
        }
        return filePath
    }

    /**
     * 旋转图片并保存到相册
     *
     * @param filePath
     * @return
     */
//    @JvmStatic
//    fun ratingImageAndSave(filePath: String?): String? {
//        return ratingImage(readPictureDegree(filePath), filePath)
//    }
}