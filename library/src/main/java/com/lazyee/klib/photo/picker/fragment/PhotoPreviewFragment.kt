package com.lazyee.klib.photo.picker.fragment

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
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

internal class PhotoPreviewFragment : Fragment(),OnBigImageClickListener {
    private val removePhotoList = mutableListOf<Photo>()
    private var mPagerAdapter: PhotoPagerAdapter? = null
    private var maxCount :Int = PhotoPickerActivity.DEFAULT_MAX_COUNT
    private lateinit var binding:FragmentPhotoPreviewBinding
    private val selectedPhotoAdapter:SelectedPhotoAdapter by lazy { SelectedPhotoAdapter(photoList!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).barColor(R.color.pager_bg).init()

        mPagerAdapter = PhotoPagerAdapter(activity!!, photoList!!)
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
        binding.vpPhotos.currentItem = arguments?.getInt(ARG_CURRENT_ITEM) ?: 0
        binding.vpPhotos.offscreenPageLimit = 5

        binding.rlBottom.visibility = if(isPreviewSelected)View.VISIBLE else View.GONE
        updateDoneTextView()

        binding.tvPhotoNumber.text = "1"
        binding.vpPhotos.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ){}

            override fun onPageSelected(position: Int) {
                binding.rvPhotos.adapter?.notifyDataSetChanged()
                if(isPreviewSelected){
                    if(isRemoved(photoList!![position])){
                        binding.llPhotoNumber.visibility = View.GONE
                        binding.ivCheck.visibility = View.VISIBLE
                    }else{
                        binding.llPhotoNumber.visibility = View.VISIBLE
                        binding.ivCheck.visibility = View.GONE
                    }
                    binding.tvPhotoNumber.text = (position + 1).toString()
                }

                smoothToCenter(binding.rvPhotos,position,200)
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

            binding.tvDone.isEnabled = photoList!!.isNotEmpty()
            binding.tvDone.setOnClickListener {
                PhotoPickerActivity.selectedPhotoList.removeAll(removePhotoList)
                (activity as PhotoPickerActivity).clickDone()
            }
            binding.rlCheck.visibility = View.VISIBLE
        }else{
            binding.rlCheck.visibility = View.GONE
        }

        binding.llPhotoNumber.setOnClickListener {
            val currentIndex = binding.vpPhotos.currentItem
            val photo = photoList!![currentIndex]
            if(currentIndex + 1 < photoList!!.size && removePhotoList.firstOrNull() == photoList!![currentIndex + 1]){
                removePhotoList.add(0,photo)
            }else{
                removePhotoList.add(photo)

                photoList!!.remove(photo)
                photoList!!.add(photo)

                binding.vpPhotos.setCurrentItem(photoList!!.size - 1,false)
            }

            mPagerAdapter?.notifyDataSetChanged()
            selectedPhotoAdapter.notifyDataSetChanged()

            binding.llPhotoNumber.visibility = View.GONE
            binding.ivCheck.visibility = View.VISIBLE
            updateDoneTextView()
        }

        binding.ivCheck.setOnClickListener {
            val photo = photoList!![binding.vpPhotos.currentItem]
            if(removePhotoList.isNotEmpty()){
                val firstRemovePhoto = removePhotoList[0]
                val insertIndex = photoList!!.indexOf(firstRemovePhoto)

                removePhotoList.remove(photo)
                photoList!!.remove(photo)
                photoList!!.add(insertIndex,photo)
            }

            binding.vpPhotos.setCurrentItem(photoList!!.indexOf(photo),false)
            mPagerAdapter?.notifyDataSetChanged()
            selectedPhotoAdapter.notifyDataSetChanged()
            binding.llPhotoNumber.visibility = View.VISIBLE
            binding.ivCheck.visibility = View.GONE
            updateDoneTextView()
        }
    }

    private fun updateDoneTextView(){
        binding.tvDone.text = getString(R.string.done,
            (photoList!!.size - removePhotoList.size).toString(),
            maxCount.toString())

        binding.tvDone.isEnabled = photoList!!.size != removePhotoList.size
    }

    /**
     * 是否被移除
     * @param photo Photo
     * @return Boolean
     */
    private fun isRemoved(photo: Photo): Boolean {
        return removePhotoList.contains(photo)
    }

    override fun onDetach() {
        super.onDetach()

        PhotoPickerActivity.selectedPhotoList.removeAll(removePhotoList)
        (activity as PhotoPickerActivity).notifyDataSetChanged()
    }

    companion object {
        const val ARG_CURRENT_ITEM = "ARG_CURRENT_ITEM"
        var isPreviewSelected = false
        var maxCount = PhotoPickerActivity.DEFAULT_MAX_COUNT
        var photoList :MutableList<Photo>? = null

        fun newInstance(currentItem: Int): PhotoPreviewFragment {
            val f = PhotoPreviewFragment()
            val args = Bundle()
            args.putInt(ARG_CURRENT_ITEM, currentItem)
            f.arguments = args
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

    /**
     * 滚动到中间位置
     */
    fun smoothToCenter(recyclerView: RecyclerView, position: Int?,time: Int){
        try {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val scroller =  CenterSmoothScroller(activity!!,time)
            var finalPosition = 0
            if(position != null){
                finalPosition = if(position < 0) 0 else position
            }

            scroller.targetPosition = finalPosition
            if(scroller.isRunning)return
            layoutManager.startSmoothScroll(scroller)
        }catch (e:Exception){
            e.printStackTrace()
            recyclerView.scrollToPosition(position?:0)
        }
    }

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


    private fun dip2px(dpValue:Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    //选中的图片的适配器
    inner class SelectedPhotoAdapter(private val photos:List<Photo>) :RecyclerView.Adapter<SelectedPhotoViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedPhotoViewHolder {
            return SelectedPhotoViewHolder(TemplatePreviewSelectedPhotoBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        }

        override fun onBindViewHolder(holder: SelectedPhotoViewHolder, position: Int) {
            val photo = photos[position]
            holder.binding.ivImage.loadThumbnail(photo.path!!)
            holder.binding.ivImage.setOnClickListener {
                binding.vpPhotos.setCurrentItem(position,false)
            }
            holder.binding.maskView.visibility = if(isRemoved(photo))View.VISIBLE else View.GONE
            holder.binding.llCurrent.visibility = if(position == binding.vpPhotos.currentItem)View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int {
            return photos.size
        }
    }

    inner class SelectedPhotoViewHolder(val binding: TemplatePreviewSelectedPhotoBinding):RecyclerView.ViewHolder(binding.root)

    internal class CenterSmoothScroller(context: Context,private val time:Int) : LinearSmoothScroller(context) {

        override fun calculateTimeForDeceleration(dx: Int): Int {
            return time
        }

        override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
            return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            return 50f / displayMetrics.densityDpi
        }
    }
}

