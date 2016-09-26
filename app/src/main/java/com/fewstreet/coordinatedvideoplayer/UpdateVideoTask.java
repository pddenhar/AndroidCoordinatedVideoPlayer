package com.fewstreet.coordinatedvideoplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by peter on 9/22/16.
 */
public class UpdateVideoTask extends AsyncTask<String,Void,URI> {
    private final String TAG = "UpdateVideoTask";
    VideoPlaybackActivity ctx;

    public UpdateVideoTask(VideoPlaybackActivity ctx) {
        super();
        this.ctx = ctx;
    }

    @Override
    protected URI doInBackground(String... strings) {
        Log.d(TAG, "made it here");
        if(strings.length == 1) {
            String fileurl = strings[0];
            final int TIMEOUT_CONNECTION = 5000;//5sec
            final int TIMEOUT_SOCKET = 30000;//30sec

            URL url = null;
            try {
                url = new URL(fileurl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            long startTime = System.currentTimeMillis();
            Log.d(TAG, "Video download beginning: "+fileurl);
            try {
                //Open a connection to that URL.
                URLConnection ucon = url.openConnection();

                //this timeout affects how long it takes for the app to realize there's a connection problem
                ucon.setReadTimeout(TIMEOUT_CONNECTION);
                ucon.setConnectTimeout(TIMEOUT_SOCKET);


                //Define InputStreams to read from the URLConnection.
                // uses 3KB download buffer
                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 50);
                FileOutputStream outStream = ctx.openFileOutput("video.mp4", Context.MODE_PRIVATE);
                Log.d(TAG, "File path: "+ctx.getFileStreamPath("video.mp4"));
                byte[] buff = new byte[50 * 1024];

                //Read bytes (and store them) until there is nothing more to read(-1)
                int len;
                while ((len = inStream.read(buff)) != -1) {
                    outStream.write(buff, 0, len);
                }

                //clean up
                outStream.flush();
                outStream.close();
                inStream.close();

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            Log.d(TAG, "Video download completed in " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return ctx.getFileStreamPath("video.mp4").toURI();
        }
        return null;
    }

    protected void 	onPreExecute() {
        Toast.makeText(ctx, "Starting video download task", Toast.LENGTH_SHORT).show();
    }

    protected void onPostExecute(URI result) {
        if(result!= null) {
            Toast.makeText(ctx, "Video download successful", Toast.LENGTH_SHORT).show();
            String uri =result.toString();
            Log.d(TAG, "Setting video file URI to "+uri);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("filePicker", uri);
            editor.commit();
            ctx.loadVideo(uri);
        } else {
            Toast.makeText(ctx, "Video download failed", Toast.LENGTH_SHORT).show();
        }
    }
}