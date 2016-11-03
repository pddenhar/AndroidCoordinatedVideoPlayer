package com.fewstreet.coordinatedvideoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControlActivity extends AppCompatActivity {
    private InetAddress bcastAddr;
    DatagramSocket bcastSocket = null;
    final String TAG = "ControlActivity";
    ScheduledExecutorService scheduler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
    }

    @Override protected void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //check if the app is in control mode. If not, start the main activity
        boolean coordinatorMode = preferences.getBoolean("coordinatorMode", false);
        if(!coordinatorMode) {
            Intent intent = new Intent(this,VideoPlaybackActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            this.finish();
            return;
        }

        try {
            bcastAddr = getBroadcastAddress();
            Toast.makeText(this, "Broadcast Address = " + bcastAddr.toString(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Exception while trying to get WiFi broadcast address", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            bcastAddr=null;
        }

        TextView wifiState = (TextView)findViewById(R.id.wifiIP);
        if(bcastAddr!=null) {
            wifiState.setText("WiFi Broadcast Addr: " + bcastAddr.toString());
        } else {
            wifiState.setText(getResources().getString(R.string.wifi_not_connected));
        }

        try {
            bcastSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            Log.d(TAG, "Socket open failed");
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
            (new Runnable() {
                public void run() {
                    sendKeepalive();
                }
            }, 2, 10, TimeUnit.SECONDS);

    }

    @Override
    protected void onStop() {
        super.onStop();
        scheduler.shutdownNow();
        if(bcastSocket!=null) {
            bcastSocket.close();
        }
    }

    /**
     * Create an asynctask and send a broadcast udp packet with the contents of cp
     * @param cp Command packet to be broadcast out
     * @param repeatTimes How many times to send the packet
     * @param successToast Toast message to show on a successful broadcast
     * @param failToast Toast message to show if broadcast fails
     */
    public void sendCommandPacket(final CommandPacket cp, final int repeatTimes, final String successToast, final String failToast) {
        final Activity parent = this;
        Gson gson = new Gson();
        final String json = gson.toJson(cp);
        new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void... params) {
                byte[] message = json.getBytes();
                DatagramPacket p = new DatagramPacket(message, message.length, bcastAddr,8941);
                try {
                    for (int i = 0; i < repeatTimes; i++) {
                        bcastSocket.send(p);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            @Override
            protected void onPostExecute(Boolean result) {
                if(result==true) {
                    Log.d(TAG, "Sent command packet: "+ json);
                    if(successToast!= null) {
                        Toast.makeText(parent, successToast, Toast.LENGTH_SHORT).show();
                    }
                } else if(successToast!=null){
                    Toast.makeText(parent, failToast, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    public void broadcastClick(View v)
    {
        final Activity parent = this;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Integer delaypref = Integer.parseInt(preferences.getString("playbackDelay", "8"));
        if(delaypref < 0)
            delaypref = 8;
        final int delay = delaypref;
        long currentTime = System.currentTimeMillis()/1000L + delay;
        CommandPacket cp = new CommandPacket();
        cp.playback_ts = currentTime;
        sendCommandPacket(cp,10,"Sent broadcast to start playback in " + delay + " seconds", "Sending broadcast failed");
    }

    public void sendKeepalive() {
        final Activity parent = this;
        CommandPacket cp = new CommandPacket();
        cp.keep_alive = true;
        sendCommandPacket(cp, 2, null, getString(R.string.keepalive_failed));

    }

    public void clearScreen(View v) {
        final Activity parent = this;
        CommandPacket cp = new CommandPacket();
        cp.clear_screen = true;
        sendCommandPacket(cp,5,getString(R.string.clear_screen_command), getString(R.string.clear_screen_command_failed));
    }

    public void updateClick(View v) {
        final Activity parent = this;
        CommandPacket cp = new CommandPacket();
        cp.update_video = true;
        sendCommandPacket(cp,10,getString(R.string.sent_update_command), getString(R.string.update_command_failed));
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_settings, menu);
        return true;
    }

    public void launchSettings(MenuItem item) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }
    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();

        if (myDhcpInfo == null) {
            Toast.makeText(this, R.string.no_broadcast_address, Toast.LENGTH_LONG).show();
            return null;
        }
        Log.d(TAG, myDhcpInfo.toString());
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
                | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
}
