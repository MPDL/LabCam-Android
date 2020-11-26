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

TODO 
- 启动页
- 获取所有library，本地过滤没权限的不显示
- 缓存keeper 目录接口
- 修改上传逻辑
    - 预览只看本次进入 Camera 拍摄的照片
    - 上传队列与预览分离
- OCR 功能
