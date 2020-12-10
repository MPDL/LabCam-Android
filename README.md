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

