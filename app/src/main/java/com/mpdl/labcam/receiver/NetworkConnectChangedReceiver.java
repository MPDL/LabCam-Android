package com.mpdl.labcam.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.mpdl.labcam.mvvm.ui.activity.MainActivity;
import com.mpdl.labcam.service.UploadFilesService;

import timber.log.Timber;

public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkConnect";
    public static final String TAG1 = "NetworkConnect_1";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Log.i(TAG1, "CONNECTIVITY_ACTION");

            UploadFilesService filesService = MainActivity.Companion.getUploadFilesService();
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
            int networkType = -1;
            if (activeNetwork != null) { // connected to the internet
                if (activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi
                        networkType = 1;
                        Log.e(TAG, "WiFi available ");
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        networkType = 0;
                        Log.e(TAG, "Cellular available ");
                    }
                } else {
                    networkType = -1;
                    Log.e(TAG, "No internet, please check your connection ");
                }

                //网络发送改变
                Timber.e("networkType "+networkType+" getCurNetworkType "+MainActivity.Companion.getCurNetworkType());
                if (networkType != MainActivity.Companion.getCurNetworkType()){
                    MainActivity.Companion.setCurNetworkType(networkType);
                    if (networkType == 1 || networkType == MainActivity.Companion.getUploadNetwork()){
                        Timber.e("filesService "+filesService);
                        //开始上传图片
                        if (filesService != null){
                            filesService.startUploadFile();
                        }
                    }else {
                        //停止上传图片
                        if (filesService != null){
                            filesService.stopUploadFile();
                        }
                    }
                }

                Log.e(TAG1, "info.getTypeName()" + activeNetwork.getTypeName());
                Log.e(TAG1, "getSubtypeName()" + activeNetwork.getSubtypeName());
                Log.e(TAG1, "getState()" + activeNetwork.getState());
                Log.e(TAG1, "getDetailedState()"
                        + activeNetwork.getDetailedState().name());
                Log.e(TAG1, "getDetailedState()" + activeNetwork.getExtraInfo());
                Log.e(TAG1, "getType()" + networkType);
            } else {   // not connected to the internet
                Log.e(TAG, "No internet，please check your connection ");
                MainActivity.Companion.setCurNetworkType(-1);
                //stop upload file
                if (filesService != null){
                    filesService.stopUploadFile();
                }
            }


        }
    }


}
