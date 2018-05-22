package com.yefl.videosplitdemo.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.lazylibrary.util.StringUtils;
import com.github.lazylibrary.util.ToastUtils;
import com.luck.picture.lib.tools.DateUtils;
import com.luck.picture.lib.tools.ScreenUtils;
import com.orhanobut.logger.Logger;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yefl.videosplitdemo.R;
import com.yefl.videosplitdemo.app.GlideApp;
import com.yefl.videosplitdemo.model.ThumbnailModel;
import com.yefl.videosplitdemo.utils.ExtractVideoInfoUtil;
import com.yefl.videosplitdemo.utils.ThreadPoolManager;
import com.yefl.videosplitdemo.utils.VideoClip;
import com.yefl.videosplitdemo.widget.RangeSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.functions.Action1;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by yefl on 2018/5/22.
 */

public class VideoSplitActivity extends BaseActivity {
    @BindView(R.id.video_view)
    VideoView mVideoView;
    @BindView(R.id.tv_start)
    TextView tvStart;
    @BindView(R.id.tv_end)
    TextView tvEnd;
    @BindView(R.id.rv_thumb)
    RecyclerView rvThumb;
    @BindView(R.id.position_icon)
    ImageView positionIcon;
    @BindView(R.id.id_seekBar_layout)
    LinearLayout seekBarLayout;
    @BindView(R.id.layout_bottom)
    RelativeLayout layoutBottom;

    private Context mContext;
    private static final long MIN_CUT_DURATION = 5 * 1000L;// 最小剪辑时间3s
    private static final long MAX_CUT_DURATION = 15 * 1000L;//视频最多剪切多长时间
    private static int MAX_COUNT_RANGE = 10;//seekBar的区域内一共有多少张图片
    private int mMaxWidth;
    private long duration;
    private float averageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private long leftProgress, rightProgress;
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private BaseQuickAdapter adapter;
    private List<ThumbnailModel> thumbnailModelList = new ArrayList<>();
    private boolean isOverScaledTouchSlop;
    private RangeSeekBar seekBar;
    private String path;
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private int rvWidth, itemHeight;
    RxPermissions rxPermissions;

    @Override
    protected int getContentView() {
        return R.layout.activity_video_split;
    }

    @Override
    protected void initView() {
        super.initView();
        mContext = this;
        rvWidth = ScreenUtils.getScreenWidth(mContext) - getResources().getDimensionPixelSize(R.dimen.dim_12) * 2;
        itemHeight = getResources().getDimensionPixelSize(R.dimen.dim_55);
        rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                if (aBoolean) {
                    initData();
                    initEditVideo();
                    initPlay();
                } else {
                    finish();
                }
            }
        });
        rvThumb.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        adapter = new BaseQuickAdapter<ThumbnailModel, BaseViewHolder>(R.layout.item_edit_video, thumbnailModelList) {

            @Override
            protected void convert(BaseViewHolder helper, ThumbnailModel item) {
                ImageView imageView = helper.getView(R.id.thumbnail);
                LinearLayout.LayoutParams layoutparams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
                layoutparams.width = rvWidth / MAX_COUNT_RANGE;
                GlideApp.with(mContext).load(item.getBitmap()).centerCrop().into(imageView);
            }
        };
        rvThumb.setAdapter(adapter);
        rvThumb.addOnScrollListener(mOnScrollListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.split_video, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                String outPath = mContext.getFilesDir() + "/clip_" + System.currentTimeMillis() + ".mp4";
                interruptExtractRunnable = true;
                if (!StringUtils.isBlank(path)) {
                    if (duration > MAX_CUT_DURATION) {
                        ToastUtils.showToast(mContext, "这里应该菊花在转转...正在裁剪...");
                        ThreadPoolManager.getInstance().execute(new VideoClipThread(path, leftProgress, rightProgress, outPath));
                    } else {
                        Intent intent = new Intent(mContext, VideoPreviewActivity.class);
                        intent.putExtra("videoPath", path);
                        startActivity(intent);
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void initData() {
        if (getIntent().hasExtra("filepath")) {
            path = getIntent().getStringExtra("filepath");
        }
        if (!new File(path).exists()) {
            ToastUtils.showToast(mContext, "视频文件不存在");
            finish();
        }
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(path);
        duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
        tvEnd.setText(DateUtils.timeParseMinute(duration));
        mMaxWidth = ScreenUtils.getScreenWidth(this) - ScreenUtils.dip2px(this, (int) getResources().getDimension(R.dimen.dim_5) * 2);
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }


    private void initEditVideo() {
        long startPosition = 0;
        long endPosition = duration;
        final int thumbnailsCount;
        int rangeWidth;
        boolean isOverTime;
        if (endPosition <= MAX_CUT_DURATION) {
            isOverTime = false;
            thumbnailsCount = MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
        } else {
            isOverTime = true;
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f) * MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MAX_COUNT_RANGE * thumbnailsCount;
        }
        ThreadPoolManager.getInstance().execute(new ExtractThread(thumbnailsCount));
        //init seekBar
        if (isOverTime) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);
        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }
        seekBar.setMin_cut_time(MIN_CUT_DURATION);//设置最小裁剪时间
        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        seekBarLayout.addView(seekBar);
        averageMsPx = duration * 1.0f / rangeWidth * 1.0f;

        //init pos icon start
        leftProgress = 0;
        if (isOverTime) {
            rightProgress = MAX_CUT_DURATION;
        } else {
            rightProgress = endPosition;
        }
        averagePxMs = (mMaxWidth * 1.0f / (rightProgress - leftProgress));
    }

    private void initPlay() {
        mVideoView.setVideoPath(path);
        //设置videoview的OnPrepared监听
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //设置MediaPlayer的OnSeekComplete监听
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        if (!isSeeking) {
                            videoStart();
                        }
                    }
                });
            }
        });
        //first
        videoStart();
    }


    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isSeeking = false;
                videoStart();
            } else {
                isSeeking = true;
                if (isOverScaledTouchSlop && mVideoView != null && mVideoView.isPlaying()) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            //初始状态,why ? 因为默认的时候有5dp的空白！
            if (scrollX == -ScreenUtils.dip2px(mContext, (int) getResources().getDimension(R.dimen.dim_5))) {
                scrollPos = 0;
            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                if (mVideoView != null && mVideoView.isPlaying()) {
                    videoPause();
                }
                isSeeking = true;
                scrollPos = (long) (averageMsPx * (ScreenUtils.dip2px(mContext, (int) getResources().getDimension(R.dimen.dim_5)) + scrollX));
                leftProgress = seekBar.getSelectedMinValue() + scrollPos;
                rightProgress = seekBar.getSelectedMaxValue() + scrollPos;
                Log.d("test", "-------leftProgress:>>>>>" + leftProgress);
                Log.d("test", "-------rightProgress:>>>>>" + rightProgress);
                mVideoView.seekTo((int) leftProgress);

                tvStart.setText(DateUtils.timeParseMinute(leftProgress));
                tvEnd.setText(DateUtils.timeParseMinute(rightProgress));

            }
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     *
     * @return int px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvThumb.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private ValueAnimator animator;

    private void anim() {
        if (positionIcon.getVisibility() == View.GONE) {
            positionIcon.setVisibility(View.VISIBLE);
        }
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) positionIcon.getLayoutParams();
        int start = (int) (ScreenUtils.dip2px(this, (int) getResources().getDimension(R.dimen.dim_5)) + (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos) * averagePxMs);
        int end = (int) (ScreenUtils.dip2px(this, (int) getResources().getDimension(R.dimen.dim_5)) + (rightProgress - scrollPos) * averagePxMs);
        animator = ValueAnimator
                .ofInt(start, end)
                .setDuration((rightProgress - scrollPos) - (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                positionIcon.setLayoutParams(params);
            }
        });
        animator.start();
    }

    private RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {
            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    mVideoView.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ?
                            leftProgress : rightProgress));
                    tvStart.setText(DateUtils.timeParseMinute(leftProgress));
                    tvEnd.setText(DateUtils.timeParseMinute(rightProgress));
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    //从minValue开始播
                    mVideoView.seekTo((int) leftProgress);
