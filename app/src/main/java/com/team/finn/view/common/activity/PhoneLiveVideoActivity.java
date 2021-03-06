package com.team.finn.view.common.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bigkoo.svprogresshud.SVProgressHUD;
import com.facebook.drawee.view.SimpleDraweeView;
import com.team.finn.R;
import com.team.finn.base.BaseActivity;
import com.team.finn.base.BaseView;
import com.team.finn.danmu.utils.DanmuProcess;
import com.team.finn.model.logic.common.CommonPhoneLiveVideoModelLogic;
import com.team.finn.model.logic.common.bean.TempLiveVideoInfo;
import com.team.finn.model.logic.home.bean.HomeRecommendHotCate;
import com.team.finn.presenter.common.impl.CommonPhoneLiveVideoPresenterImp;
import com.team.finn.presenter.common.interfaces.CommonPhoneLiveVideoContract;
import com.team.finn.ui.DivergeView;
import com.team.finn.ui.loadplay.LoadingView;
import com.team.finn.view.common.fragment.MainDialogFragment;

import java.util.ArrayList;

import butterknife.BindView;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.utils.ScreenResolution;
import io.vov.vitamio.widget.VideoView;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * 作者：finn
 * 版本号：1.0
 * 备注消息：
 **/
public class PhoneLiveVideoActivity extends BaseActivity<CommonPhoneLiveVideoModelLogic, CommonPhoneLiveVideoPresenterImp> implements CommonPhoneLiveVideoContract.View, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {

    @BindView(R.id.vm_videoview)
    VideoView vmVideoview;
    @BindView(R.id.im_logo)
    ImageView imLogo;
    @BindView(R.id.lv_playloading)
    LoadingView lvPlayloading;
    @BindView(R.id.fl_loading)
    FrameLayout flLoading;
    @BindView(R.id.iv_control_img)
    ImageView ivControlImg;
    @BindView(R.id.tv_control_name)
    TextView tvControlName;
    @BindView(R.id.tv_control)
    TextView tvControl;
    @BindView(R.id.control_center)
    RelativeLayout controlCenter;
    @BindView(R.id.tv_loading_buffer)
    TextView tvLoadingBuffer;
    @BindView(R.id.danmakuView)
    DanmakuView danmakuView;
    @BindView(R.id.img_loading)
    SimpleDraweeView imgLoading;
    @BindView(R.id.divergeView)
    DivergeView divergeView;
    private HomeRecommendHotCate.RoomListEntity mRoomEntity;
    private TempLiveVideoInfo videoInfo;
    private String Room_id;
    private SVProgressHUD svProgressHUD;
    private String imgUrl;
    //    点赞 动画
    private ArrayList<Bitmap> mList;
    private int mIndex = 0;

    /**
     * 弹幕
     */
    private DanmuProcess mDanmuProcess;
    /**
     * 音量   亮度
     *
     * @return
     */
    private int mScreenWidth = 0;//屏幕宽度
    private boolean mIsFullScreen = true;//是否为全屏
    private int mShowVolume;//声音
    private int mShowLightness;//亮度
    private int mMaxVolume;//最大声音
    private AudioManager mAudioManager;
    private GestureDetector mGestureDetector;
    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener;
    /**
     * 声音
     */
    public final static int ADD_FLAG = 1;
    /**
     * 亮度
     */
    public final static int SUB_FLAG = -1;

    public static final int HIDE_CONTROL_BAR = 0x02;//隐藏控制条
    public static final int HIDE_TIME = 5000;//隐藏控制条时间
    public static final int SHOW_CENTER_CONTROL = 0x03;//显示中间控制
    public static final int SHOW_CONTROL_TIME = 1000;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /**
                 *  隐藏top ,bottom
                 */
                case HIDE_CONTROL_BAR:

                    break;
                /**
                 *  隐藏center控件
                 */
                case SHOW_CENTER_CONTROL:
                    if (controlCenter != null) {
                        controlCenter.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    };


    @Override
    protected int getLayoutId() {
//        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Vitamio.isInitialized(this);
        return R.layout.activity_phonelive_video;
    }

