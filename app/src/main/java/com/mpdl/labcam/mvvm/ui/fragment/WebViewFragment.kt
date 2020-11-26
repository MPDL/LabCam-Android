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
    ): View = inflater.inflate(R.layout.fragment_webview,container,false)

    /** AndroidX navigation arguments */
    private val args: WebViewFragmentArgs by navArgs()

    override fun initData(savedInstanceState: Bundle?) {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Timber.d("newProgress $newProgress")
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE //加载完网页进度条消失
                } else {
                    progressBar.visibility = View.VISIBLE //开始加载网页时显示进度条
                    progressBar.progress = newProgress //设置进度值
                }
                super.onProgressChanged(view, newProgress)
            }
        }

        val settings = webview.settings
        settings.javaScriptEnabled = true
        //支持缩放
        settings.useWideViewPort = true//设定支持viewport
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true

        //设置自适应屏幕，两者合用
        settings.useWideViewPort = true; //将图片调整到适合webview的大小
        settings.loadWithOverviewMode = true; // 缩放至屏幕的大小

        //缩放操作
        settings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        settings.builtInZoomControls = true; //设置内置的缩放控件。若为false，则该WebView不可缩放
        settings.displayZoomControls = false; //隐藏原生的缩放控件

        webview.loadUrl(args.url)

        webview.setOnKeyListener { _, _, keyEvent ->
            if(keyEvent.keyCode == KeyEvent.KEYCODE_BACK && webview.canGoBack()) {
                webview.goBack();
                true
            }else{
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webview.onResume()
    }

    override fun onPause() {
        super.onPause()
        webview.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webview.destroy()
    }
}