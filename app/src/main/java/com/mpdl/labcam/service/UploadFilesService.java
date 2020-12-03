package com.mpdl.labcam.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean;
import com.mpdl.labcam.mvvm.ui.activity.MainActivity;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.simple.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.reactivex.Flowable;
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
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mVideoExecutor = new ThreadPoolExecutor(2,3,10, TimeUnit.SECONDS,new LinkedBlockingQueue<>(100));
    }

    public void startUploadFile(){
        if (TextUtils.isEmpty(MainActivity.Companion.getUploadUrl())){
            return;
        }
        if (uploadStart){
            return;
        }
        Timber.d("startUploadFile");
        uploadStart = true;
        File fileDir = MainActivity.Companion.getOutputDirectory(this);
        Timber.d("fileDir: "+ Arrays.toString(fileDir.list()));
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
                        }
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {
                        uploadStart = false;
                        if (fileDir.listFiles().length == 0){
                            Timber.i("onComplete 上传完成：");
                        }else {
                            MainActivity.Companion.startUpload();
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

    class UploadFileRunnable implements Runnable{
        private File file;
        public UploadFileRunnable(File file){
            this.file  = file;
        }
        @Override
        public void run() {
            if (file != null){
                try {
                    if (file.getName().matches(".*\\.md")){
                        if (TextUtils.isEmpty(openText(file.getPath()))){
                            return;
                        }
                    }

                    Timber.i("上传 File: "+file.getAbsolutePath());
                    uploadingNameList.add(file.getName());

                    MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(),
                            RequestBody.create(MediaType.parse("multipart/form-data"), file));

                    SaveDirectoryBean saveDir = MainActivity.Companion.getSaveDirectory();
                    String path = "/";
                    if (saveDir != null){
                        path = saveDir.getPath();
                    }
                    try {
                        if ("".equals(MainActivity.Companion.getUploadUrl())){
                            stopUploadFile();
                            return;
                        }
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
                                            //上传成功
                                            uploadingNameList.remove(file.getName());
                                            file.delete();
                                            EventBus.getDefault().post("", MainActivity.EVENT_UPLOAD_OVER);
                                        }else {
                                            //上传失败
                                            Timber.i("上传失败 response："+response.message());
                                            uploadingNameList.remove(file.getName());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                        Timber.i("上传失败 t : "+t.getMessage());
                                        t.printStackTrace();
                                        uploadingNameList.remove(file.getName());
                                    }
                                });

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    uploadingNameList.remove(file.getName());
                    e.printStackTrace();
                    Timber.i("上传失败："+e.getMessage());
                }

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
