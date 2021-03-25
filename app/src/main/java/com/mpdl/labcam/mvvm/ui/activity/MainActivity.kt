package com.mpdl.labcam.mvvm.ui.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mpdl.labcam.BuildConfig
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem
import com.mpdl.labcam.mvvm.vm.MainViewModel
import com.mpdl.labcam.receiver.NetworkConnectChangedReceiver
import com.mpdl.labcam.service.UploadFilesService
import com.mpdl.mvvm.base.BaseActivity
import com.mpdl.mvvm.common.Preference
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.ext.android.getViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity: BaseActivity<MainViewModel>() {
    override fun initViewModel(): MainViewModel = getViewModel()

    override fun initView(savedInstanceState: Bundle?): Int = R.layout.activity_main

    override fun initData(savedInstanceState: Bundle?) {
        context = applicationContext
        okHttpClient = mViewModel.okHttpClient
        Timber.d("okHttpClient $okHttpClient")
        isResume = true

        galleryList.clear()

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

    override fun onResume() {
        super.onResume()
        isResume = true
    }

    override fun onPause() {
        super.onPause()
        isResume = false
        uploadFilesService?.errorUrl = null
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
        const val EVENT_CHANGE_UPLOAD_PATH:String = "event_change_upload_path"
        const val EVENT_CHANGE_OCR_TEXT:String = "event_change_ocr_text"
        private const val SP_UPLOAD_URL = "sp_upload_url"
        private const val SP_UPLOAD_URL_TIME = "sp_upload_url_time"
        private const val SP_TOKEN = "sp_token"
        private const val SP_SAVE_DIRECTORY = "sp_save_directory"
        private const val SP_UPLOAD_NETWORK = "sp_upload_network"
        private const val SP_TREE_NOTES = "sp_tree_notes"


        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        const val VIDEO_EXTENSION = ".mp4"
        const val TEXT_EXTENSION = ".md"

        var octText = ""

        var context: Context? = null

        var okHttpClient: OkHttpClient? = null

        var uploadFilesService: UploadFilesService? = null

        var baseUrl: String = BuildConfig.API_BASE_URL

        var isCheckDirPath = false

        private var retrofit: Retrofit? = null

        val galleryList :MutableList<Uri> = mutableListOf()

        var openOcr:Boolean = false

        var isResume = false

        /** Helper function used to create a timestamped file */
        fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.GERMAN)
                .format(System.currentTimeMillis()) + extension)

        fun createImage(baseFolder: File) =
            File(baseFolder, SimpleDateFormat(FILENAME, Locale.GERMAN)
                .format(System.currentTimeMillis()) + PHOTO_EXTENSION)

        fun createText(baseFolder: File,name: String)=
            File(baseFolder, name.replace(".","_") + TEXT_EXTENSION)



        fun cleanRetrofit(){
            retrofit = null
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
                                .serializeNulls()
                                //enable maps with objects as keys, keys are Strings by default
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
        private var uploadUrlTime = 0L
        fun getUploadUrl(): String?{
            if (TextUtils.isEmpty(uploadUrl)){
                uploadUrl = Preference.preferences.getString(SP_UPLOAD_URL,"")
            }
            return uploadUrl
        }

        fun getUploadUrlTime():Long{
            if (uploadUrlTime == 0L){
                uploadUrlTime = Preference.preferences.getLong(SP_UPLOAD_URL_TIME,0)
            }
            return uploadUrlTime;
        }

        fun setUploadUrl(url: String){
            uploadUrlTime = System.currentTimeMillis()
            uploadUrl = url
            Preference.preferences.edit()
                .putString(SP_UPLOAD_URL,url)
                .putLong(SP_UPLOAD_URL_TIME, uploadUrlTime)
                .apply()
        }

        private var curDirItem: KeeperDirItem? = null

        fun setCurDirItem(item: KeeperDirItem?){
            curDirItem = item
            if (item == null){
                Preference.preferences.edit().putString(SP_SAVE_DIRECTORY,null).apply()
            }else{
                Preference.preferences.edit().putString(SP_SAVE_DIRECTORY,Gson().toJson(item)).apply()
            }
        }

        fun getCurDirItem():KeeperDirItem?{
            if (curDirItem == null){
                var jsonStr = Preference.preferences.getString(SP_SAVE_DIRECTORY,null)
                if (!TextUtils.isEmpty(jsonStr)){
                    curDirItem = Gson().fromJson(jsonStr,KeeperDirItem::class.java)
                }
            }
            return curDirItem
        }

        private var curTreeNodes: List<KeeperDirItem>? = null

        fun setCurTreeNodes(curTreeNodes: List<KeeperDirItem>?){
            this.curTreeNodes = curTreeNodes
            val json = Gson().toJson(curTreeNodes)
            Timber.e("curTreeNodes $json")
            Preference.preferences.edit().putString(SP_TREE_NOTES,json).apply()
        }
        fun getCurTreeNodes(): List<KeeperDirItem>?{
            if(curTreeNodes == null){
                var json = Preference.preferences.getString(SP_TREE_NOTES,null)
                json?.let {
                    val type = object : TypeToken<List<KeeperDirItem>>(){}.type
                    curTreeNodes  = Gson().fromJson<List<KeeperDirItem>>(json,type)
                    Timber.e("curTreeNodes $curTreeNodes")
                }
            }
            return curTreeNodes
        }

        fun loginOut(){
            galleryList.clear()
            setToken("")
            setUploadUrl("")
            setCurDirItem(null)
            cleanRetrofit()
        }

        fun startUpload(){
            Timber.d("startUpload, uploadNetwork:${getUploadNetwork()}  curNetworkType:$curNetworkType")
            if(getUploadNetwork() == 1 || curNetworkType == getUploadNetwork()){
                Timber.d("startUpload uploadFilesService:$uploadFilesService")
                uploadFilesService?.startUploadFile()
            }
        }

        /**
         * Upload network options
         * 0/1
         * Cellular/wifi
         */
        private var uploadNetwork:Int = -1

        /**
         * Current network type
         * -1/0/1
         * not network/Cellular/wifi
         */
        var curNetworkType: Int = -1

        fun setUploadNetwork(position: Int){
            uploadNetwork = position
            startUpload()
            Preference.preferences.edit().putInt(SP_UPLOAD_NETWORK, uploadNetwork).apply()
        }

        fun getUploadNetwork(): Int{
            if (uploadNetwork == -1){
                uploadNetwork = Preference.preferences.getInt(SP_UPLOAD_NETWORK,1)
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

        fun isNetworkConnected(): Boolean {
            return try {
                val mConnectivityManager: ConnectivityManager = context!!
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mNetworkInfo: NetworkInfo = mConnectivityManager.activeNetworkInfo!!
                mNetworkInfo.isAvailable
            }catch (e:Exception){
                false
            }
        }

        fun sendNotification(context: Context){
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "message"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mChannel = NotificationChannel(channelId, BuildConfig.BUILD_TYPE, NotificationManager.IMPORTANCE_DEFAULT)
                mChannel.description = "upload success notify"
                mChannel.enableLights(true)
                mChannel.enableVibration(true)
                mNotificationManager.createNotificationChannel(mChannel)
            }
            var mBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.mipmap.ic_launcher))
                .setContentTitle("LabCam")
                .setContentText("Upload Completed")
            mNotificationManager.notify(SystemClock.uptimeMillis().toInt(), mBuilder.build())
        }

    }
}