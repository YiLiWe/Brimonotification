package com.example.brimonotification.xposed.hook;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.example.brimonotification.xposed.callback.ActivityLifecycleCallbacks;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Data;

//获取Context，监听Activity
@Data
@SuppressLint("StaticFieldLeak")
public class ApplicationHook {
    private static ApplicationHook applicationHook;
    private final XC_LoadPackage.LoadPackageParam loadPackageParam;
    private Context context;

    public ApplicationHook(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        this.loadPackageParam = loadPackageParam;
        start();
    }

    public void start() {
        XposedHelpers.findAndHookMethod("android.app.Instrumentation"
                , loadPackageParam.classLoader, "callApplicationOnCreate"
                , Application.class
                , new XC_MethodHook(this::HooksContentMethod));
    }

    private void HooksContentMethod(int i, de.robv.android.xposed.XC_MethodHook.MethodHookParam methodHookParam) {
        if (i == 0) return;
        if (methodHookParam.thisObject instanceof Application application) {
            this.context = application.getApplicationContext();
            application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(this));
        }
    }

    public static ApplicationHook getInstance(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (applicationHook == null) {
            applicationHook = new ApplicationHook(loadPackageParam);
        }
        return applicationHook;
    }
}