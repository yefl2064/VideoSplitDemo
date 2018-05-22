package com.yefl.videosplitdemo.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.luck.picture.lib.tools.ScreenUtils;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yefl.videosplitdemo.R;
import com.yefl.videosplitdemo.app.GlideApp;
import com.yefl.videosplitdemo.model.ThumbnailModel;
import com.yefl.videosplitdemo.utils.BitmapUtil;
import com.yefl.videosplitdemo.utils.ExtractVideoInfoUtil;
import com.yefl.videosplitdemo.utils.ThreadPoolManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.functions.Action1;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by yefl on 2018/5/22.
 */

public class VideoPreviewActivity extends BaseActivity {
    @BindView(R.id.rv_thumb)
    RecyclerView rvThumb;
    @BindView(R.id.video_view)
    VideoView mVideoView;
    @BindView(R.id.relativelayout)
    RelativeLayout relativelayout;
    @BindView(R.id.seek_bar)
    SeekBar seekBar;

    private BaseQuickAdapter adapter;
    private Context mContext;
    private List<ThumbnailModel> thumbnailModelList = new ArrayList<>();
    private String videoPath;
    private int itemWidth, itemHeight;
    private static int MAX_COUNT_RANGE = 10;
    ExtractVideoInfoUtil mExtractVideoInfoUtil;
    long duration;
    Bitmap mBitmap;
    private long mCurrentTime = 0;

    @Override
    protected int getContentView() {
        return R.layout.activity_video_preview;
    }

    @Override
    protected void initView() {
        super.initView();
        mContext = this;
        if (getIntent().hasExtra("videoPath")) {
            videoPath = getIntent().getStringExtra("videoPath");
        }
        itemWidth = ScreenUtils.getScreenWidth(mContext) - getResources().getDimensionPixelSize(R.dimen.dim_12) * 2;
        itemHeight = getResources().getDimensionPixelSize(R.dimen.dim_55);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                long progressTime = duration * seekBar.getProgress() / 100;
                mVideoView.seekTo((int) (progressTime));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long progressTime = duration * seekBar.getProgress() / 100;
                setTimeSeek(progressTime);
            }
        });
        new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                if (aBoolean) {
                    mExtractVideoInfoUtil = new ExtractVideoInfoUtil(videoPath);
                    duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
                    mVideoView.setVideoPath(videoPath);
                    mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mVideoView.seekTo(0);
                        }
                    });
                    initPreview();
                    ThreadPoolManager.getInstance().execute(new ExtractThread(MAX_COUNT_RANGE));
                } else {
                    finish();
                }

            }
        });
    }

    private void initPreview() {
        rvThumb.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new BaseQuickAdapter<ThumbnailModel, BaseViewHolder>(R.layout.item_edit_video, thumbnailModelList) {

            @Override
            protected void convert(BaseViewHolder helper, ThumbnailModel item) {
                ImageView imageView = helper.getView(R.id.thumbnail);
                LinearLayout.LayoutParams layoutparams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
                layoutparams.width = itemWidth / MAX_COUNT_RANGE;
                GlideApp.with(mContext).load(item.getBitmap()).centerCrop().into(imageView);
                if (itemHeight == 0) {
                    itemHeight = layoutparams.height;
                }
            }
        };
        rvThumb.setAdapter(adapter);
        relayoutSeekBar();
    }

    /**
     * 设置seekbar thumb高度
     */
    private void relayoutSeekBar() {
        ViewTreeObserver vto = rvThumb.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rvThumb.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                itemHeight = rvThumb.getHeight();
                setTimeSeek(0);
            }

        });
    }

    private void setTimeSeek(final long time) {
        mCurrentTime = time;
        mVideoView.seekTo((int) time);
        ThreadPoolManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                mBitmap = mExtractVideoInfoUtil.extractFrame(time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST, itemWidth / MAX_COUNT_RANGE, itemHeight);
                if (mBitmap != null) {
                    mBitmap = BitmapUtil.zoomAndCut(mBitmap, itemHeight + 20, itemHeight + 20);
                    final Drawable thumbDrawable = new BitmapDrawable(getResources(), mBitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (seekBar != null) {
                                seekBar.setThumb(thumbDrawable);
                            }
                        }
                    });
                }

            }
        });


    }

    private boolean interruptExtractRunnable = false; //true中断提取帧线程

    /**
     * 视频取帧
     */
    class ExtractThread implements Runnable {
        private int thumbnailsCount;

        public ExtractThread(int thumbnailsCount) {
            this.thumbnailsCount = thumbnailsCount;
        }

        @Override
        public void run() {
            for (int i = 0; i < thumbnailsCount; i++) {
                if (interruptExtractRunnable) {
                    return;
                }
                long time = i * duration / thumbnailsCount;
                if (mExtractVideoInfoUtil != null) {
                    ThumbnailModel thumbnailModel = new ThumbnailModel();
                    thumbnailModel.setTime(time);
                    Bitmap bitmap = mExtractVideoInfoUtil.extractFrame(time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST, itemWidth / MAX_COUNT_RANGE, itemHeight);
                    thumbnailModel.setBitmap(bitmap);
                    thumbnailModelList.add(i, thumbnailModel);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

            }
        }
    }


    @Override
    protected void onDestroy() {
        for (ThumbnailModel thumbnailModel : thumbnailModelList) {
            if (thumbnailModel != null && thumbnailModel.getBitmap() != null && !thumbnailModel.getBitmap().isRecycled()) {
                thumbnailModel.getBitmap().recycle();
                thumbnailModel.setBitmap(null);
                thumbnailModel = null;
            }
        }
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
        interruptExtractRunnable = true;
        mExtractVideoInfoUtil.release();
        super.onDestroy();
    }
}
