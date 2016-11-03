package com.fewstreet.coordinatedvideoplayer;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by peter on 8/17/16.
 */
public class ReceiveCommandTask extends AsyncTask<DatagramSocket,Void,CommandPacket> {
    private final String TAG = "ReceiveCommandTask";
    private VideoPlaybackActivity videoPlayer;

    public ReceiveCommandTask(VideoPlaybackActivity player) {
        super();
        videoPlayer = player;
    }

    @Override
    protected CommandPacket doInBackground(DatagramSocket... datagramSockets) {
        if(datagramSockets.length == 1){
            byte[] recvBuf = new byte[1500];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                datagramSockets[0].receive(packet);
                String json = new String(packet.getData(), 0, packet.getLength());
                Gson gson = new Gson();
                CommandPacket recvd_command = gson.fromJson(json, CommandPacket.class);
                return recvd_command;
            } catch (IOException e) {
                //e.printStackTrace();
                Log.d(TAG, "Datagram socket receive was interrupted ");
                return null;
            }
        }
        return null;
    }

    protected void onPostExecute(CommandPacket result) {
        if(result != null && result.playback_ts != null) {
            Log.d(TAG, "Got a time to play video: " + result.toString());
            videoPlayer.setVideoPlaybackTime(result.playback_ts);
        } else if(result != null && result.update_video) {
            Log.d(TAG, "Received update command");
            videoPlayer.updateVideo(null);
        } else if(result!=null && result.clear_screen) {
            Log.d(TAG, "Received clear screen command.");
            videoPlayer.clearViewView();
        } else if(result!=null && result.keep_alive) {
            Log.d(TAG, "Received keep alive packet.");
        } else {
            Log.d(TAG, "Result from UDP socket was null");
        }
        videoPlayer.startSocketListenerTask(); //restart a new socket listener to receive any further packets
    }
}
