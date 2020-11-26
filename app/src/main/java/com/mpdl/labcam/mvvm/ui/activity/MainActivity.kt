package com.mpdl.labcam.mvvm.ui.activity

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import androidx.navigation.Navigation
import com.mpdl.labcam.BuildConfig
import com.mpdl.labcam.R
import com.mpdl.labcam.receiver.NetworkConnectChangedReceiver
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.service.UploadFilesService
import com.mpdl.labcam.mvvm.vm.MainViewModel
import com.mpdl.mvvm.base.BaseActivity
import com.mpdl.mvvm.common.Preference
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.ext.android.getViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File

class MainActivity: BaseActivity<MainViewModel>()  {
    override fun initViewModel(): MainViewModel = getViewModel()

    override fun initView(savedInstanceState: Bundle?): Int = R.layout.activity_main

    override fun initData(savedInstanceState: Bundle?) {
        okHttpClient = mViewModel.okHttpClient
        Timber.d("okHttpClient $okHttpClient")

        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        filter.addAction("android.net.wifi.STATE_CHANGE")
        registerReceiver(NetworkConnectChangedReceiver(), filter)


        bindService(Intent(this,UploadFilesService::class.java),object : ServiceConnection{
            override fun onServiceDisconnected(p0: ComponentName?) {
                uploadFilesService = null
            }

            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                uploadFilesService = (binder as UploadFilesService.MyBinder).service
            }
        }, Context.BIND_AUTO_CREATE)
    }

    override fun onBackPressed() {
        val curLabel = Navigation.findNavController(this,R.id.my_nav_host_fragment)
            .currentDestination!!.label
        if ("LoginFragment" == curLabel || "CameraFragment" == curLabel){
            val home = Intent(Intent.ACTION_MAIN)
            home.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            home.addCategory(Intent.CATEGORY_HOME)
            startActivity(home)
        }else{
            super.onBackPressed()
        }
    }

    companion object {
        public const val EVENT_UPLOAD_OVER:String = "event_upload_over"
        private const val SP_UPLOAD_URL = "sp_upload_url"
        private const val SP_TOKEN = "sp_token"
        private const val SP_SAVE_DIRECTORY = "sp_save_directory"
        private const val SP_UPLOAD_NETWORK = "sp_upload_network"

        var okHttpClient: OkHttpClient? = null

        var uploadFilesService: UploadFilesService? = null

        var baseUrl: String = BuildConfig.API_BASE_URL

        var isCheckDirPath = false

        private var retrofit: Retrofit? = null

        fun cleanRetrofit(){
            retrofit = null;
        }
        fun getRetrofit(): Retrofit?{
            Timber.d("okHttpClient $okHttpClient")
            okHttpClient?.let {
                if (retrofit == null){
                    retrofit = Retrofit.Builder()
                        .client(it)
                        .baseUrl(baseUrl)//BuildConfig.API_BASE_URL
                        .addConverterFactory(GsonConverterFactory.create(
                            GsonBuilder()
                                //支持序列化值为 null 的参数
                                .serializeNulls()
                                //支持将序列化 key 为 Object 的 Map, 默认只能序列化 key 为 String 的 Map
                                .enableComplexMapKeySerialization()
                                .create()))
                        .build()
                }
            }
            return retrofit
        }


        private var token: String? = ""
        fun getToken(): String? {
            if (TextUtils.isEmpty(token)){
                token = Preference.preferences.getString(SP_TOKEN,"")
            }
            return token
        }

        fun setToken(token: String){
            this.token = token
            Preference.preferences.edit().putString(MainActivity.SP_TOKEN,token).apply()
        }

        private var uploadUrl: String? = ""
        fun getUploadUrl(): String?{
            if (TextUtils.isEmpty(token)){
                uploadUrl = Preference.preferences.getString(SP_UPLOAD_URL,"")
            }
            return uploadUrl
        }

        fun setUploadUrl(url: String){
            uploadUrl = url
            Preference.preferences.edit().putString(SP_UPLOAD_URL,url).apply()
        }

        private var saveDirectoryBean: SaveDirectoryBean? = null
        fun setSaveDirectory(bean: SaveDirectoryBean?){
            saveDirectoryBean = bean
            if (bean == null){
                Preference.preferences.edit().putString(SP_SAVE_DIRECTORY,null).apply()
            }else{
                Preference.preferences.edit().putString(SP_SAVE_DIRECTORY,Gson().toJson(bean)).apply()
            }
        }
        fun getSaveDirectory():SaveDirectoryBean?{
            if (saveDirectoryBean == null){
                var jsonStr = Preference.preferences.getString(SP_SAVE_DIRECTORY,null)
                if (!TextUtils.isEmpty(jsonStr)){
                    saveDirectoryBean = Gson().fromJson(jsonStr,SaveDirectoryBean::class.java)
                }
            }
            return saveDirectoryBean
        }

        fun loginOut(){
            setToken("")
            setUploadUrl("")
            setSaveDirectory(null)
            cleanRetrofit()
        }

        fun startUpload(){
            if(getUploadNetwork() == 1 || curNetworkType == getUploadNetwork()){
                uploadFilesService?.startUploadFile()
            }
        }

        /**
         * 用户选择的上传网络
         * 0/1 wifi/Cellular
         */
        private var uploadNetwork:Int = -1

        /**
         * 当前网络状态
         * -1/0/1
         * not network/wifi/Cellular
         */
        var curNetworkType: Int = -1

        fun setUploadNetwork(position: Int){
            uploadNetwork = position
            startUpload()
            Preference.preferences.edit().putInt(SP_UPLOAD_NETWORK, uploadNetwork).apply()
        }

        fun getUploadNetwork(): Int{
            if (uploadNetwork == -1){
                uploadNetwork = Preference.preferences.getInt(SP_UPLOAD_NETWORK,0)
            }
            return uploadNetwork
        }



        val EXTENSION_WHITELIST = arrayOf("JPG")
        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()){
                mediaDir
            } else {
                appContext.filesDir
            }
        }
    }
}