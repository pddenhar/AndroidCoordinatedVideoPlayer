package com.fewstreet.coordinatedvideoplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import java.net.DatagramSocket;
import java.net.SocketException;

public class VideoPlaybackActivity extends AppCompatActivity {

    private final String TAG = "VideoPlaybackActivity";
    DatagramSocket syncSocket = null;
    ReceiveStartCommandTask syncSocketListener;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private boolean mVisible;
    private static final int AUTO_HIDE_DELAY_MILLIS = 1500;
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
                final VideoView videoView = (VideoView) findViewById(R.id.videoView);
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

        final VideoView videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                videoReady = true;
            }
        });

//        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
//        {
//            @Override
//            public void onCompletion(MediaPlayer mp)
//            {
//                videoView.setVisibility(View.GONE);
//                videoView.setVisibility(View.VISIBLE);
//            }
//        });
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
        syncSocketListener = new ReceiveStartCommandTask(this);
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


    @Override protected void onStart() {
        super.onStart();

        try {
            syncSocket = new DatagramSocket(8941);
            syncSocket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
            Log.d(TAG, "Socket open failed");
        }

        startSocketListenerTask();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String videoPath = preferences.getString("filePicker", "");
        final VideoView videoView = (VideoView) findViewById(R.id.videoView);
        if(!videoPath.equals("")) {
            videoView.setVideoURI(Uri.parse(videoPath));
            videoReady = false;
        }

        delayedHide(100);
    }

    @Override
    protected void onStop() {
        super.onStop();
        syncSocketListener.cancel(true);
        syncSocket.close();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    public void launchSettings(MenuItem item) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
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
