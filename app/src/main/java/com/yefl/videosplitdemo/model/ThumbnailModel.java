package com.yefl.videosplitdemo.model;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by yefl on 2018/4/28.
 */

public class ThumbnailModel implements Serializable {
    private Bitmap bitmap;
    private long time;

    public ThumbnailModel() {

    }

    public ThumbnailModel(Bitmap bitmap, long time) {
        this.bitmap = bitmap;
        this.time = time;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object obj) {
        if (time == ((ThumbnailModel) obj).getTime()) {
            return true;
        } else {
            return false;
        }

    }
}
