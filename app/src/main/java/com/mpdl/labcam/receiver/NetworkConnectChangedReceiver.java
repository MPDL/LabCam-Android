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


/**
 * 网络改变监控广播
 * <p>
 * 监听网络的改变状态,只有在用户操作网络连接开关(wifi,mobile)的时候接受广播,
 * 然后对相应的界面进行相应的操作，并将 状态 保存在我们的APP里面
 * <p>
 * <p>
 * Created by xujun
 */
public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkConnect";
    public static final String TAG1 = "NetworkConnect_1";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
        // 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。见log
        // 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
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
                        Log.e(TAG, "当前WiFi连接可用 ");
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        networkType = 0;
                        Log.e(TAG, "当前移动网络连接可用 ");
                    }
                } else {
                    networkType = -1;
                    Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
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
                Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                MainActivity.Companion.setCurNetworkType(-1);
                //停止上传图片
                if (filesService != null){
                    filesService.stopUploadFile();
                }
            }


        }
    }


}
