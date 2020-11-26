/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mpdl.labcam.mvvm.ui.fragment

import android.Manifest
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.vm.LaunchViewModel
import com.mpdl.mvvm.base.BaseFragment
import com.tbruyelle.rxpermissions3.RxPermissions
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber
import java.util.*


/** Fragment used for each individual page showing a photo inside of [GalleryFragment] */
class LaunchFragment: BaseFragment<LaunchViewModel>() {
    override fun initViewModel(): LaunchViewModel = getViewModel()

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_launch,container,false)

    private val timer = Timer()
    private  var timerOut = false

    override fun initData(savedInstanceState: Bundle?) {
        timer()
    }

    override fun onResume() {
        super.onResume()
        if (timerOut){
            timer()
        }
    }

    private fun timer(){
        RxPermissions(this)
            .requestEachCombined(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe {
                timer.schedule(object : TimerTask(){
                    override fun run() {
                        timerOut = true
                        if (TextUtils.isEmpty(MainActivity.getToken())){
                            Timber.d("actionLaunchFragmentToLoginFragment")
                            Navigation.findNavController(requireView())
                                .navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment())
                        }else{
                            Navigation.findNavController(requireView())
                                .navigate(LaunchFragmentDirections.actionLaunchFragmentToCameraFragment())
                        }
                    }
                },1800)
            }
    }

}