package com.yefl.videosplitdemo.utils;

import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class ExtractVideoInfoUtil {
    private FFmpegMediaMetadataRetriever mMetadataRetriever;
    private long fileLength = 0;//毫秒

    public ExtractVideoInfoUtil(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("path must be not null !");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("path file not exists !");
        }
        mMetadataRetriever = new FFmpegMediaMetadataRetriever();
        mMetadataRetriever.setDataSource(file.getAbsolutePath());
        String len = getVideoLength();
        fileLength = TextUtils.isEmpty(len) ? 0 : Long.valueOf(len);
    }

    public int getVideoWidth() {
        String w;
        if (getVideoDegree() == 90) {
            w = mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        } else {
            w = mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        }
        int width = -1;
        if (!TextUtils.isEmpty(w)) {
            width = Integer.valueOf(w);
        }
        return width;
    }

    public int getVideoHeight() {
        String h;
        if (getVideoDegree() == 90) {
            h = mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        } else {
            h = mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        }
        int height = -1;
        if (!TextUtils.isEmpty(h)) {
            height = Integer.valueOf(h);
        }
        return height;
    }

    /**
     * 获取视频的典型的一帧图片，不耗时
     *
     * @return Bitmap
     */
    public Bitmap extractFrame() {
        return mMetadataRetriever.getFrameAtTime();
    }


    public Bitmap extractFrame(long timeMs, int option) {
        if (timeMs > fileLength) {
            timeMs = fileLength;
        }
        Bitmap bitmap;
        bitmap = mMetadataRetriever.getFrameAtTime(timeMs * 1000, option);
        if (bitmap != null && getVideoDegree() == 90) {
            bitmap = BitmapUtil.Tobitmap90(bitmap);
        }
        return bitmap;
    }

    public Bitmap extractFrame(long timeMs, int option, int width, int height) {
        if (timeMs > fileLength) {
            timeMs = fileLength;
        }
        Bitmap bitmap;
        bitmap = mMetadataRetriever.getScaledFrameAtTime(timeMs * 1000, option, width * 2, height * 2);
        if (bitmap != null && getVideoDegree() == 90) {
            bitmap = BitmapUtil.Tobitmap90(bitmap);
        }
        return bitmap;
    }


    /***
     * 获取视频的长度时间
     *
     * @return String 毫秒
     */
    public String getVideoLength() {
        return mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
    }

    /**
     * 获取视频旋转角度
     *
     * @return
     */
    public int getVideoDegree() {
        int degree = 0;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                String degreeStr = mMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (!TextUtils.isEmpty(degreeStr)) {
                    degree = Integer.valueOf(degreeStr);
                }
            }
            return degree;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void release() {
        if (mMetadataRetriever != null) {
            mMetadataRetriever.release();
        }
    }

}
