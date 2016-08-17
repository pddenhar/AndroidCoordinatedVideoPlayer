package com.fewstreet.coordinatedvideoplayer;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;

/**
 * Created by peter on 8/17/16.
 */
public class ReceiveStartCommandTask extends AsyncTask<DatagramSocket,Void,Date> {
    private final String TAG = "ReceiveStartCommandTask";
    private VideoPlaybackActivity videoPlayer;

    public ReceiveStartCommandTask(VideoPlaybackActivity player) {
        super();
        videoPlayer = player;
    }

    @Override
    protected Date doInBackground(DatagramSocket... datagramSockets) {
        if(datagramSockets.length == 1){
            byte[] recvBuf = new byte[1500];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                datagramSockets[0].receive(packet);
            } catch (IOException e) {
                //e.printStackTrace();
                Log.d(TAG, "Datagram socket receive was interrupted ");
                return null;
            }
        }
        return null;
    }

    protected void onPostExecute(Date result) {
        if(result != null) {
            Log.d(TAG, "Got a time to play video: " + result.toString());
            videoPlayer.setVideoPlaybackTime(123);
        } else {
            Log.d(TAG, "Result from UDP socket was null");
        }
        videoPlayer.startSocketListenerTask(); //restart a new socket listener to receive any further packets
    }
}
