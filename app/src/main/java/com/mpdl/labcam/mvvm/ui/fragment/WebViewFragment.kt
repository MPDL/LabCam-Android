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

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.navigation.fragment.navArgs
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.vm.LaunchViewModel
import com.mpdl.mvvm.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_webview.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber


class WebViewFragment: BaseFragment<LaunchViewModel>() {
    override fun initViewModel(): LaunchViewModel = getViewModel()

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_dir_tree_view,container,false)

    /** AndroidX navigation arguments */
    private val args: WebViewFragmentArgs by navArgs()

    override fun initData(savedInstanceState: Bundle?) {
//        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

//        webview.webChromeClient = object : WebChromeClient() {
//            override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                Timber.d("newProgress $newProgress")
//                if (newProgress == 100) {
//                    progressBar.visibility = View.GONE
//                } else {
//                    progressBar.visibility = View.VISIBLE
//                    progressBar.progress = newProgress
//                }
//                super.onProgressChanged(view, newProgress)
//            }
//        }
//
//        val settings = webview.settings
//        settings.javaScriptEnabled = true
//        settings.useWideViewPort = true//设定支持viewport
//        settings.loadWithOverviewMode = true
//        settings.builtInZoomControls = true
//
//        settings.useWideViewPort = true;
//        settings.loadWithOverviewMode = true;
//
//        settings.setSupportZoom(true);
//        settings.builtInZoomControls = true;
//        settings.displayZoomControls = false;
//
//        webview.loadUrl(args.url)
//
//        webview.setOnKeyListener { _, _, keyEvent ->
//            if(keyEvent.keyCode == KeyEvent.KEYCODE_BACK && webview.canGoBack()) {
//                webview.goBack();
//                true
//            }else{
//                false
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
//        webview.onResume()
    }

    override fun onPause() {
        super.onPause()
//        webview.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webview.destroy()
    }
}