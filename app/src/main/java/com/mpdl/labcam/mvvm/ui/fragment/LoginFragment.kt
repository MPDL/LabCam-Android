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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.mpdl.labcam.BuildConfig
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.repository.bean.ChannelBean
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.ui.adapter.ChannelAdapter
import com.mpdl.labcam.mvvm.ui.widget.DirTreeViewPopup
import com.mpdl.labcam.mvvm.vm.LoginViewModel
import com.mpdl.labcam.treeviewbase.TreeNode
import com.mpdl.mvvm.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber


class LoginFragment: BaseFragment<LoginViewModel>() {
    override fun initViewModel(): LoginViewModel = getViewModel()
    private lateinit var channelData: List<ChannelBean>

    private var dirTreeViewPopup: DirTreeViewPopup? = null
    private var passwordStatus = false

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
                        btn_forgot.visibility = View.VISIBLE
                    }
                    1->{
                        btn_register.visibility = View.GONE
                        btn_forgot.visibility = View.GONE
                    }
                    2->{
                        btn_register.visibility = View.GONE
                        btn_forgot.visibility = View.GONE
                    }
                }
            }

        }

        setListener()

        observe(mViewModel.getDirDialogState()){state->
            state?.let{
                dirTreeViewPopup?.let {
                    val curItem = dirTreeViewPopup?.curTreeNode as TreeNode<KeeperDirItem>
                    if (curItem.content?.id == state.node?.content?.id){
                        it.setData(curItem, state.list)
                    }
                }
            }
        }


        if (!mViewModel.getUploadFileUrl().hasObservers()){
            observe(mViewModel.getUploadFileUrl()){
                if (!TextUtils.isEmpty(it)){
                    dirTreeViewPopup?.dismiss()
                    Timber.d("actionLoginFragmentToCameraFragment")
                    Navigation.findNavController(requireActivity(),R.id.my_nav_host_fragment)
                        .navigate(LoginFragmentDirections.actionLoginFragmentToCameraFragment())
                }
            }
        }


        observe(mViewModel.getUiState()){
            if (it.loginSuccess){
                showDirTreeViewPopup()
            }

            if (it.showFileSelectorDialog){
                showDirTreeViewPopup()
            }
        }

        if (BuildConfig.DEBUG){
            et_account.setText(BuildConfig.DEBUG_ACCOUNT)
            et_password.setText(BuildConfig.DEBUG_PASSWORD)
        }

    }

    private fun getDir(node: TreeNode<KeeperDirItem>){
        if (node.isRoot){
            mViewModel.getRepos(node)
        }else {
            mViewModel.getDir(node = node, dirItem = node.content)
        }
    }

    private fun setListener(){

        btn_login.setOnClickListener {
//            dirTreeViewPopup.show()
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
            goWebView("${et_url.text.toString().trim()}/accounts/register/")
        }

        btn_forgot.setOnClickListener {
            goWebView("${et_url.text.toString().trim()}/accounts/password/reset/")
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

        ll_password_status.setOnClickListener {
            passwordStatus = !passwordStatus
            if (passwordStatus){
                et_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                iv_password_status.setImageResource(R.mipmap.ic_eye_on)
            }else{
                iv_password_status.setImageResource(R.mipmap.ic_eye_off)
                et_password.transformationMethod = PasswordTransformationMethod.getInstance()
            }
        }
    }

    private fun goWebView(url:String){
        val uri: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun loginVisible(){
        if (!TextUtils.isEmpty(et_account.text.toString()) && !TextUtils.isEmpty(et_password.text.toString())){
            btn_login.setBackgroundResource(R.drawable.bg_login_btn)
        }else if (TextUtils.isEmpty(et_account.text.toString()) || TextUtils.isEmpty(et_password.text.toString())){
            btn_login.setBackgroundResource(R.drawable.shape_login_btn_off)
        }
    }

    private fun showDirTreeViewPopup(){
        if(dirTreeViewPopup != null){
            dirTreeViewPopup?.release();
            dirTreeViewPopup = null
        }
        dirTreeViewPopup = DirTreeViewPopup.builder(requireContext()).build().setDirTreeViewListener(object :
            DirTreeViewPopup.DirTreeViewListener {
            override fun onConfirm(item: KeeperDirItem?) {
                if (item == null){
                    showMessage("Please select a directory")
                }else{
                    mViewModel.getUploadLink(item)
                }
            }

            override fun onItemClick(node: TreeNode<*>) {
                if (node.childList.isEmpty()){
                    getDir(node as TreeNode<KeeperDirItem>)
                }
            }
        })
        dirTreeViewPopup?.show()
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