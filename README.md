### Need to add before running
labcam_config.gradle
```
ext{

    labcam_config = [
            "key-alias"      :   "***",
            "key-password"   :   "***",
            "store-password" :   "***",
            "bugly-app-id"   :   "\"This needs to be replaced with your own BuglyAppId\"",
            "debug-account"   :   "\"Test account\"",
            "debug-password"   :   "\"Test password\""
    ]
}
```