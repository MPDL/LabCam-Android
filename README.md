### 运行前需要添加 
labcam_config.gradle文件，内容如下
```
ext{

    labcam_config = [
            "key-alias"      :   "***",
            "key-password"   :   "***",
            "store-password" :   "***",
            "bugly-app-id"   :   "\"此处需要替换成自己的BuglyAppId\"",
            "debug-account"   :   "\"测试账号\"",
            "debug-password"   :   "\"测试账号的密码\""
    ]
}
```



###（V1.0.4 ）LabCam Testing Report on 22.12.2020
- [x] 1. push notification的措辞需要修改为：Upload Completed
- [ ] 2. 一个没有push notification（测试机器为三星平板）的情况：关闭wifi-照相-退出app但不kill - 打开wifi - 照片随后上传成功但没有收到notification；
- [ ] 3. OCR功能的bug：当用横屏模式扫描横着放的书时，OCR无法识别文字。但将书竖着放用横屏模式扫描时，扫描成功；
- [x] 4. 测试过程中收到一个“Upload failed: Permission denied”的错误报告；
- [ ] 5. OCR功能的优化：
        1）扫描文档时，让文字保持不动3秒。在目前的版本中，被扫描的内容一直在动。
        2）点击快门键后，自动release掉被扫描的文字，以提醒用户这一内容已经扫描完毕。
        3）UI方面，让显示text的place holder（灰色部分）保持大小不变。即不需要根据text的长短来变化尺寸。

###（V1.0.2 ）LabCam Testing Report on 10.12.2020
- [x] 1. 相机界面横屏时 拍摄不变 只改变icon 方向
- [x] 2. 重新打开应用程序时提示保存路径及上传方式
- [x] 3. Specific use case: select the folder “test 1.1.1”(see the left screenshot) - tap Confirm - take photos - exit LabCam - delete the folder “test 1.1.1” in KEEPER - open LabCam again - pop up the alert - click Change - show the former layer of the folder tree (see the right screen shot).  4、添加上传成功通知
- [x] 4. 添加上传成功通知
- [x] 5. library 下的 folders 重复
- [x] 6. library 打不开
- [x] 7. 未显示共享的文件夹


###（V1.0.1 ）LabCam Testing Report on 03.12.2020
- [x] ***1. Logo*** 
In the Android system, the logo shows in various shapes on the desktop. Adjust the logo’s
shape according to the WeChat icon in the left screenshot.

- [x] ***2. Login Page***
Delete the button and link of “Forgot Password?” on these two pages (when the user selects
SeaCloud.cc or Others).

- [x] ***3. Upload in Background Failed***
The current problem is that the user has to activate the app to upload the photos into Keeper.
- Case 1: Turn on WiFi ▶ open the app ▶ take 5 photos ▶ exit the app ▶ upload failed.
- Case 2: Turn on WiFi ▶ open the app ▶ take 5 photos ▶ turn off WiFi during uploading ▶
take another 5 photos ▶ exit the app ▶ turn on WiFi ▶ upload failed.
But our goal is:
- All photos will be backed up in the background and be uploaded after the device
reconnects to the internet.
- Upload all photos no matter the app is active or not.

- [ ] ***4. Camera***
In the case of rotating the screen between landscape mode and portrait mode, the camera
page is going to be black.

- [ ] ***5. OCR Function***
- Design a page for the scanned text. The current spacing makes the text look crowded and
unreadable.
- No file in .md format generated after scanning.
- This reminder shows on the screen even after deactivating the OCR button. Only when the
user clicks the preview and then back to this page again, this reminder will vanish.


### TODO 
- [x] 启动页
- [x] 获取所有library，本地过滤没权限的不显示
- [x] 缓存keeper 目录接口
- [x] 修改上传逻辑
    - 预览只看本次进入 Camera 拍摄的照片
    - 上传队列与预览分离
- [x] OCR 功能
- [x] 注册、忘记密码 直接跳转到浏览器
- [x] 重构目录选择器
- [x] 修改Camera 界面

