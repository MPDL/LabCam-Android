package com.mpdl.labcam.mvvm.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.vm.CameraViewModel
import com.mpdl.mvvm.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_gallery.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber
import java.io.File
import java.util.*
class GalleryFragment: BaseFragment<CameraViewModel>()  {
    override fun initViewModel(): CameraViewModel = getViewModel()

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gallery,container,false)

    private lateinit var mediaList: MutableList<Uri>

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class MediaPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = mediaList.size
        override fun getItem(position: Int): Fragment = PhotoFragment().setUri(mediaList[position])
        override fun getItemPosition(obj: Any): Int = POSITION_NONE
    }


    override fun initData(savedInstanceState: Bundle?) {

        retainInstance = true
        // Get root directory of media from navigation arguments
//        val rootDirectory = File(args.rootDirectory)
        mediaList = MainActivity.galleryList
        Timber.d("mediaList: $mediaList")
        // Populate the ViewPager and implement a cache of two media items
        photo_view_pager.apply {
            offscreenPageLimit = 2
            adapter = MediaPagerAdapter(childFragmentManager)
        }

        // Handle back button press
        back_button.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.my_nav_host_fragment).popBackStack()
        }

    }

}