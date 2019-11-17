package com.github.bricktheworld.navigationx;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class AppView {
    private Bitmap splash;
    private String packageName;

    public Bitmap getSplash () {
        return splash;
    }
    public void setSplash (Bitmap value) {
        splash = value;
    }
    public String getPackageName () {
        return packageName;
    }
    public void setPackageName (String value) {
        packageName = value;
    }

}
