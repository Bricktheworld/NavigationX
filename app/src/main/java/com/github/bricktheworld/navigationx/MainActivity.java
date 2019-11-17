package com.github.bricktheworld.navigationx;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    //Start button
    private Button StartNavBar;
    //Wallpaper Screenshot button
    private Button WallpaperButton;
    //Icon Screenshot button
    private Button IconButton;



    //Request Codes to Start the Settings Menu Request
    public final static int REQUEST_CODE = 10101;
    public final static int PERMISSION_CODE = 10102;

    //Request, result, and data codes from the on Activity result, required for Screenshots
    private int requestCode,resultCode;
    private Intent data;

    //Intent to start Floating Window class
//    private Intent intent;

    //Resulting Service from binding to the Floating Window service
    private FloatingWindow mService;

    //After requesting permission to record the screen, we set the global variables for use later in the service class
    @Override
    protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
        requestCode = _requestCode;
        resultCode = _resultCode;
        data = _data;
    }

    //everything that happens when created lol
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check if user has given permission to draw over other apps
        checkDrawOverlayPermission();

        checkUsagePermission();
        //ask user to record the screen
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), PERMISSION_CODE);

        //Getting the start button in the activity
        StartNavBar = findViewById(R.id.start);
        //Listener for the start button click
        StartNavBar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mService = mService.getSharedInstance();
                if (mService != null) {
                    // The service is running and connected.
                    mService.setVals(requestCode, resultCode, data);
                }
            }
        });
        //Getting the start button in the activity
        WallpaperButton = findViewById(R.id.WallpaperButton);
        //Listener for the start button click
        WallpaperButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mService = mService.getSharedInstance();

                mService.takeWallpaperScreenshot();
            }
        });
        //Getting the start button in the activity
        IconButton = findViewById(R.id.IconButton);
        //Listener for the start button click
        IconButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mService = mService.getSharedInstance();

                mService.takeIconScreenshot();
            }
        });

    }



    //Getting the Permission from the user to draw over other apps (If not already gotten)
    public void checkDrawOverlayPermission() {

        // Checks if app already has permission to draw overlays
        if (!Settings.canDrawOverlays(this)) {

            // If not, form up an Intent to launch the permission request
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));

            // Launch Intent, with the supplied request code
            startActivityForResult(intent, REQUEST_CODE);
        }

    }

    public void checkUsagePermission() {
        boolean granted = false;
        AppOpsManager appOps = (AppOpsManager) this
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), this.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (this.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        if(!granted) {
            Intent UsageIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(UsageIntent, 10201);
        }
    }
}
