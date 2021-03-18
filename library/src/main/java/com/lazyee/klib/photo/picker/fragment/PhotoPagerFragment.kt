package com.lazyee.klib.photo.picker.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.gyf.immersionbar.ImmersionBar
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.databinding.FragmentPhotoPagerBinding
import com.lazyee.klib.photo.picker.adapter.PhotoPagerAdapter
import java.util.*

internal class PhotoPagerFragment : Fragment() {
    var paths = mutableListOf<String>()
    private var mPagerAdapter: PhotoPagerAdapter? = null
    private var thumbnailTop = 0
    private var thumbnailLeft = 0
    private var thumbnailWidth = 0
    private var thumbnailHeight = 0
    private var hasAnim = false
    private val colorizerMatrix = ColorMatrix()
    private var currentItem = 0

    fun setPhotos(paths: List<String>?, currentItem: Int) {
        this.paths.clear()
        this.paths.addAll(paths!!)
        this.currentItem = currentItem
        binding.vpPhotos.currentItem = currentItem
        binding.vpPhotos.adapter!!.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).barColor(R.color.pager_bg).init();
        paths = ArrayList()
        val bundle = arguments
        if (bundle != null) {
            val pathArr = bundle.getStringArray(ARG_PATH)
            paths.clear()
            if (pathArr != null) {
                paths = ArrayList(Arrays.asList(*pathArr))
            }
            hasAnim = bundle.getBoolean(ARG_HAS_ANIM)
            currentItem = bundle.getInt(ARG_CURRENT_ITEM)
            thumbnailTop = bundle.getInt(ARG_THUMBNAIL_TOP)
            thumbnailLeft = bundle.getInt(ARG_THUMBNAIL_LEFT)
            thumbnailWidth = bundle.getInt(ARG_THUMBNAIL_WIDTH)
            thumbnailHeight = bundle.getInt(ARG_THUMBNAIL_HEIGHT)
        }
        mPagerAdapter = PhotoPagerAdapter(activity!!, paths)
    }
    private lateinit var binding:FragmentPhotoPagerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = FragmentPhotoPagerBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vpPhotos.adapter = mPagerAdapter
        binding.vpPhotos.currentItem = currentItem
        binding.vpPhotos.offscreenPageLimit = 5

        // Only run the animation if we're coming from the parent activity, not if
        // we're recreated automatically by the window manager (e.g., device rotation)
        if (savedInstanceState == null && hasAnim) {
            val observer = binding.vpPhotos.viewTreeObserver
            observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.vpPhotos.viewTreeObserver.removeOnPreDrawListener(this)

                    // Figure out where the thumbnail and full size versions are, relative
                    // to the screen and each other
                    val screenLocation = IntArray(2)
                    binding.vpPhotos.getLocationOnScreen(screenLocation)
                    thumbnailLeft -= screenLocation[0]
                    thumbnailTop -= screenLocation[1]
                    runEnterAnimation()
                    return true
                }
            })
        }
        binding.vpPhotos.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                hasAnim = currentItem == position
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    /**
     * The enter animation scales the picture in from its previous thumbnail
     * size/location, colorizing it in parallel. In parallel, the background of the
     * activity is fading in. When the pictue is in place, the text description
     * drops down.
     */
    private fun runEnterAnimation() {
        val duration = ANIM_DURATION

        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up


//    ViewHelper.setPivotX(mViewPager, 0);
//    ViewHelper.setPivotY(mViewPager, 0);
//    ViewHelper.setScaleX(mViewPager, (float) thumbnailWidth / mViewPager.getWidth());
//    ViewHelper.setScaleY(mViewPager, (float) thumbnailHeight / mViewPager.getHeight());
//    ViewHelper.setTranslationX(mViewPager, thumbnailLeft);
//    ViewHelper.setTranslationY(mViewPager, thumbnailTop);

        // Animate scale and translation to go from thumbnail to full size
        binding.vpPhotos.animate()
            .setDuration(3000).interpolator = DecelerateInterpolator()

        // Fade in the black background
        val bgAnim = ObjectAnimator.ofInt(
            binding.vpPhotos.background, "alpha", 0, 255)
        bgAnim.duration = duration
        bgAnim.start()

        // Animate a color filter to take the image from grayscale to full color.
        // This happens in parallel with the image scaling and moving into place.
        val colorizer = ObjectAnimator.ofFloat(this@PhotoPagerFragment,
            "saturation", 0f, 1f)
        colorizer.duration = duration
        colorizer.start()
    }

    /**
     * The exit animation is basically a reverse of the enter animation, except that if
     * the orientation has changed we simply scale the picture back into the center of
     * the screen.
     *
     * @param endAction This action gets run after the animation completes (this is
     * when we actually switch activities)
     */
    fun runExitAnimation(endAction: Runnable) {
        if (!arguments!!.getBoolean(ARG_HAS_ANIM, false) || !hasAnim) {
            endAction.run()
            return
        }
        val duration = ANIM_DURATION

        // Animate image back to thumbnail size/location
//    ViewPropertyAnimator.animate(mViewPager)
        binding.vpPhotos.animate()
            .setDuration(duration) //            .setDuration(5000)
            .alpha(0f) //            .setInterpolator(new AccelerateInterpolator())
            //            .rotationX(0)
            //            .rotationY(0)
            //            .scaleX((float) thumbnailWidth / mViewPager.getWidth())
            //            .scaleY((float) thumbnailHeight / mViewPager.getHeight())
            //            .translationX(thumbnailLeft)
            //            .translationY(thumbnailTop)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    endAction.run()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })

        // Fade out background
        val bgAnim = ObjectAnimator.ofInt(
            binding.vpPhotos.background, "alpha", 0)
        bgAnim.duration = duration
        bgAnim.start()

        // Animate a color filter to take the image back to grayscale,
        // in parallel with the image scaling and moving into place.
        val colorizer = ObjectAnimator.ofFloat(this@PhotoPagerFragment, "saturation", 1f, 0f)
        colorizer.duration = duration
        colorizer.start()
    }

    /**
     * This is called by the colorizing animator. It sets a saturation factor that is then
     * passed onto a filter on the picture's drawable.
     * @param value saturation
     */
    fun setSaturation(value: Float) {
        colorizerMatrix.setSaturation(value)
        val colorizerFilter = ColorMatrixColorFilter(colorizerMatrix)
        binding.vpPhotos.background.colorFilter = colorizerFilter
    }

    fun getCurrentItem(): Int {
        return binding.vpPhotos.currentItem
    }

    interface AddImagePagerFragment {
        fun addImagePagerFragment(imagePagerFragment: PhotoPagerFragment?)
    }

    companion object {
        const val ARG_PATH = "PATHS"
        const val ARG_CURRENT_ITEM = "ARG_CURRENT_ITEM"
        const val ANIM_DURATION = 0L
        const val ARG_THUMBNAIL_TOP = "THUMBNAIL_TOP"
        const val ARG_THUMBNAIL_LEFT = "THUMBNAIL_LEFT"
        const val ARG_THUMBNAIL_WIDTH = "THUMBNAIL_WIDTH"
        const val ARG_THUMBNAIL_HEIGHT = "THUMBNAIL_HEIGHT"
        const val ARG_HAS_ANIM = "HAS_ANIM"
        fun newInstance(paths: List<String>, currentItem: Int): PhotoPagerFragment {
            val f = PhotoPagerFragment()
            val args = Bundle()
            args.putStringArray(ARG_PATH, paths.toTypedArray())
            args.putInt(ARG_CURRENT_ITEM, currentItem)
            args.putBoolean(ARG_HAS_ANIM, false)
            f.arguments = args
            return f
        }

        fun newInstance(
            paths: List<String>,
            currentItem: Int,
            screenLocation: IntArray,
            thumbnailWidth: Int,
            thumbnailHeight: Int
        ): PhotoPagerFragment {
            val f = newInstance(paths, currentItem)
            f.arguments!!
                .putInt(ARG_THUMBNAIL_LEFT, screenLocation[0])
            f.arguments!!.putInt(ARG_THUMBNAIL_TOP, screenLocation[1])
            f.arguments!!
                .putInt(ARG_THUMBNAIL_WIDTH, thumbnailWidth)
            f.arguments!!
                .putInt(ARG_THUMBNAIL_HEIGHT, thumbnailHeight)
            f.arguments!!.putBoolean(ARG_HAS_ANIM, true)
            return f
        }
    }
}