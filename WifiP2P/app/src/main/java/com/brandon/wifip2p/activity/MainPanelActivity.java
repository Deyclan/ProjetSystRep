package com.brandon.wifip2p.activity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.brandon.wifip2p.R;
import com.brandon.wifip2p.Utils.DataHolder;
import com.brandon.wifip2p.p2pmanagement.WiFiDirectBroadcastReceiver;
import com.brandon.wifip2p.peeridentification.doesntwork.NetworkUpdater;
import com.brandon.wifip2p.peeridentification.doesntwork.P2pIdentificationConnexion;
import com.brandon.wifip2p.tcp.ConnexionTcp;
import com.brandon.wifip2p.tcp.ServerTcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainPanelActivity extends WifiP2pActivity {

    private Button send;
    private EditText messageEditText;
    private TextView console;
    private TextView connexionStatus;
    private Switch forceGO;
    private Button discover;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ListView availableDevicesListView;
    private ListView connectedDevicesListView;

    public static int SERVER_PORT = 2525;
    public static int TCP_TIMEOUT = 1000;
    public final static int MESSAGE_TYPE = 0;
    public final static int DATA_TYPE = 1;
    public final static int IDENTIFICATION = 2;
    public final static int ERROR = -1;

    private ServerTcp serverTcp;
    private ConnexionTcp connexionTcp;
    private Thread serverTcpThread;
    private Thread connexionThread;

    private Handler loggerHandler;

    public static String currentIpConnected;
    private DataHolder dataHolder = DataHolder.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_panel);

        initWifi();
        initView();
        setupListeners();

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        if (getConnectionInfo() != null && getConnectionInfo().groupFormed)
            forceGO.setChecked(true);

        scanPeers();
    }

    private void initView() {
        send = findViewById(R.id.sendButton);
        messageEditText = findViewById(R.id.messageEditText);
        console = findViewById(R.id.console);
        connexionStatus = findViewById(R.id.connectionStatus);
        updateConnectionState(0);
        availableDevicesListView = findViewById(R.id.availableList);
        connectedDevicesListView = findViewById(R.id.connectedList);
        forceGO = findViewById(R.id.forceGO);
        discover = findViewById(R.id.discover);
        drawerLayout = findViewById(R.id.main_pannel_drawer_layout);
        navigationView = findViewById(R.id.main_panel_nav_view);
    }

    private void setupListeners() {
        loggerHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                    case MESSAGE_TYPE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logln(msg.obj.toString());
                                if (msg.obj.toString().contains("Client connected.\n\tIP : ")){
                                    currentIpConnected = msg.obj.toString().split("\n")[1].replace("\t", "").replace(" ", "").substring(3);
                                }
                            }
                        });
                        break;
                    case DATA_TYPE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logln("Data received");     // TODO
                            }
                        });
                        break;
                    case IDENTIFICATION:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String[] s = msg.obj.toString().split("#");
                                    dataHolder.updateNetworkPeers(s[0], s[1]);
                                    networkUpdater.sendNetworkUpdate();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    default:
                        break;
                }
            }
        };

        forceGO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    createGroupP2P();
                else
                    disconnectGroupP2P(false);
            }
        });

        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanPeers();
            }
        });

        availableDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connectToPeer(availableDevices.get(i));
            }
        });

        connectedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiP2pGroup wifiP2pGroupInfo = getGroupInfo();
                if (wifiP2pGroupInfo == null) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainPanelActivity.this);
                    alertDialogBuilder.setTitle("Warning")
                            .setMessage("You are the Group Owner. This action will remove the entire group. Are you sure of your move ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    disconnectGroupP2P(false);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                } else {
                    disconnectGroupP2P(false);
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connexionTcp != null) {
                    connexionTcp.send(messageEditText.getText().toString());
                } else if (serverTcp != null) {
                    serverTcp.sendBroadcast(messageEditText.getText().toString());
                }
                logln(messageEditText.getText().toString());
                messageEditText.setText("");
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mainToChat:
                        if (getConnectionInfo().groupFormed)
                            goToWifiP2pActivity(ChatActivity.class);
                        else
                            logln("Error : You need to be connected to at least one peer to go to Chat Activity.");
                        break;
                    case R.id.mainToVocal:
                        goToWifiP2pActivity(VocalActivity.class);
                        if (getConnectionInfo().groupFormed)
                            System.out.println(); // TODO : remettre la secu
                        else
                            logln("Error : You need to be connected to at least one peer to go to Vocal Activity.");
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getConnectionInfo() != null && getConnectionInfo().groupFormed)
            forceGO.setChecked(true);
        broadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    protected void updateConnectedDeviceList() {
        wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                connectedDevices = new ArrayList<>();
                if (wifiP2pGroup != null) {
                    if (wifiP2pGroup.isGroupOwner()) {
                        Collection<WifiP2pDevice> wifiP2pDevices = wifiP2pGroup.getClientList();
                        connectedDevices = new ArrayList<>(wifiP2pDevices);
                    } else {
                        connectedDevices.add(wifiP2pGroup.getOwner());
                    }
                }
                updateListView(connectedDevicesListView, connectedDevices);
            }
        });
    }

    private void updateListView(ListView listView, List<WifiP2pDevice> deviceList) {
        List<String> deviceNames = new ArrayList<>();
        for (WifiP2pDevice device : deviceList) {
            if (!device.deviceName.equals("") && !device.deviceName.contains("  ")) {
                deviceNames.add(device.deviceName);
            } else {
                String s = new StringBuilder().append(device.deviceAddress).append(" || type : ").append(device.primaryDeviceType).toString();
                if (s.equals(""))
                    deviceNames.add("Unknown device");
                else
                    deviceNames.add(s);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);
        listView.setAdapter(adapter);
    }

    private void updateConnectionState(int state) {
        switch (state) {
            case 0:
                connexionStatus.setText("Not Connected");
                connexionStatus.setTextColor(Color.rgb(230, 10, 10));
                break;
            case 1:
                connexionStatus.setText("Trying to connect");
                connexionStatus.setTextColor(Color.rgb(255, 200, 10));
                break;
            case 2:
                connexionStatus.setText("Connected");
                connexionStatus.setTextColor(Color.rgb(10, 230, 10));
                break;
        }
    }

    private void logln(String message) {
        log(new StringBuilder(message).append("\n").toString());
    }

    private void log(String message) {
        console.append(message);
    }

    private void disconnectTCP() {
        if (serverTcpThread != null && serverTcpThread.isAlive()) {
            serverTcpThread.interrupt();
        }
        if (connexionThread != null && connexionThread.isAlive()) {
            connexionThread.interrupt();
        }
        serverTcp = null;
        connexionTcp = null;
        logln("TCP services disconnected");
    }

    /**
     *  #########################################################################################
     *  #####                           WIFI DIRECT METHODS                                 #####
     *  #########################################################################################
     */

    @Override
    protected void createGroupP2P(){
        if (wifiP2pManager != null){
            wifiP2pManager.createGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    logln("Group Created");
                    logln("Waiting for GM connexions");
                }

                @Override
                public void onFailure(int i) {
                    logln("Failed to greate group");
                }
            });
        }
    }
    @Override
    protected void disconnectGroupP2P(final boolean emergency){
        if (wifiP2pManager != null){
            wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener(){
                @Override
                public void onSuccess() {
                    logln("Disconnected from group");
                }

                @Override
                public void onFailure(int i) {
                    if (emergency){
                        logln("Failed to disconnect, shutting down wifi.");
                        wifiManager.setWifiEnabled(false);
                    }
                    else {
                        logln("Failed to disconnect.");
                    }
                }
            });
        }
    }
    @Override
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
    @Override
    protected void connectToPeer(WifiP2pDevice p2pDevice){
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = p2pDevice.deviceAddress;
        wifiP2pManager.connect(wifiP2pChannel, config, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                logln("Initializing connexionTcp.");
                updateConnectionState(1);
            }
            @Override
            public void onFailure(int i) {
                logln("Failed to init connexionTcp.");
                updateConnectionState(0);
            }
        });
    }
    // Useless
    @Override
    protected WifiP2pGroup getGroupInfo(){
        if (wifiP2pManager != null){
            wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                    MainPanelActivity.this.wifiP2pGroup = wifiP2pGroup;
                }
            });
        }
        return wifiP2pGroup;
    }
    @Override
    protected WifiP2pInfo getConnectionInfo(){
        if (wifiP2pManager != null){
            wifiP2pManager.requestConnectionInfo(wifiP2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    MainPanelActivity.this.wifiP2pInfo = wifiP2pInfo;

                }
            });
        }
        return wifiP2pInfo;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            logln("Connected as Group Owner");
            updateConnectionState(2);
            dataHolder.updateNetworkPeers(thisDevice.deviceName, wifiP2pInfo.groupOwnerAddress.toString()); // TODO : Check thisDevice not null
            new P2pIdentificationConnexion(SERVER_PORT, loggerHandler).requestIdentification();
            if (NetworkUpdater.getInstance() != null){
                NetworkUpdater.getInstance().sendNetworkUpdate(20);
            }

        }
        else if (wifiP2pInfo.groupFormed){
            logln("Connected as Group Member");
            forceGO.setChecked(false);
            updateConnectionState(2);
            new P2pIdentificationConnexion(wifiP2pInfo.groupOwnerAddress, SERVER_PORT, thisDevice,  loggerHandler).sendIdentification();

        }
        else {
            updateConnectionState(0);
            forceGO.setChecked(false);
            scanPeers();
        }
        updateConnectedDeviceList();
        updateListView(connectedDevicesListView, connectedDevices);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        if (!wifiP2pDeviceList.equals(availableDevices)) {
            availableDevices.clear();
            availableDevices.addAll(wifiP2pDeviceList.getDeviceList());
            updateListView(availableDevicesListView, availableDevices);
        }
        if (availableDevices.size() == 0) {
            Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    @Override
    protected void goToWifiP2pActivity(Class<? extends WifiP2pActivity> cls) {
        super.goToWifiP2pActivity(cls);
        disconnectTCP();
    }
}
