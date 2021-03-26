package com.lazyee.klib.photo.picker.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.gyf.immersionbar.ImmersionBar
import com.lazyee.klib.photo.Photo
import com.lazyee.klib.photo.R
import com.lazyee.klib.photo.databinding.FragmentPhotoPreviewBinding
import com.lazyee.klib.photo.databinding.TemplatePreviewSelectedPhotoBinding
import com.lazyee.klib.photo.extension.loadThumbnail
import com.lazyee.klib.photo.picker.activity.PhotoPickerActivity
import com.lazyee.klib.photo.picker.adapter.OnBigImageClickListener
import com.lazyee.klib.photo.picker.adapter.PhotoPagerAdapter
import java.util.*

internal class PhotoPreviewFragment : Fragment(),OnBigImageClickListener {
//    var paths = mutableListOf<String>()
    private var mPagerAdapter: PhotoPagerAdapter? = null
    private var thumbnailTop = 0
    private var thumbnailLeft = 0
    private var thumbnailWidth = 0
    private var thumbnailHeight = 0
    private var hasAnim = false
    private val colorizerMatrix = ColorMatrix()
    private var currentIndex = 0
    private var isPreviewSelected = false
    private lateinit var binding:FragmentPhotoPreviewBinding
    private val selectedPhotoAdapter:SelectedPhotoAdapter by lazy { SelectedPhotoAdapter(PhotoPickerActivity.selectedPhotoList) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).barColor(R.color.pager_bg).init();
        val bundle = arguments
        if (bundle != null) {
            hasAnim = bundle.getBoolean(ARG_HAS_ANIM)
            currentIndex = bundle.getInt(ARG_CURRENT_ITEM)
            thumbnailTop = bundle.getInt(ARG_THUMBNAIL_TOP)
            thumbnailLeft = bundle.getInt(ARG_THUMBNAIL_LEFT)
            thumbnailWidth = bundle.getInt(ARG_THUMBNAIL_WIDTH)
            thumbnailHeight = bundle.getInt(ARG_THUMBNAIL_HEIGHT)
            isPreviewSelected = bundle.getBoolean(ARG_IS_PREVIEW_SELECTED)
        }
        mPagerAdapter = PhotoPagerAdapter(activity!!, PhotoPickerActivity.selectedPhotoList)
        mPagerAdapter?.onBigImageClickListener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentPhotoPreviewBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vpPhotos.adapter = mPagerAdapter
        binding.vpPhotos.currentItem = currentIndex
        binding.vpPhotos.offscreenPageLimit = 5

