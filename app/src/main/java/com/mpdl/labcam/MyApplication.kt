package com.mpdl.labcam

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.mpdl.labcam.di.mAppModule
import com.mpdl.labcam.di.mViewModelModule
import com.mpdl.mvvm.common.Preference
import com.mpdl.mvvm.di.mSingleModule
import com.squareup.leakcanary.LeakCanary
import com.tencent.bugly.Bugly
import com.tencent.bugly.beta.Beta
import me.jessyan.autosize.AutoSizeConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin{
            if (BuildConfig.DEBUG){
                androidLogger()
            }
            // use the Android context given there
            androidContext(this@MyApplication)
            modules(listOf(mAppModule,mSingleModule, mViewModelModule))
        }

        if (BuildConfig.DEBUG) {
            LeakCanary.install(this)
            Timber.plant(Timber.DebugTree())
        }

        setAutoSizeConfig()

        initBugly()

        Preference.setContext(applicationContext)
    }

    /**
     * Screen Ration
     * < 0.6 based on width（handy）
     * > 0.6 based on height（tablet）
     */
    private fun setAutoSizeConfig(){
        val metric = DisplayMetrics()
        val mWindowManager: WindowManager =
            this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mWindowManager.defaultDisplay.getMetrics(metric)
        val width: Int = metric.widthPixels
        val height: Int = metric.heightPixels
        Timber.e("width: $width  height: $height width/height${width.toDouble()/height} isBaseOnWidth:${width.toDouble()/height<0.6}")
        AutoSizeConfig.getInstance().isBaseOnWidth = width.toDouble()/height<0.6
    }

    private fun initBugly(){
        Beta.largeIconId = R.mipmap.ic_launcher
        Beta.smallIconId = R.mipmap.ic_launcher
        Beta.autoCheckUpgrade = true
        Beta.upgradeCheckPeriod = 60 * 1000
        Bugly.init(applicationContext, BuildConfig.BUGLY_APP_ID, BuildConfig.DEBUG)
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // clear Glide cache
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Glide.get(this).clearMemory()
        }
        // trim memory
        Glide.get(this).trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // low memory clear Glide cache
        Glide.get(this).clearMemory()
    }
}