package com.brandon.wifip2p.activity;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.brandon.wifip2p.R;

public class ChatActivity extends WifiP2pActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView console;
    private EditText messageEditText;
    private Button send;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initViews();
    }

    private void initViews(){
        drawerLayout = findViewById(R.id.chat_drawer_layout);
        navigationView = findViewById(R.id.chat_nav_view);
        console = findViewById(R.id.chatConsole);
        messageEditText = findViewById(R.id.chatMessage);
        send = findViewById(R.id.chatSend);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        super.onConnectionInfoAvailable(wifiP2pInfo);
        if (!wifiP2pInfo.groupFormed){
            finish();
        }
    }
}
