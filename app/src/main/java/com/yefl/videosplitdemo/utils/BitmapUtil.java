package com.yefl.videosplitdemo.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;


public class BitmapUtil {

    public static Bitmap Tobitmap90(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    /**
     * bitmap 等比缩放并截取中间部分到指定的比例
     *
     * @param bitmap
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomAndCut(Bitmap bitmap, int w, int h) {
        float scalex = (float) bitmap.getWidth() / w;
        float scaley = (float) bitmap.getHeight() / h;
        float scale = Math.min(scalex, scaley);
        int mw, mh;

        if (scalex > scaley) {
            mw = (int) (bitmap.getWidth() / scale);
            mh = h;
        } else {
            mw = w;
            mh = (int) (bitmap.getHeight() / scale);
        }
        bitmap = zoom(bitmap, mw, mh);
        if (bitmap.getWidth() > w) {//截取x
            bitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - w) / 2, 0, w, h);
        } else if (bitmap.getHeight() > h) {//截取y
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h);
        }
        return bitmap;
    }


    /**
     * 放大缩小图片
     *
     * @param bitmap 源Bitmap
     * @param w      宽
     * @param h      高
     * @return 目标Bitmap
     */
    public static Bitmap zoom(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                matrix, true);
        return newbmp;
    }

}