//                    videoStart();
                    tvStart.setText(DateUtils.timeParseMinute(leftProgress));
                    tvEnd.setText(DateUtils.timeParseMinute(rightProgress));
                    break;
                default:
                    break;
            }
        }
    };


    private void videoStart() {
        mVideoView.start();
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
        handler.removeCallbacks(run);
        handler.post(run);
    }

    private void videoProgressUpdate() {
        long currentPosition = mVideoView.getCurrentPosition();
        if (currentPosition >= (rightProgress)) {
            mVideoView.seekTo((int) leftProgress);
            positionIcon.clearAnimation();
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            anim();
        }
    }

    private void videoPause() {
        isSeeking = false;
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
            handler.removeCallbacks(run);
        }
        if (positionIcon.getVisibility() == View.VISIBLE) {
            positionIcon.setVisibility(View.GONE);
        }
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.seekTo((int) leftProgress);
//            videoStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            videoPause();
        }
    }

    private Handler handler = new Handler();
    private Runnable run = new Runnable() {

        @Override
        public void run() {
            videoProgressUpdate();
            handler.postDelayed(run, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        for (ThumbnailModel thumbnailModel : thumbnailModelList) {
            if (thumbnailModel != null && thumbnailModel.getBitmap() != null && !thumbnailModel.getBitmap().isRecycled()) {
                thumbnailModel.getBitmap().recycle();
                thumbnailModel.setBitmap(null);
                thumbnailModel = null;
            }
        }
        if (animator != null) {
            animator.cancel();
        }
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
        interruptExtractRunnable = true;
        rvThumb.removeOnScrollListener(mOnScrollListener);
        handler.removeCallbacksAndMessages(null);
        mExtractVideoInfoUtil.release();
        super.onDestroy();
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
                    Bitmap bitmap = mExtractVideoInfoUtil.extractFrame(time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC | FFmpegMediaMetadataRetriever.OPTION_CLOSEST, rvWidth / MAX_COUNT_RANGE, itemHeight);
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


    /**
     * 视频裁剪
     */
    class VideoClipThread implements Runnable {
        private long starttime, endtime;
        private String sourcePath, outPath;

        public VideoClipThread(String sourcePath, long starttime, long endtime, String outPath) {
            this.starttime = starttime;
            this.endtime = endtime;
            this.sourcePath = sourcePath;
            this.outPath = outPath;
        }

        @Override
        public void run() {
            long t1 = System.currentTimeMillis();
            VideoClip videoClip = new VideoClip(sourcePath, outPath, starttime, endtime);
            videoClip.clip();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(outPath))));
            Logger.e("clip time--->" + (System.currentTimeMillis() - t1));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(mContext, VideoPreviewActivity.class);
                    intent.putExtra("videoPath", outPath);
                    startActivity(intent);
                }
            });

        }
    }
}