    @Override
    protected void onInitView(Bundle bundle) {
        Room_id = getIntent().getExtras().getString("Room_id");
        imgUrl = getIntent().getExtras().getString("Img_Path");
        if (imgUrl != null) {
            imgLoading.setImageURI(Uri.parse(imgUrl));
        }
        // 保持 屏幕常亮
        vmVideoview.setKeepScreenOn(true);
        mPresenter.getPresenterPhoneLiveVideoInfo(Room_id);
        svProgressHUD = new SVProgressHUD(this);
        //获取屏幕宽度
        Pair<Integer, Integer> screenPair = ScreenResolution.getResolution(this);
        mScreenWidth = screenPair.first;
//   初始化声音和亮度
        initVolumeWithLight();
        vmVideoview.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, 0);
        initDanMu(Room_id);
        initDrawView();
        show();
        new MainDialogFragment().show(getSupportFragmentManager(), "MainDialogFragment");
    }

    private void initDanMu(String room_id) {
        mDanmuProcess = new DanmuProcess(this, danmakuView, Integer.valueOf(room_id));
        mDanmuProcess.start();
    }

    private void hide() {
        vmVideoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void show() {
        vmVideoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }


    public void initDrawView() {
        /**
         *  点赞
         */
        mList = new ArrayList<>();
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon1, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon2, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon3, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon4, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon5, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon6, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon7, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon8, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon9, null)).getBitmap());
        mList.add(((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prise_icon10, null)).getBitmap());
        divergeView.post(new Runnable() {
            @Override
            public void run() {
                divergeView.setEndPoint(new PointF(divergeView.getMeasuredWidth() / 2, 0));
                divergeView.setDivergeViewProvider(new Provider());
            }
        });
        /**
         *  启动动画
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (divergeView != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mIndex == 9) {
                                    mIndex = 0;
                                }
                                if (divergeView != null) {
                                    divergeView.startDiverges(mIndex);
                                    mIndex++;
                                }
                            }
                        });
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }
            }
        }).start();

    }

    /**
     * 巴塞尔曲线
     */
    class Provider implements DivergeView.DivergeViewProvider {

        @Override
        public Bitmap getBitmap(Object obj) {
            return mList == null ? null : mList.get((int) obj);
        }
    }

    /**
     * 初始化声音和亮度
     */
    private void initVolumeWithLight() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mShowVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / mMaxVolume;
        mShowLightness = getScreenBrightness();
    }

    /**
     * 获得当前屏幕亮度值 0--255
     */
    private int getScreenBrightness() {
        int screenBrightness = 255;
        try {
            screenBrightness = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenBrightness;
    }

    @Override
    protected void onEvent() {
        //        添加手势监听
        addTouchListener();
//        视频播放监听
        vmVideoview.setOnInfoListener(this);
        vmVideoview.setOnBufferingUpdateListener(this);
        vmVideoview.setOnErrorListener(this);
    }

    /**
     * 触摸事件进行监听
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    protected BaseView getView() {
        return this;
    }

    @Override
    public void getViewPhoneLiveVideoInfo(TempLiveVideoInfo mLiveVideoInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                videoInfo = mLiveVideoInfo;
                getViewInfo(mLiveVideoInfo);
            }
        });
    }

    private void getViewInfo(TempLiveVideoInfo mLiveVideoInfo) {
        if (mLiveVideoInfo.getData() != null) {
            String url = mLiveVideoInfo.getData().getHls_url();
            Uri uri = Uri.parse(url);
            if (vmVideoview != null) {
                vmVideoview.setVideoURI(uri);
                vmVideoview.setBufferSize(1024 * 1024 * 20);
                /**
                 * 设置视频质量。参数quality参见MediaPlayer的常量：
                 * VIDEOQUALITY_LOW（流畅）、VIDEOQUALITY_MEDIUM（普通）、VIDEOQUALITY_HIGH（高质）。
                 */
                vmVideoview.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
                vmVideoview.requestFocus();
                vmVideoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        // optional need Vitamio 4.0
                        mediaPlayer.setPlaybackSpeed(1.0f);
                        flLoading.setVisibility(View.GONE);
//                    vmVideoview.setBackgroundResource(0);
                        mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_BAR, HIDE_TIME);
                    }
                });
            }
        }
    }

    @Override
    public void showErrorWithStatus(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                svProgressHUD.showErrorWithStatus("主播还在赶来的路上~~");
            }
        });
    }

    /**
     * 添加手势操作
     */
    private void addTouchListener() {
        mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            //滑动操作
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                if (!mIsFullScreen)//非全屏不进行手势操作
                    return false;

                float x1 = e1.getX();

                float absDistanceX = Math.abs(distanceX);// distanceX < 0 从左到右
                float absDistanceY = Math.abs(distanceY);// distanceY < 0 从上到下

                // Y方向的距离比X方向的大，即 上下 滑动
                if (absDistanceX < absDistanceY) {
                    if (distanceY > 0) {//向上滑动
                        if (x1 >= mScreenWidth * 0.65) {//右边调节声音
                            changeVolume(ADD_FLAG);
                        } else {//调节亮度
                            changeLightness(ADD_FLAG);
                        }
                    } else {//向下滑动
                        if (x1 >= mScreenWidth * 0.65) {
                            changeVolume(SUB_FLAG);
                        } else {
                            changeLightness(SUB_FLAG);
                        }
                    }
                } else {
                    // X方向的距离比Y方向的大，即 左右 滑动

                }
                return false;
            }

            //双击事件，有的视频播放器支持双击播放暂停，可从这实现
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return super.onDoubleTap(e);
            }

            //单击事件
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {

                return true;
            }
        };
        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);
    }

    /**
     * 改变声音
     */
    private void changeVolume(int flag) {
        mShowVolume += flag;
        if (mShowVolume > 100) {
            mShowVolume = 100;
        } else if (mShowVolume < 0) {
            mShowVolume = 0;
        }
        tvControlName.setText("音量");
        ivControlImg.setImageResource(R.drawable.img_volume);
        tvControl.setText(mShowVolume + "%");
        int tagVolume = mShowVolume * mMaxVolume / 100;
        //tagVolume:音量绝对值
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, tagVolume, 0);
        mHandler.removeMessages(SHOW_CENTER_CONTROL);
        controlCenter.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessageDelayed(SHOW_CENTER_CONTROL, SHOW_CONTROL_TIME);
    }

    /**
     * 改变亮度
     */
    private void changeLightness(int flag) {
        mShowLightness += flag;
        if (mShowLightness > 255) {
            mShowLightness = 255;
        } else if (mShowLightness <= 0) {
            mShowLightness = 0;
        }
        tvControlName.setText("亮度");
        ivControlImg.setImageResource(R.drawable.img_light);
        tvControl.setText(mShowLightness * 100 / 255 + "%");
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = mShowLightness / 255f;
        getWindow().setAttributes(lp);

        mHandler.removeMessages(SHOW_CENTER_CONTROL);
        controlCenter.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessageDelayed(SHOW_CENTER_CONTROL, SHOW_CONTROL_TIME);
    }

    /**
     * 正在缓冲....
     *
     * @param mp      the MediaPlayer the update pertains to
     * @param percent the percentage (0-100) of the buffer that has been filled thus
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (flLoading != null) {
            flLoading.setVisibility(View.VISIBLE);
        }
        if (vmVideoview != null) {
            if (vmVideoview.isPlaying())
                vmVideoview.pause();
            if (tvLoadingBuffer != null) {
                tvLoadingBuffer.setText("直播已缓冲" + percent + "%...");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mPresenter.getPresenterPhoneLiveVideoInfo(Room_id);
        if (vmVideoview != null) {
            vmVideoview.start();
        }
        if (danmakuView != null && mDanmuProcess != null) {
            danmakuView.restart();
            mDanmuProcess.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vmVideoview != null) {
            vmVideoview.pause();
        }
        if (danmakuView != null) {
            danmakuView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (vmVideoview != null) {
            //        释放资源
            vmVideoview.stopPlayback();
        }
        /**
         *  销毁点赞动画
         */
        if (divergeView != null) {
            divergeView.stop();
            divergeView = null;
        }
        mDanmuProcess.finish();
        mDanmuProcess.close();
        if (danmakuView != null) {
            danmakuView.release();
            danmakuView.clear();
        }
        super.onDestroy();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (vmVideoview.isPlaying()) {
                    vmVideoview.pause();
                }
                mHandler.removeMessages(HIDE_CONTROL_BAR);
                break;
//            完成缓冲
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                flLoading.setVisibility(View.GONE);
                if (!vmVideoview.isPlaying())
                    vmVideoview.start();
                mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_BAR, HIDE_TIME);
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:

                break;
        }
        return true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            svProgressHUD.showErrorWithStatus("主播还在赶来的路上~~");
        }
        return false;
    }

}
