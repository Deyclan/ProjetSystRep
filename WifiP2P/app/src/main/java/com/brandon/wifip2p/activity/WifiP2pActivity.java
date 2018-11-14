package com.brandon.wifip2p.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.brandon.wifip2p.p2pmanagement.WiFiDirectBroadcastReceiver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiP2pActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.PeerListListener {

    protected static List<WifiP2pDevice> availableDevices;
    protected static List<WifiP2pDevice> connectedDevices;

    protected static WifiManager wifiManager;
    protected static WifiP2pManager wifiP2pManager;
    protected static WifiP2pManager.Channel wifiP2pChannel;
    protected static BroadcastReceiver broadcastReceiver;
    protected static IntentFilter intentFilter;
    protected static WifiP2pGroup wifiP2pGroup;
    protected static WifiP2pInfo wifiP2pInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (availableDevices == null)
            availableDevices = new ArrayList<>();
        if (connectedDevices == null)
            connectedDevices = new ArrayList<>();

        if (!loadPreviousWifiP2pActivity())
            initWifi();
        if (!wifiManager.isWifiEnabled())
            askEnableWifi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
        registerReceiver(broadcastReceiver, intentFilter);
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    protected void askEnableWifi(){
        AlertDialog.Builder builder = new AlertDialog.Builder(WifiP2pActivity.this);
        builder.setTitle("Warning")
                .setMessage("This Application need Wifi to be turned on. If not, it may crash. \nDo you agree to enable wifi ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        enableWifi();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    protected void updateConnectedDeviceList(){
        wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                connectedDevices = new ArrayList<>();
                if (wifiP2pGroup != null) {
                    if (wifiP2pGroup.isGroupOwner()) {
                        Collection<WifiP2pDevice> wifiP2pDevices = wifiP2pGroup.getClientList();
                        connectedDevices = new ArrayList<>(wifiP2pDevices);
                    }
                    else{
                        connectedDevices.add(wifiP2pGroup.getOwner());
                    }
                }
            }
        });
    }
    protected void initWifi(){
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        broadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
    }
    protected void enableWifi(){
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }
    protected void createGroupP2P(){
        if (wifiP2pManager != null){
            wifiP2pManager.createGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i("WifiP2pActivity","Group Created");
                    Log.i("WifiP2pActivity","Waiting for GM connexions");
                }

                @Override
                public void onFailure(int i) {
                    Log.i("WifiP2pActivity","Failed to greate group");
                }
            });
        }
    }
    protected void disconnectGroupP2P(final boolean emergency){
        if (wifiP2pManager != null){
            wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener(){
                @Override
                public void onSuccess() {
                    Log.i("WifiP2pActivity","Disconnected from group");
                }

                @Override
                public void onFailure(int i) {
                    if (emergency){
                        Log.i("WifiP2pActivity","Failed to disconnect, shutting down wifi.");
                        wifiManager.setWifiEnabled(false);
                    }
                    else {
                        Log.i("WifiP2pActivity","Failed to disconnect.");
                    }
                }
            });
        }
    }
    protected void scanPeers(){
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                Log.d("DEBUG", "Discovering peers");
            }
            @Override
            public void onFailure(int i) {
                Log.d("DEBUG", "Failed to discover peers");
            }
        });
    }
    protected void connectToPeer(WifiP2pDevice p2pDevice){
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = p2pDevice.deviceAddress;
        wifiP2pManager.connect(wifiP2pChannel, config, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                Log.i("WifiP2pActivity","Initializing connexionTcp.");
            }
            @Override
            public void onFailure(int i) {
                Log.i("WifiP2pActivity","Failed to init connexionTcp.");
            }
        });
    }
    // Useless
    protected WifiP2pGroup getGroupInfo(){
        if (wifiP2pManager != null){
            wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                    WifiP2pActivity.this.wifiP2pGroup = wifiP2pGroup;
                }
            });
        }
        return wifiP2pGroup;
    }
    protected WifiP2pInfo getConnectionInfo(){
        if (wifiP2pManager != null){
            wifiP2pManager.requestConnectionInfo(wifiP2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    WifiP2pActivity.this.wifiP2pInfo = wifiP2pInfo;

                }
            });
        }
        return wifiP2pInfo;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

    }


    public List<WifiP2pDevice> getAvailableDevices() {
        return availableDevices;
    }

    public void setAvailableDevices(List<WifiP2pDevice> availableDevices) {
        this.availableDevices = availableDevices;
    }

    public List<WifiP2pDevice> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(List<WifiP2pDevice> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public void setWifiManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    public void setWifiP2pManager(WifiP2pManager wifiP2pManager) {
        this.wifiP2pManager = wifiP2pManager;
    }

    public WifiP2pManager.Channel getWifiP2pChannel() {
        return wifiP2pChannel;
    }

    public void setWifiP2pChannel(WifiP2pManager.Channel wifiP2pChannel) {
        this.wifiP2pChannel = wifiP2pChannel;
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public IntentFilter getIntentFilter() {
        return intentFilter;
    }

    public WifiP2pGroup getWifiP2pGroup() {
        return wifiP2pGroup;
    }

    public WifiP2pInfo getWifiP2pInfo() {
        return wifiP2pInfo;
    }

    protected void goToWifiP2pActivity(Class<? extends WifiP2pActivity> cls){
        Intent intent = new Intent(getApplicationContext(), cls);
        startActivity(intent);
    }

    protected boolean loadPreviousWifiP2pActivity(){
        return false;
    }

}