        binding.rlBottom.visibility = if(isPreviewSelected)View.VISIBLE else View.GONE

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
            ){}

            override fun onPageSelected(position: Int) {
                hasAnim = currentIndex == position
                currentIndex = position
                binding.rvPhotos.adapter?.notifyDataSetChanged()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.ivClose.setOnClickListener {
            if (!activity!!.isFinishing) {
                activity?.onBackPressed()
            }
        }

        if(isPreviewSelected){
            binding.rvPhotos.layoutManager = LinearLayoutManager(activity!!).also {
                it.orientation = LinearLayoutManager.HORIZONTAL
            }
            binding.rvPhotos.adapter = selectedPhotoAdapter

            binding.tvDone.isEnabled = PhotoPickerActivity.selectedPhotoList.isNotEmpty()
            binding.tvDone.setOnClickListener { (activity as PhotoPickerActivity).clickDone() }
        }
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
        val colorizer = ObjectAnimator.ofFloat(this@PhotoPreviewFragment,
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
        val colorizer = ObjectAnimator.ofFloat(this@PhotoPreviewFragment, "saturation", 1f, 0f)
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

    companion object {
        const val ARG_CURRENT_ITEM = "ARG_CURRENT_ITEM"
        const val ANIM_DURATION = 0L
        const val ARG_THUMBNAIL_TOP = "THUMBNAIL_TOP"
        const val ARG_THUMBNAIL_LEFT = "THUMBNAIL_LEFT"
        const val ARG_THUMBNAIL_WIDTH = "THUMBNAIL_WIDTH"
        const val ARG_THUMBNAIL_HEIGHT = "THUMBNAIL_HEIGHT"
        const val ARG_HAS_ANIM = "HAS_ANIM"
        const val ARG_IS_PREVIEW_SELECTED = "IS_PREVIEW_SELECTED"
        fun newInstance(currentItem: Int,isPreviewSelected:Boolean = false): PhotoPreviewFragment {
            val f = PhotoPreviewFragment()
            val args = Bundle()
            args.putInt(ARG_CURRENT_ITEM, currentItem)
            args.putBoolean(ARG_HAS_ANIM, false)
            args.putBoolean(ARG_IS_PREVIEW_SELECTED,isPreviewSelected)
            f.arguments = args
            return f
        }

        fun newInstance(
            currentItem: Int,
            screenLocation: IntArray,
            thumbnailWidth: Int,
            thumbnailHeight: Int,
            isPreviewSelected:Boolean = false
        ): PhotoPreviewFragment {
            val f = newInstance(currentItem)
            f.arguments!!
                .putInt(ARG_THUMBNAIL_LEFT, screenLocation[0])
            f.arguments!!.putInt(ARG_THUMBNAIL_TOP, screenLocation[1])
            f.arguments!!
                .putInt(ARG_THUMBNAIL_WIDTH, thumbnailWidth)
            f.arguments!!
                .putInt(ARG_THUMBNAIL_HEIGHT, thumbnailHeight)
            f.arguments!!.putBoolean(ARG_HAS_ANIM, true)
            f.arguments!!.putBoolean(ARG_IS_PREVIEW_SELECTED,isPreviewSelected)
            return f
        }
    }

    //动画
    private var isAnimPlaying = false
    private val topAnimIn by lazy { AnimationUtils.loadAnimation(activity,R.anim.anim_preview_top_in).also {
        it.setAnimationListener(object :Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {
                isAnimPlaying = true
                binding.llTop.visibility = View.VISIBLE

                if(!isPreviewSelected)return
                binding.rlBottom.visibility = View.VISIBLE

            }

            override fun onAnimationEnd(animation: Animation?) {
                isAnimPlaying = false
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }

        })
    } }
    private val topAnimOut by lazy { AnimationUtils.loadAnimation(activity,R.anim.anim_preview_top_out).also {
        it.setAnimationListener(object :Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {
                isAnimPlaying = true
            }

            override fun onAnimationEnd(animation: Animation?) {
                isAnimPlaying = false
                binding.llTop.visibility = View.GONE
                if(!isPreviewSelected)return
                binding.rlBottom.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        })
    } }

    private val bottomAnimIn by lazy { AnimationUtils.loadAnimation(activity,R.anim.anim_preview_bottom_in) }
    private val bottomAnimOut by lazy { AnimationUtils.loadAnimation(activity,R.anim.anim_preview_bottom_out) }

    override fun onClickBigImage() {
        if(binding.llTop.isShown){
            binding.llTop.startAnimation(topAnimOut)
        }else{
            binding.llTop.startAnimation(topAnimIn)
        }

        if(isPreviewSelected){
            if(binding.rlBottom.isShown){
                binding.rlBottom.startAnimation(bottomAnimIn)
            }else{
                binding.rlBottom.startAnimation(bottomAnimOut)
            }
        }
    }

    //选中的图片的适配器
    inner class SelectedPhotoAdapter(private val photos:List<Photo>) :RecyclerView.Adapter<SelectedPhotoViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedPhotoViewHolder {
            return SelectedPhotoViewHolder(TemplatePreviewSelectedPhotoBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }

        override fun onBindViewHolder(holder: SelectedPhotoViewHolder, position: Int) {
            holder.binding.ivImage.loadThumbnail(photos[position].path!!)
            holder.binding.ivImage.setOnClickListener {
                binding.vpPhotos.setCurrentItem(position,false)
            }
            holder.binding.llCurrent.visibility = if(position == currentIndex)View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int {
            return photos.size
        }
    }

    inner class SelectedPhotoViewHolder(val binding: TemplatePreviewSelectedPhotoBinding):RecyclerView.ViewHolder(binding.root)
}

