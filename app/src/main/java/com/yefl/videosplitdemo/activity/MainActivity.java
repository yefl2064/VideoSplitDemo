package com.yefl.videosplitdemo.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yefl.videosplitdemo.R;

import java.util.List;

import butterknife.OnClick;
import rx.functions.Action1;

public class MainActivity extends BaseActivity {

    RxPermissions rxPermissions;
    Context mContext;

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        super.initView();
        mContext = this;
        rxPermissions = new RxPermissions(this);
    }

    @OnClick(R.id.btn_select)
    public void onViewClicked() {
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                if (aBoolean) {
                    selectVideo();
                }
            }
        });
    }


    private void selectVideo() {
        PictureSelector.create(MainActivity.this)
                .openGallery(PictureMimeType.ofVideo())
                .selectionMode(PictureConfig.SINGLE)
                .previewImage(false)
                .previewEggs(false)
                .isCamera(true)
                .compress(true)
                .enableCrop(false)
                .imageSpanCount(4)
                .glideOverride(160, 160)
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    List<LocalMedia> picList = PictureSelector.obtainMultipleResult(data);
                    if (picList.size() > 0) {
                        LocalMedia media = picList.get(0);
                        Intent intent = new Intent(mContext, VideoSplitActivity.class);
                        intent.putExtra("filepath", media.getPath());
                        startActivity(intent);
                    }
                    break;
            }
        }
    }
}
