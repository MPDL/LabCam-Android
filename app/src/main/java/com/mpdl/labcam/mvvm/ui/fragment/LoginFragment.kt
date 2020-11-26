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

import android.app.ProgressDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.navigation.Navigation
import com.mpdl.labcam.BuildConfig
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.repository.bean.ChannelBean
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.ui.adapter.ChannelAdapter
import com.mpdl.labcam.mvvm.ui.widget.FileSelectorDialog
import com.mpdl.labcam.mvvm.vm.LoginViewModel
import com.mpdl.mvvm.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber


class LoginFragment: BaseFragment<LoginViewModel>() {
    override fun initViewModel(): LoginViewModel = getViewModel()
    private lateinit var channelData: List<ChannelBean>

    private lateinit var fileSelectorDialog: FileSelectorDialog

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login,container,false)

    override fun initData(savedInstanceState: Bundle?) {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        channelData = mViewModel.getChannels()
        spinner_channel.adapter = ChannelAdapter(requireContext(),channelData)
        spinner_channel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                tv_spinner.text = channelData[position].name
                et_url.setText(channelData[position].url)
                when(position){
                    0->{
                        btn_register.visibility = View.VISIBLE
                    }
                    1->{
                        btn_register.visibility = View.GONE
                    }
                    2->{
                        btn_register.visibility = View.GONE
                    }
                }
            }

        }

        fileSelectorDialog = FileSelectorDialog(requireContext())
        fileSelectorDialog.setCanceledOnTouchOutside(false)

        setListener()

        observe(mViewModel.getDirectoryData()){list->
            fileSelectorDialog?.let {
                it.setData(list)
            }
        }

        if (!mViewModel.getUploadFileUrl().hasObservers()){
            observe(mViewModel.getUploadFileUrl()){
                if (!TextUtils.isEmpty(it)){
                    fileSelectorDialog?.dismiss()
                    Timber.d("actionLoginFragmentToCameraFragment")
                    Navigation.findNavController(requireActivity(),R.id.my_nav_host_fragment)
                        .navigate(LoginFragmentDirections.actionLoginFragmentToCameraFragment())
                }
            }
        }


        observe(mViewModel.getUiState()){
            if (it.loginSuccess){
                if (MainActivity.getSaveDirectory() == null){
                    fileSelectorDialog.show()
                }else{
                    mViewModel.getUploadLink(MainActivity.getSaveDirectory()!!)
                }
            }

            if (it.showFileSelectorDialog){
                fileSelectorDialog?.show()
            }
        }

        if (BuildConfig.DEBUG){
            et_account.setText(BuildConfig.DEBUG_ACCOUNT)
            et_password.setText(BuildConfig.DEBUG_PASSWORD)
        }

    }

    private fun getDir(bean: SaveDirectoryBean?){
        if (bean == null){
            mViewModel.getRepos()
        }else {
            mViewModel.getDir(bean)
        }
    }

    private fun setListener(){
        fileSelectorDialog.setFileSelectorListener(object :
            FileSelectorDialog.FileSelectorListener {
            override fun onBack(bean: SaveDirectoryBean?) {
                getDir(bean)
            }

            override fun onSave(bean: SaveDirectoryBean?) {
                if (bean == null){
                    showMessage("Please select a directory")
                }else{
                    MainActivity.setSaveDirectory(bean)
                    mViewModel.getUploadLink(bean)
                }

            }

            override fun onItemClick(bean: SaveDirectoryBean?) {
                getDir(bean)
            }
        })

        btn_login.setOnClickListener {
            val username = et_account.text.toString().trim()
            val password = et_password.text.toString().trim()
            val baseUrl = et_url.text.toString().trim()
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(baseUrl)){
                return@setOnClickListener
            }
            MainActivity.baseUrl = baseUrl
            MainActivity.cleanRetrofit()
            mViewModel.login(username,password)
        }

        btn_register.setOnClickListener {
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections
                    .actionLoginFragmentToWebViewFragment("${et_url.text.toString().trim()}/accounts/register/"))
        }

        btn_forgot.setOnClickListener {
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections
                    .actionLoginFragmentToWebViewFragment("${et_url.text.toString().trim()}/accounts/password/reset/"))
        }

        tv_spinner.setOnClickListener {
            spinner_channel.performClick()
        }

        et_account.addTextChangedListener {
            loginVisible()
        }

        et_password.addTextChangedListener {
            loginVisible()
        }
    }

    private fun loginVisible(){
        if (!TextUtils.isEmpty(et_account.text.toString()) && !TextUtils.isEmpty(et_password.text.toString())){
            btn_login.setBackgroundResource(R.drawable.bg_login_btn)
        }else if (TextUtils.isEmpty(et_account.text.toString()) || TextUtils.isEmpty(et_password.text.toString())){
            btn_login.setBackgroundResource(R.drawable.shape_login_btn_off)
        }
    }

    private var progressDialog: ProgressDialog? = null

    override fun showLoading() {
        if (progressDialog == null){
            progressDialog = ProgressDialog(requireContext())
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.setMessage("LOGIN...")
        }
        progressDialog?.show()
        val window: Window? = progressDialog!!.window
        val lp = window!!.attributes
        lp.gravity = Gravity.CENTER
        lp.width = (window.windowManager.defaultDisplay.width * 0.85).toInt()
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        progressDialog!!.window!!.attributes = lp
    }

    override fun hideLoading() {
        progressDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.cleanUiState()
    }
}