package com.fewstreet.coordinatedvideoplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_playback);
    }

    public void startSocketListenerTask() {
        syncSocketListener = new ReceiveStartCommandTask(this);
        syncSocketListener.execute(syncSocket);
    }

    public void setVideoPlaybackTime(long unixtime) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String videoPath = preferences.getString("filePicker", "");
        Log.d(TAG, "Path: " + videoPath);
        //for now, just play the video instead of scheduling it
        final VideoView videoView = (VideoView) findViewById(R.id.videoView);
        if(!videoPath.equals("")) {
            //videoView.setVideoPath(videoPath);
            videoView.setVideoURI(Uri.parse(videoPath));

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    videoView.start();
                }
            });
        }
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

    private static final int FILE_SELECT_CODE = 0;
    private Uri videoURI = null;
    public void chooseVideoClick(View view) {
        showFileChooser();
    }
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Play"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    videoURI = uri;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
