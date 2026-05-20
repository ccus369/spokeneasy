package com.spokeneasy.app;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class SpokenEasyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SpeechUtility.createUtility(this,
                SpeechConstant.APPID + "=" + BuildConfig.XUNFEI_APPID);
    }
}
