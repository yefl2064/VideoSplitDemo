package com.yefl.videosplitdemo.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {

    Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(getContentView());
        unbinder = ButterKnife.bind(this);
        initTopBar();
        initView();
        initVar();
    }

    protected int getContentView() {
        return -1;
    }

    //标题栏
    protected void initTopBar() {

    }

    //布局
    protected void initView() {

    }

    //数据
    protected void initVar() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }


}