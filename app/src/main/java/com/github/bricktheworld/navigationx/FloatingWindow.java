package com.github.bricktheworld.navigationx;

import android.accessibilityservice.AccessibilityService;   
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.rvalerio.fgchecker.AppChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;
import io.alterac.blurkit.BlurLayout;

import com.eightbitlab.supportrenderscriptblur.SupportRenderScriptBlur;



public class FloatingWindow extends AccessibilityService {

    private WindowManager wm;
    private LinearLayout ll;
    private ImageView animImage = null, backgroundImage = null, lastAppImage = null, forwardAppImage = null, wallpaperImage = null;
    private String lastAppName, iconsDir, wallpaperDir, iconsUnprocessedDir;
    private Boolean sortAble = true;
    private int height, width, density, offset, index = 0;
    private float radius = 20;
    private FrameLayout background_layout, foreground_layout;
    private View backgroundView, foregroundView;
    private BlurView blurView;
    private double percentageDifference;
    private Bitmap background, wallpaper, icons, icons_unprocessed, iconsUnprocessed;
    public MediaProjection mProjection;
    private Handler mHandler;
    public ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;
    private AppChecker appChecker = new AppChecker();
    Context context;
    private AccessibilityService accessibilityService;
    private List<AppView> appViewList = new ArrayList<>();
    final Handler handler = new Handler();
    final Point windowSize = new Point();
    static WindowManager.LayoutParams backgroundParameters = new WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
            , PixelFormat.TRANSPARENT);


    private static final String TAG = "CustomAccessibility";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }
    private static FloatingWindow sSharedInstance;

    public boolean onUnbind(Intent intent) {
        sSharedInstance = null;
        return false;
    }

    public static FloatingWindow getSharedInstance() {
        return sSharedInstance;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onServiceConnected() {
        super.onCreate();
        sSharedInstance = this;
        accessibilityService = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        ll = new LinearLayout(this);
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        height = metrics.heightPixels;
        width = metrics.widthPixels;
        density = metrics.densityDpi;
        wm.getDefaultDisplay().getRealSize(windowSize);
        offset = height/2 + 100;
        //inflate the layout_background.xml layout
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        backgroundView = inflater.inflate(R.layout.layout_background, null);
        foregroundView = inflater.inflate(R.layout.layout_foreground, null);
        background_layout = backgroundView.findViewById(R.id.backgroundLayout);
        foreground_layout = foregroundView.findViewById(R.id.foregroundLayout);
        blurView = backgroundView.findViewById(R.id.blurView);



        lastAppImage = foregroundView.findViewById(R.id.lastAppImage);
        forwardAppImage = foregroundView.findViewById(R.id.forwardAppImage);
        backgroundImage = backgroundView.findViewById(R.id.icons);
        wallpaperImage = backgroundView.findViewById(R.id.wallpaper);
        animImage = foregroundView.findViewById(R.id.animImage);
        context = this;
        LinearLayout.LayoutParams llParameters = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT);
        ll.setBackgroundColor(Color.argb(0, 0, 0 , 0));
        ll.setLayoutParams(llParameters);

//
//        wallpaperImage.setLayoutParams(llParameters);
//        backgroundImage.setLayoutParams(llParameters);
//        animImage.setLayoutParams(llParameters);
//        lastAppImage.setLayoutParams(llParameters);
        lastAppImage.setImageAlpha(0);
//        forwardAppImage.setLayoutParams(llParameters);
        forwardAppImage.setImageAlpha(0);


        final WindowManager.LayoutParams parameters = new WindowManager.LayoutParams(
                                            400,
                                            100,
                                            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        parameters.x = 0;
        parameters.y = offset;
        parameters.gravity = Gravity.CENTER | Gravity.CENTER;

        final WindowManager.LayoutParams imageOverlayParameters = new WindowManager.LayoutParams(
                windowSize.x,
                windowSize.y,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                , PixelFormat.TRANSPARENT);
        imageOverlayParameters.x = 0;
        imageOverlayParameters.y = 0;
        imageOverlayParameters.gravity = Gravity.START | Gravity.TOP;

        backgroundParameters.x = 0;
        backgroundParameters.y = 0;
        backgroundParameters.height = windowSize.y;
        backgroundParameters.width = windowSize.x;
        backgroundParameters.gravity = Gravity.CENTER | Gravity.CENTER;
        backgroundImage.requestLayout();
        backgroundImage.setY(0);
        backgroundImage.setImageAlpha(0);
        wallpaperImage.requestLayout();
        wallpaperImage.setY(0);
        wallpaperImage.setImageAlpha(0);
//        wm.addView(wallpaperImage, backgroundParameters);
//        wm.addView(backgroundImage, backgroundParameters);
//        blurView.setupWith(background_layout)
//                .setBlurAlgorithm(new SupportRenderScriptBlur(this))
//                .setBlurRadius(20)
//                .setHasFixedTransformationMatrix(true);
        wm.addView(background_layout, backgroundParameters);
        wm.addView(foreground_layout, imageOverlayParameters);

//        wm.addView(animImage, imageOverlayParameters);
//        wm.addView(lastAppImage, imageOverlayParameters);
//        wm.addView(forwardAppImage, imageOverlayParameters);
//        appChecker.whenAny(new AppChecker.Listener() {
//            @Override
//            public void onForeground(String packageName) {
//                lastAppName = packageName
//
//            }
//        }).timeout(500).start(this);
        wm.addView(ll, parameters);

        blurView.setupWith(background_layout)
                .setBlurAlgorithm(new SupportRenderScriptBlur(this))
                .setBlurRadius(radius)
                .setHasFixedTransformationMatrix(true);
        blurView.setAlpha(0);
//        blurView.startBlur();

        ll.setOnTouchListener(new View.OnTouchListener() {
            private WindowManager.LayoutParams updateParameters = parameters;
            private WindowManager.LayoutParams updateImageOverlayParameters = imageOverlayParameters;
            int y, _width, posY;
            float touchedY, touchedX;
            Intent intent = new Intent("android.intent.action.MAIN");
            @Override
            public boolean onTouch(View arg0, MotionEvent event){
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        radius = 20;
                        blurView.setBlurRadius(radius);
                        blurView.setBlurEnabled(true);
                        blurView.setAlpha(1);
                        intent.addCategory("android.intent.category.HOME");
                        lastAppName = appChecker.getForegroundApp(context);
                        touchedY = event.getRawY();
                        touchedX = event.getRawX();
                        y = updateParameters.y;
                        _width = updateParameters.width;
                        Bitmap screenshotUnrounded = takeScreenshot();
                        Bitmap screenshot = getRoundedCornerBitmap(screenshotUnrounded,70);
//                        bitmapComapringAsyncTask comparingTask = new bitmapComapringAsyncTask();
//                        bitmapComapringAsyncTask.execute(screenshotUnrounded, background);
                        if(iconsUnprocessed != null && lastAppName.equals(getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName)) {
                            percentageDifference = getDifferencePercent(iconsUnprocessed, screenshotUnrounded);
                        } else {
                            percentageDifference = -1;
                        }
                        Log.d("Percentage", Double.toString(percentageDifference));
                        if(!lastAppName.equals(getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName) || percentageDifference > 0.4) {
                            animImage.setImageAlpha(255);
                            backgroundImage.setImageAlpha(255);
                            wallpaperImage.setImageAlpha(255);
                        } else {
                            index = appViewList.size();
                            animImage.setImageAlpha(0);
                            backgroundImage.setImageAlpha(0);
                            wallpaperImage.setImageAlpha(0);
                            blurView.setAlpha(0);
                        }
                        lastAppImage.setImageAlpha(255);
                        forwardAppImage.setImageAlpha(255);
                        AppView _appView = new AppView();
                        _appView.setPackageName(lastAppName);
                        _appView.setSplash(screenshot);
                        if(sortAble) {
                            for (int i = 0; i < appViewList.size(); i++) {
                                if(lastAppName.equals(appViewList.get(i).getPackageName())) {
                                    appViewList.remove(i);
                                }
                            }
                            if(!lastAppName.equals(getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName)) {
                                appViewList.add(_appView);
                                index = appViewList.size() - 1;
                            } else {
                                index = appViewList.size();
                            }
                        } else {
//                            for (int i = 0; i < appViewList.size(); i++) {
//                                if(lastAppName.equals(appViewList.get(i).getPackageName())) {
////                                    appViewList.get(i).setPackageName(_appView.getPackageName());
//                                    appViewList.get(i).setSplash(_appView.getSplash());
//                                }
//                            }
                            if(index < appViewList.size())appViewList.get(index).setSplash(_appView.getSplash());
                        }
                        animImage.setImageBitmap(screenshot);
                        if(index - 1 >= 0) {
                            lastAppImage.setImageAlpha(255);
                            lastAppImage.setImageBitmap(appViewList.get(index - 1).getSplash());
                        } else {
                            lastAppImage.setImageAlpha(0);
                        }
                        if(index + 1 < appViewList.size()) {
                            forwardAppImage.setImageAlpha(255);
                            forwardAppImage.setImageBitmap(appViewList.get(index + 1).getSplash());
                        } else {
                            forwardAppImage.setImageAlpha(0);
                        }
                        updateImageOverlayParameters.height = windowSize.y;
                        updateImageOverlayParameters.width = windowSize.x;
                        backgroundImage.animate().scaleX(0.8f).setDuration(0).start();
                        backgroundImage.animate().scaleY(0.8f).setDuration(0).start();
                        wallpaperImage.animate().scaleX(1.2f).setDuration(0).start();
                        wallpaperImage.animate().scaleY(1.2f).setDuration(0).start();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(!lastAppName.equals(getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName) || percentageDifference > 0.4) animImage.setImageAlpha(255);
                        if(index - 1 >= 0) lastAppImage.setImageAlpha(255);
                        if(index + 1 < appViewList.size()) forwardAppImage.setImageAlpha(255);
                        posY = (int) (y + (event.getRawY() - touchedY));

                        float y = windowSize.y + (event.getRawY() - touchedY) * 1.5f;
                        float mapY = (y-0)/(2280-0) * (1-0) + 0;
                        ObjectAnimator lastAppImageX = ObjectAnimator.ofFloat(lastAppImage, "X", -windowSize.x + (event.getRawX() - touchedX)).setDuration(0);
                        ObjectAnimator lastAppImageY = ObjectAnimator.ofFloat(lastAppImage, "Y", 0.25f *  (event.getRawY() - touchedY)).setDuration(0);
                        ObjectAnimator forwardAppImageX = ObjectAnimator.ofFloat(forwardAppImage, "X", windowSize.x + (event.getRawX() - touchedX)).setDuration(0);
                        ObjectAnimator forwardAppImageY = ObjectAnimator.ofFloat(forwardAppImage, "Y", 0.25f *  (event.getRawY() - touchedY)).setDuration(0);
                        ObjectAnimator animImageX = ObjectAnimator.ofFloat(animImage, "X", (event.getRawX() - touchedX)).setDuration(0);
                        ObjectAnimator animImageY = ObjectAnimator.ofFloat(animImage, "Y", 0.25f *  (event.getRawY() - touchedY)).setDuration(0);
                        ObjectAnimator lastAppImageScaleX = ObjectAnimator.ofFloat(lastAppImage, "scaleX", mapY).setDuration(0);
                        ObjectAnimator lastAppImageScaleY = ObjectAnimator.ofFloat(lastAppImage, "scaleY", mapY).setDuration(0);
                        ObjectAnimator forwardAppImageScaleX = ObjectAnimator.ofFloat(forwardAppImage, "scaleX", mapY).setDuration(0);
                        ObjectAnimator forwardAppImageScaleY = ObjectAnimator.ofFloat(forwardAppImage, "scaleY", mapY).setDuration(0);
                        ObjectAnimator animImageScaleX = ObjectAnimator.ofFloat(animImage, "scaleX", mapY).setDuration(0);
                        ObjectAnimator animImageScaleY = ObjectAnimator.ofFloat(animImage, "scaleY", mapY).setDuration(0);
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.play(lastAppImageX)
                                .with(lastAppImageY)
                                .with(forwardAppImageX)
                                .with(forwardAppImageY)
                                .with(animImageX)
                                .with(animImageY)
                                .with(lastAppImageScaleX)
                                .with(lastAppImageScaleY)
                                .with(forwardAppImageScaleX)
                                .with(forwardAppImageScaleY)
                                .with(lastAppImageScaleY)
                                .with(animImageScaleX)
                                .with(animImageScaleY);
                        animatorSet.start();
                        return true;
                    //region Release
                    case MotionEvent.ACTION_UP:
                        //Main animation for going home
                        animImage.requestLayout();
                        backgroundImage.requestLayout();
                        wallpaperImage.requestLayout();
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(animImage, "scaleY", 0f);
                        scaleY.setInterpolator(new LinearOutSlowInInterpolator());
                        scaleY.setDuration(350);
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(animImage, "scaleX", 0f);
                        scaleX.setInterpolator(new LinearOutSlowInInterpolator());
                        scaleX.setDuration(400);
                        ObjectAnimator translateY = ObjectAnimator.ofFloat(animImage, "Y", 0);
                        translateY.setInterpolator(new LinearOutSlowInInterpolator());
                        translateY.setDuration(300);
                        ObjectAnimator translateX = ObjectAnimator.ofFloat(animImage, "X", 0f);
                        translateX.setInterpolator(new LinearOutSlowInInterpolator());
                        translateX.setDuration(300);
                        ObjectAnimator backgroundScaleY = ObjectAnimator.ofFloat(backgroundImage, "scaleY", 1f);
                        backgroundScaleY.setInterpolator(new LinearOutSlowInInterpolator());
                        backgroundScaleY.setDuration(500);
                        ObjectAnimator backgroundScaleX = ObjectAnimator.ofFloat(backgroundImage, "scaleX", 1f);
                        backgroundScaleX.setInterpolator(new LinearOutSlowInInterpolator());
                        backgroundScaleX.setDuration(500);
                        ObjectAnimator wallpaperScaleY = ObjectAnimator.ofFloat(wallpaperImage, "scaleY", 1f);
                        wallpaperScaleY.setInterpolator(new DecelerateInterpolator());
                        wallpaperScaleY.setDuration(500);
                        ObjectAnimator wallpaperScaleX = ObjectAnimator.ofFloat(wallpaperImage, "scaleX", 1f);
                        wallpaperScaleX.setInterpolator(new DecelerateInterpolator());
                        wallpaperScaleX.setDuration(500);
                        AnimatorSet scaleDown = new AnimatorSet();
                        scaleDown.play(scaleX).with(scaleY).with(translateY).with(translateX).with(backgroundScaleX).with(backgroundScaleY).with(wallpaperScaleX).with(wallpaperScaleY);
                        if (Math.abs(posY - offset) > windowSize.y /3) {
                            try {
                                ObjectAnimator _scaleY = ObjectAnimator.ofFloat(animImage, "scaleY", 0.7f);
                                scaleY.setInterpolator(new LinearOutSlowInInterpolator());
                                scaleY.setDuration(400);
                                ObjectAnimator _scaleX = ObjectAnimator.ofFloat(animImage, "scaleX", 0.7f);
                                scaleX.setInterpolator(new LinearOutSlowInInterpolator());
                                scaleX.setDuration(400);
                                ObjectAnimator _translateY = ObjectAnimator.ofFloat(animImage, "Y", 0);
                                translateY.setInterpolator(new LinearOutSlowInInterpolator());
                                translateY.setDuration(300);
                                ObjectAnimator _translateX = ObjectAnimator.ofFloat(animImage, "X", 50);
                                translateX.setInterpolator(new LinearOutSlowInInterpolator());
                                translateX.setDuration(300);
                                AnimatorSet _scaleDown = new AnimatorSet();
                                _scaleDown.play(_scaleX).with(_scaleY).with(_translateY).with(_translateX);
                                _scaleDown.start();
                                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                                forwardAppImage.setImageAlpha(0);
                                lastAppImage.setImageAlpha(0);
                                forwardAppImage.setY(0);
                                forwardAppImage.setX(windowSize.x);
                                lastAppImage.setY(0);
                                lastAppImage.setX(-windowSize.x);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        animImage.setImageAlpha(0);
                                        backgroundImage.setImageAlpha(0);
                                        wallpaperImage.setImageAlpha(0);
                                        blurView.setAlpha(0);
                                    }
                                }, 600);


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //Go home
                        } else if (Math.abs(posY - offset) > 130 && (Math.abs(animImage.getX()) < windowSize.x / 4 || Math.abs(posY - offset) > windowSize.y / 4)) {
                            sortAble = true;
                            scaleDown.start();
                            updateRadius.run();
//                            backgroundImage.animate().scaleX(1f).setDuration(200).start();
//                            backgroundImage.animate().scaleY(1f).setDuration(200).start();
                            lastAppImage.setImageAlpha(0);
                            forwardAppImage.setImageAlpha(0);
//                            Intent startMain = new Intent(Intent.ACTION_MAIN);
//                            startMain.addCategory(Intent.CATEGORY_HOME);
//                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(startMain);
                            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
//                            Intent intent = new Intent ("com.android.systemui.recents.action.TOGGLE_RECENTS");
//                            intent.setComponent (new ComponentName ("com.android.systemui", "com.android.systemui.recents.RecentsActivity"));
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity (intent);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    backgroundImage.setImageAlpha(0);
                                    wallpaperImage.setImageAlpha(0);
                                    blurView.setAlpha(0);

                                }
                            }, 500);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
//                                    background = takeScreenshot();
                                    forwardAppImage.setY(0);
                                    forwardAppImage.setX(windowSize.x);
                                    lastAppImage.setY(0);
                                    lastAppImage.setX(-windowSize.x);
//                                    backgroundImage.setImageBitmap(fastblur(background, 1, 20));
                                }
                            }, 420);
                        }
                        //Switch apps General
                        else if((Math.abs(posY - offset) < windowSize.y / 4) && (Math.abs(animImage.getX()) > windowSize.x / 4) && lastAppName != null) {
//                            updateParameters.width = 0;
//                            wm.updateViewLayout(ll, updateParameters);
                            sortAble = false;
                            //Switch apps left
                            if(animImage.getX() > windowSize.x / 4) {
                                //Check if app exists, if so continue
                                if(index - 1 >= 0) {
                                    //try to launch app, catch if app does not exist for whatever reason
                                    try {
                                        Intent i = getPackageManager().getLaunchIntentForPackage(appViewList.get(index - 1).getPackageName());
                                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(i);
                                        index--;
                                    } catch (Exception e) {
                                        appViewList.remove(index - 1);
                                        index--;
                                        e.printStackTrace();
                                    }


                                    animImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().translationXBy(-animImage.getX() + (windowSize.x * 1.5f)).setInterpolator(new DecelerateInterpolator()).setDuration(300).start();
                                    animImage.animate().translationYBy(-animImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().translationXBy(-lastAppImage.getX()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().translationYBy(-lastAppImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    updateParameters.y = offset;
                                    updateParameters.width = _width;
                                    wm.updateViewLayout(ll, updateParameters);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            backgroundImage.setImageAlpha(0);
                                            blurView.setAlpha(0);

                                            wallpaperImage.setImageAlpha(0);
                                        }
                                    }, 400);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            animImage.setImageAlpha(0);
                                            lastAppImage.setImageAlpha(0);
                                            forwardAppImage.setImageAlpha(0);
                                            animImage.setY(0);
                                            animImage.setX(0);
                                            lastAppImage.setY(0);
                                            lastAppImage.setX(-windowSize.x);
                                            forwardAppImage.setY(0);
                                            forwardAppImage.setX(windowSize.x);
                                        }
                                    }, 700);
                                //If last app does not exist, reset
                                } else {
                                    animImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().translationXBy(-animImage.getX()).setInterpolator(new DecelerateInterpolator()).setDuration(300).start();
                                    animImage.animate().translationYBy(-animImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    updateParameters.y = offset;
                                    updateParameters.width = _width;
                                    wm.updateViewLayout(ll, updateParameters);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            backgroundImage.setImageAlpha(0);
                                            blurView.setAlpha(0);

                                            wallpaperImage.setImageAlpha(0);

                                        }
                                    }, 300);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            animImage.setImageAlpha(0);
                                            lastAppImage.setImageAlpha(0);
                                            forwardAppImage.setImageAlpha(0);
                                            animImage.setY(0);
                                            animImage.setX(0);
                                            lastAppImage.setY(0);
                                            lastAppImage.setX(-windowSize.x);
                                            forwardAppImage.setY(0);
                                            forwardAppImage.setX(windowSize.x);
                                        }
                                    }, 700);
                                }
                            //Switch apps right
                            } else {
                                //Check if next app exists, if so continue
                                if(index + 1 < appViewList.size()) {
                                    //try to launch app if it exists, catch if it does not for whatever reason
                                    try {
                                        Intent i = getPackageManager().getLaunchIntentForPackage(appViewList.get(index + 1).getPackageName());
                                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(i);
                                        index++;
                                    } catch (Exception e) {
                                        appViewList.remove(index + 1);
                                        e.printStackTrace();
                                    }

                                    animImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().translationXBy(-(-animImage.getX() + (windowSize.x * 1.5f))).setInterpolator(new DecelerateInterpolator()).setDuration(300).start();
                                    animImage.animate().translationYBy(-animImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().translationXBy(-forwardAppImage.getX()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().translationYBy(-forwardAppImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    updateParameters.y = offset;
                                    updateParameters.width = _width;
                                    wm.updateViewLayout(ll, updateParameters);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            backgroundImage.setImageAlpha(0);
                                            wallpaperImage.setImageAlpha(0);
                                            blurView.setAlpha(0);

                                        }
                                    }, 400);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            animImage.setImageAlpha(0);
                                            lastAppImage.setImageAlpha(0);
                                            forwardAppImage.setImageAlpha(0);
                                            animImage.setY(0);
                                            animImage.setX(0);
                                            lastAppImage.setY(0);
                                            lastAppImage.setX(-windowSize.x);
                                            forwardAppImage.setY(0);
                                            forwardAppImage.setX(windowSize.x);
                                        }
                                    }, 700);
                                //If next app does not exist, reset
                                } else {
                                    animImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    animImage.animate().translationXBy(-animImage.getX()).setInterpolator(new DecelerateInterpolator()).setDuration(300).start();
                                    animImage.animate().translationYBy(-animImage.getY()).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    lastAppImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().scaleX(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    forwardAppImage.animate().scaleY(1f).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                                    updateParameters.y = offset;
                                    updateParameters.width = _width;
                                    wm.updateViewLayout(ll, updateParameters);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            backgroundImage.setImageAlpha(0);

                                            wallpaperImage.setImageAlpha(0);
                                            blurView.setAlpha(0);


                                        }
                                    }, 300);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            animImage.setImageAlpha(0);
                                            lastAppImage.setImageAlpha(0);
                                            forwardAppImage.setImageAlpha(0);
                                            animImage.setY(0);
                                            animImage.setX(0);
                                            lastAppImage.setY(0);
                                            lastAppImage.setX(-windowSize.x);
                                            forwardAppImage.setY(0);
                                            forwardAppImage.setX(windowSize.x);
                                        }
                                    }, 700);
                                }
                            }

                        }
                        //If swipe is not for switching apps or going home, reset
                        else {
                            sortAble = false;
                            lastAppImage.setImageAlpha(0);
                            forwardAppImage.setImageAlpha(0);
                            animImage.setImageAlpha(0);
                            backgroundImage.setImageAlpha(0);

                            wallpaperImage.setImageAlpha(0);
                            blurView.setAlpha(0);

                            lastAppImage.setY(0);
                            lastAppImage.setX(-windowSize.x);
                            forwardAppImage.setY(0);
                            forwardAppImage.setX(windowSize.x);
                        }

