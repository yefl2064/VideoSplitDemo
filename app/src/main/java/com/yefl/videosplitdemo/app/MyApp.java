package com.yefl.videosplitdemo.app;

import android.app.Activity;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.util.LinkedList;
import java.util.List;


public class MyApp extends MultiDexApplication {
    public static Context context;
    public static boolean isDebug = false;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        instance = this;
        activitys = new LinkedList<Activity>();
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)  //（可选）是否显示线程信息。 默认值为true
                .methodCount(2)         // （可选）要显示的方法行数。 默认2
                .methodOffset(7)        // （可选）隐藏内部方法调用到偏移量。 默认5
                .tag("test")
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
    }

    protected static MyApp instance;
    private List<Activity> activitys;

    public static MyApp getInstance() {
        return instance;
    }


    public void AddActivity(Activity activity) {
        activitys.add(activity);
    }

    public void RemoveActivity(Activity activity) {
        if (activitys.contains(activity))
            activitys.remove(activity);
    }

    public void CloseAllActivity() {
        for (int i = 0; i < activitys.size(); i++) {
            if (activitys.get(i) != null
                    && !activitys.get(i).isFinishing()) {
                activitys.get(i).finish();
            }
            activitys.clear();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
