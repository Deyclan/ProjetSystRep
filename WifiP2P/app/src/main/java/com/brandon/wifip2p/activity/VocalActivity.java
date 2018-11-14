package com.brandon.wifip2p.activity;

import android.Manifest;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.brandon.wifip2p.R;
import com.brandon.wifip2p.voice.VoiceReceiver;
import com.brandon.wifip2p.voice.VoiceStreamer;

import java.net.InetAddress;

public class VocalActivity extends WifiP2pActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ListView connectedDevicesList;
    private Button startCall;
    private Button endCall;
    private Button startReceiveCall;
    private Button endReceiveCall;
    private TextView informations;

    private InetAddress selectedInetAddress;
    private VoiceReceiver voiceReceiver;
    private VoiceStreamer voiceStreamer;

    private final int VOICE_PORT = 50005;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocal);

        initViews();
        initListeners();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void initViews(){
        drawerLayout = findViewById(R.id.vocal_drawer_layout);
        navigationView = findViewById(R.id.vocal_nav_view);
        connectedDevicesList = findViewById(R.id.vocalConnectedList);
        startCall = findViewById(R.id.call);
        endCall = findViewById(R.id.stopCall);
        informations = findViewById(R.id.selectedDeviceDescr);
        startReceiveCall = findViewById(R.id.receiveCall);
        endReceiveCall = findViewById(R.id.stopReceivingCall);
    }

    private void initListeners(){
        startCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    selectedInetAddress = InetAddress.getByName(MainPanelActivity.currentIpConnected);
                    if (selectedInetAddress != null) {
                        if (voiceStreamer != null){
                            voiceStreamer.startStreaming();
                            informations.setText("Streaming MIC on port 50005");
                        }
                        else {
                            voiceStreamer = new VoiceStreamer(VOICE_PORT, selectedInetAddress);
                            voiceStreamer.startStreaming();
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceStreamer != null){
                    voiceStreamer.stopStreaming();
                    informations.setText("Stopped to stream MIC");
                }
            }
        });

        startReceiveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceReceiver != null && !voiceReceiver.isReceiving()){
                    voiceReceiver.startReceive();
                    informations.setText("Receiving stream on port 500005");
                }
                else if (voiceReceiver == null){
                    voiceReceiver = new VoiceReceiver(VOICE_PORT);
                    voiceReceiver.startReceive();
                    informations.setText("Receiving stream on port 500005");
                }
            }
        });

        endReceiveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceReceiver != null){
                    voiceReceiver.stopReceive();
                    informations.setText("Receiving Thread Stopped");
                }
            }
        });

        connectedDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiP2pDevice device = connectedDevices.get(i);
                informations.setText(new StringBuilder()
                        .append("Name : ")
                        .append(device.deviceName)
                        .append("\n")
                        .append("Address : ")
                        .append(device.deviceAddress)
                        .append("\n")
                        .append("Status : ")
                        .append(device.status)
                        .toString()
                );
                try {
                    if (MainPanelActivity.currentIpConnected != null)
                        selectedInetAddress = InetAddress.getByName(MainPanelActivity.currentIpConnected);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.vocalToChat:
                        break;
                    case R.id.vocalToMainBoard:
                        onBackPressed();
                        break;
                }
                return false;
            }
        });
    }

    private void disconnect(){
        if (voiceReceiver != null){
            voiceStreamer.stopStreaming();
        }
        if (voiceStreamer != null){
            voiceStreamer.stopStreaming();
        }
        informations.setText("Disconnected");
    }

    @Override
    public void finish() {
        super.finish();
        disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }
}