//                        updateParameters.y = offset;
//                        updateParameters.width = _width;
//                        wm.updateViewLayout(ll, updateParameters);
                        //
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                animImage.setImageAlpha(0);
                                animImage.animate().scaleX(1f).setDuration(0).start();
                                animImage.animate().scaleY(1f).setDuration(0).start();
//                                lastAppImage.animate().scaleY(1f).setDuration(0).start();
//                                lastAppImage.animate().scaleX(1f).setDuration(0).start();
//                                forwardAppImage.animate().scaleY(1f).setDuration(0).start();
//                                forwardAppImage.animate().scaleX(1f).setDuration(0).start();
                                animImage.setY(0);
                                animImage.setX(0);
                            }
                        }, 405);

                        break;
                    //endregion release
                    default:
                        break;
                }
                return true;
            }
        });
    }


    Runnable updateRadius = new Runnable() {
        @Override
        public void run() {
            try {
                radius = lerp(radius, 2f, 0.2f);
                blurView.setBlurRadius(radius);
            } finally {
                handler.postDelayed(updateRadius, 10);
                if(radius <= 3f) {
                    radius = 20;
                    blurView.setBlurEnabled(false);
                    blurView.setAlpha(0);
                    handler.removeCallbacks(updateRadius);
                }
            }
        }
    };


    public void setVals (int requestCode, int resultCode, Intent data ) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (projectionManager == null) throw new AssertionError();
        mProjection = projectionManager.getMediaProjection(resultCode, data);
        mImageReader = ImageReader.newInstance(windowSize.x, windowSize.y, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mProjection.createVirtualDisplay("ScreenSharingDemo",
                windowSize.x, windowSize.y, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null /*Callbacks*/, null /*Handler*/);
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(context);
        wallpaper = loadImageFromStorage(shre.getString("wallpaperDir", ""), "wallpaper.png");
        backgroundImage.setImageBitmap(loadImageFromStorage(shre.getString("iconsDir", ""), "icons.png"));
        wallpaperImage.setImageBitmap(wallpaper);
        iconsUnprocessed = loadImageFromStorage(shre.getString("iconsUnprocessedDir", ""), "icons_unprocessed.png");
    }

    public void takeWallpaperScreenshot () {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wallpaper = takeScreenshot();
                wallpaperImage.setImageBitmap(wallpaper);
            }
        }, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.github.bricktheworld.navigationx");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        }, 2000);
        wallpaperDir = SaveImage(wallpaper, "wallpaper.png");
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit=shre.edit();
        edit.putString("wallpaperDir", wallpaperDir);
        edit.commit();


    }


    public void takeIconScreenshot () {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                icons_unprocessed = takeScreenshot();
            }
        }, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.github.bricktheworld.navigationx");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                icons = icons_unprocessed;
                int _height = icons_unprocessed.getHeight();
                int _width = icons_unprocessed.getWidth();

                for(int y = 0; y < _height; y++) {
                    for(int x = 0; x < _width; x++) {
                        int pixel = icons_unprocessed.getPixel(x, y);
                        int pixel_wallpaper = wallpaper.getPixel(x, y);
                        if(pixel == pixel_wallpaper) {
                            icons_unprocessed.setPixel(x, y, Color.TRANSPARENT);
                        }
                    }
                }
                background = icons_unprocessed;
                backgroundImage.setImageBitmap(background);
                iconsDir = SaveImage(icons_unprocessed, "icons.png");
                iconsUnprocessedDir = SaveImage(icons, "icons_unprocessed.png");
                SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit=shre.edit();
                edit.putString("iconsDir", iconsDir);
                edit.putString("iconsUnprocessedDir", iconsUnprocessedDir);
                edit.commit();

            }
        }, 2000);


    }

    private Bitmap loadImageFromStorage(String path, String name) {

        try {
            File f=new File(path, name);
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            return b;
        }
        catch (FileNotFoundException e)
        {
            Toast.makeText(this, "file " + path + "/" + name + " not found", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }

    }
    private String SaveImage (Bitmap bitmapImage, String name) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory, name);
        if (mypath.exists())
            mypath.delete();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    private Bitmap takeScreenshot() {
        Image image = mImageReader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * windowSize.x;
        image.close();
        //Create bitmap
        Bitmap b = Bitmap.createBitmap(windowSize.x + rowPadding / pixelStride, windowSize.y, Bitmap.Config.ARGB_8888);
        b.copyPixelsFromBuffer(buffer);
        Bitmap croppedBitmap;
        try {
            croppedBitmap = Bitmap.createBitmap(b, 0, 0, windowSize.x, windowSize.y);
        } catch (OutOfMemoryError e) {
            Log.d(TAG, "Out of memory when cropping bitmap of screen size");
            croppedBitmap = b;
        }
        if (croppedBitmap != b) {
            b.recycle();
        }

        return croppedBitmap;
    }


    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    private static double getDifferencePercent(Bitmap img1, Bitmap img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();

        long diff = 0;
        for (int y = 0; y < height / 2; y+=4) {
            for (int x = 0; x < width; x += 4) {
                diff += pixelDiff(img1.getPixel(x, y), img2.getPixel(x, y));
            }
        }
        long maxDiff = 3L * 255 * width * height;

        return 100.0 * diff / maxDiff;
    }

    private static int pixelDiff(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >>  8) & 0xff;
        int b1 =  rgb1        & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >>  8) & 0xff;
        int b2 =  rgb2        & 0xff;
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    private float lerp(float a, float b, float percentage)
    {
        return (float) (a * (1.0 - percentage)) + (b * percentage);
    }
}