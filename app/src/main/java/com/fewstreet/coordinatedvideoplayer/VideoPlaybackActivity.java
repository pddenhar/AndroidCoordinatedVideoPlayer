package com.fewstreet.coordinatedvideoplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.VideoView;

import java.net.DatagramSocket;
import java.net.SocketException;

public class VideoPlaybackActivity extends AppCompatActivity {

    private final String TAG = "VideoPlaybackActivity";
    DatagramSocket syncSocket = null;
    ReceiveCommandTask syncSocketListener;
    UpdateVideoTask videoDownloadTask;
    private VideoView videoView;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private boolean mVisible;
    private static final int AUTO_HIDE_DELAY_MILLIS = 2500;
    private boolean videoReady = false;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private final Handler playHandler = new Handler();
    private final Runnable delayedPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if(videoReady) {
                Log.d(TAG,"Playing video");
                videoView.start();
            } else {
                Log.d(TAG, "Tried to play video but it was not ready");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate happened");
        setContentView(R.layout.activity_video_playback);
        mVisible = true;
        mContentView = findViewById(R.id.mainLayout);

        mContentView.setKeepScreenOn(true);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show();
            }
        });

        videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                videoReady = true;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(200);
    }

    public void startSocketListenerTask() {
        syncSocketListener = new ReceiveCommandTask(this);
        syncSocketListener.execute(syncSocket);
    }

    public void setVideoPlaybackTime(long unixtime) {
        long currentTime = System.currentTimeMillis();
        long delayMillis = (unixtime * 1000 - currentTime);
        if(delayMillis < 0){
            delayMillis=1;
        }
        delayMillis=3000;
        Log.d(TAG, "Current time is "+currentTime+" scheduling playback for "+delayMillis+" in the future.");
        playHandler.removeCallbacks(delayedPlayRunnable);
        playHandler.postDelayed(delayedPlayRunnable, delayMillis);
    }
    public void clearViewView() {
        videoView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
    }

    @Override protected void onStart() {
        super.onStart();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //check if the app is in control mode. If so, start that activity instead.
        boolean coordinatorMode = preferences.getBoolean("coordinatorMode", false);
        if(coordinatorMode) {
            Intent intent = new Intent(this,ControlActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            this.finish();
            return;
        }

        try {
            syncSocket = new DatagramSocket(8941);
            syncSocket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.d(TAG, "Socket open failed");
        }

        startSocketListenerTask();

        String videoPath = preferences.getString("filePicker", "");
        loadVideo(videoPath);

        delayedHide(100);
    }

    public void loadVideo(String videoPath) {
        if(!videoPath.equals("")) {
            videoView.setVideoURI(Uri.parse(videoPath));
            videoReady = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(syncSocket!=null) {
            syncSocketListener.cancel(true);
            syncSocket.close();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_settings, menu);
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    public void launchSettings(MenuItem item) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }
    public void updateVideo(MenuItem item) {
        if(videoDownloadTask == null || videoDownloadTask.getStatus() == AsyncTask.Status.FINISHED) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            videoDownloadTask = new UpdateVideoTask(this);
            String url = preferences.getString("fileURL", "");
            Log.d(TAG, "Starting video download from " + url);
            if (!url.equals("")) {
                videoDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
                Log.d(TAG, "Started task");
            }
        } else {
            Log.d(TAG, "Can't start video download task again because one is already running");
        }
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void hide() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mVisible = false;
    }

    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }
}
