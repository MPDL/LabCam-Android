package com.mpdl.labcam.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem;
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean;
import com.mpdl.labcam.mvvm.ui.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.simple.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;
import timber.log.Timber;

public class UploadFilesService extends Service {
    protected CompositeDisposable mCompositeDisposable;
    private Subscription mFileSubscription;
    private ThreadPoolExecutor mVideoExecutor;
    private boolean uploadStart;
    private List<String> uploadingNameList = new ArrayList<>();
    private String errorUrl;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.e(" onCreate()");

        mVideoExecutor = new ThreadPoolExecutor(2,3,10, TimeUnit.SECONDS,new LinkedBlockingQueue<>(100));

        addDispose(Observable.interval(60,30,TimeUnit.SECONDS,Schedulers.io())
                .subscribe(aLong -> {
                    File fileDir = MainActivity.Companion.getOutputDirectory(this);
                    Timber.d("setData uploadStart:"+uploadStart +"  uploadingNameList.size():"+uploadingNameList.size() +" \nfileDir.listFiles():"+fileDir.listFiles().length);
                    if (!uploadStart && uploadingNameList.size() == 0
                            && fileDir.listFiles().length>0){
                        startUploadFile();
                    }
                }));
    }

    public synchronized void startUploadFile(){
        Timber.d( "uploadStart:"+uploadStart);
        if (uploadStart){
            if (mFileSubscription != null){
                mFileSubscription.request(1);
            }
            return;
        }
        File fileDir = MainActivity.Companion.getOutputDirectory(this);
        Timber.d("startUploadFile fileDir: "+ Arrays.toString(fileDir.list()));
        if (fileDir.listFiles().length == 0){
            return;
        }
        Timber.d("startUploadFile");
        uploadStart = true;
        Flowable.fromArray(fileDir.listFiles())
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<File>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        mFileSubscription = s;
                        mFileSubscription.request(2);
                    }

                    @Override
                    public void onNext(File file) {
                        if (!uploadingNameList.contains(file.getName())){
                            mVideoExecutor.execute(new UploadFileRunnable(file));
                        }else {
                            mFileSubscription.request(1);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        uploadStart = false;
                        Timber.i("startUploadFile Throwable"+t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        uploadStart = false;
                        if (fileDir.listFiles().length == 0){
                            Timber.i("startUploadFile onComplete 上传完成：");
                        }
                    }
                });

    }

    public void stopUploadFile(){
        uploadStart = false;
        if (mFileSubscription != null){
            mFileSubscription.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unDispose();
        this.mCompositeDisposable = null;
    }

    protected void addDispose(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);//将所有subscription放入,集中处理
    }

    protected void unDispose() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();//保证activity结束时取消所有正在执行的订阅
        }
    }

    public class  MyBinder extends Binder {
        public UploadFilesService getService(){
            return UploadFilesService.this;
        }
    }

    private String openText(String path) {
        String readStr = "";
        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] b = new byte[fis.available()];
            fis.read(b);
            readStr = new String(b);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return readStr;
    }


    private void saveBitmap(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_"+System.currentTimeMillis()+".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");
        values.put(MediaStore.Images.Media.TITLE, System.currentTimeMillis() + ".jpg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");
        }
        Uri external  = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver  = this.getContentResolver();
        Uri insertUri = resolver.insert(external, values);
        Timber.d("saveBitmap insertUri:$insertUri");
        if (insertUri != null){
            OutputStream os = null;
            try {
                os = resolver.openOutputStream(insertUri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                MainActivity.Companion.getGalleryMap().put(file.getName(),insertUri);
                file.delete();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (os != null){
                        os.flush();
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bitmap.recycle();
            }
        }
    }

    class UploadFileRunnable implements Runnable{
        private File file;
        public UploadFileRunnable(File file){
            this.file  = file;
        }
        @Override
        public void run() {
            if (file != null){
                try {
                    //没网络
                    if (!MainActivity.Companion.isNetworkConnected()){
                        Timber.e("上传 File: 没网络");
                        getNextFile(file);
                        return;
                    }
                    boolean networkMatch = MainActivity.Companion.getCurNetworkType() == 1 ||
                            MainActivity.Companion.getCurNetworkType() == MainActivity.Companion.getUploadNetwork();
                    //网络状态不匹配
                    if (!networkMatch){
                        Timber.e("上传 File: 网络状态不匹配");
                        getNextFile(file);
                        return;
                    }

                    //文件没内容
                    if (file.getName().matches(".*\\.md")){
                        if (TextUtils.isEmpty(openText(file.getPath()))){
                            Timber.e("上传 File: 文件没内容");
                            getNextFile(file);
                            return;
                        }
                    }

                    //上传地址不存在
                    if (TextUtils.isEmpty(MainActivity.Companion.getUploadUrl())){
                        Timber.e("上传 File: 上传地址不存在");
                        //通知修改地址
                        EventBus.getDefault().post("", MainActivity.EVENT_CHANGE_UPLOAD_PATH);
//                        getNextFile(file);
                        return;
                    }

                    uploadingNameList.add(file.getName());
                    MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(),
                            RequestBody.create(MediaType.parse("multipart/form-data"), file));

                    KeeperDirItem saveDir = MainActivity.Companion.getCurDirItem();
                    String path = "/";
                    if (saveDir != null){
                        path = saveDir.getPath();
                    }
                    Timber.i("上传 File: "+file.getAbsolutePath());
                    Timber.i("上传 Path : "+path);
                    try {
                        MainActivity.Companion.getRetrofit()
                                .create(UploadApi.class)
                                .uploadFile(MainActivity.Companion.getUploadUrl(),
                                        RequestBody.create(null, path),
                                        RequestBody.create(null, "1"),part)
                                .enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                        if (response!=null && response.isSuccessful()){
                                            Timber.i("上传成功：");
                                            errorUrl = null;
                                            //上传成功
                                            saveBitmap(file);
                                            getNextFile(file);
                                            EventBus.getDefault().post("", MainActivity.EVENT_UPLOAD_OVER);
                                        }else {
                                            //上传失败
                                            try {
                                                String errorJson = response.errorBody().string();
                                                Timber.e("上传失败 response："+errorJson);
                                                JSONObject jsonObject = new JSONObject(errorJson);
                                                String error = jsonObject.getString("error");
                                                Timber.e("上传失败 error："+error);
                                                if (error.contains("Parent dir doesn't exist.") || error.contains("Failed to get repo")){
                                                    if (errorUrl == null || error.equals(MainActivity.Companion.getUploadUrl())){
                                                        errorUrl = MainActivity.Companion.getUploadUrl();
                                                        EventBus.getDefault().post("", MainActivity.EVENT_CHANGE_UPLOAD_PATH);
                                                    }
                                                }else {
                                                    Toast.makeText(UploadFilesService.this,"Upload failed: "+error,Toast.LENGTH_LONG).show();
                                                }
                                            } catch (IOException | JSONException e) {
                                                e.printStackTrace();
                                            }
                                            getNextFile(file);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                        Timber.e("上传失败 t : "+t.getMessage());
                                        getNextFile(file);
                                        t.printStackTrace();
                                    }
                                });

                    }catch (Exception e){
                        e.printStackTrace();
                        getNextFile(file);
                    }

                } catch (Exception e) {
                    getNextFile(file);
                    e.printStackTrace();
                    Timber.i("上传失败："+e.getMessage());
                }

            }
        }

        private void getNextFile(File file){
            if (file != null && uploadingNameList!= null){
                uploadingNameList.remove(file.getName());
            }
            //请求上传下一个文件
            if (mFileSubscription != null){
                mFileSubscription.request(1);
            }
        }
    }

    interface UploadApi{
        @Multipart
        @POST
        Call<ResponseBody> uploadFile(@Url String url,
                                      @Part("parent_dir")RequestBody parent_dir ,
                                      @Part("replace")RequestBody replace,
                                      @Part MultipartBody.Part file);
    }


}
