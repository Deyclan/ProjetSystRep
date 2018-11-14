package com.brandon.wifip2p.activity;

import android.Manifest;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.brandon.wifip2p.R;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class VocalActivityOld extends WifiP2pActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ListView connectedDevicesList;
    private Button startCall;
    private Button endCall;
    private Button startReceiveCall;
    private Button endReceiveCall;
    private TextView informations;

    private AudioManager audioManager;
    private AudioGroup audioGroup;
    private AudioStream audioStream;

    private InetAddress selectedInetAddress;
    private static int RTP_PORT = 22222;

    private Thread updateListViewThread;

    //Audio Configuration.
    private static final int RECORDER_SAMPLE_RATE = 44100;      //8000
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    //private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_8BIT;
    private AudioRecord recorder;
    private AudioTrack speaker;
    private Thread streamingThread;
    private Thread receivingThread;

    int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int bytesPerElement = 2; // 2 bytes in 16bit format

    private boolean streaming = true;
    private boolean receiving = true;

    private MediaPlayer mediaPlayer = new MediaPlayer();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocal);
        initViews();
        initListeners();

        updateListViewThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    updateConnectedDeviceList();
                    updateListView(connectedDevicesList, connectedDevices);
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        updateListViewThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startCall(InetAddress inetAddress, int port) throws Exception {
        if (audioManager == null)
            return;

        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioStream = new AudioStream(InetAddress.getByAddress(getLocalIPAddress()));
        audioStream.setCodec(AudioCodec.PCMU);
        audioStream.setMode(RtpStream.MODE_NORMAL);
        audioStream.associate(inetAddress, port);
        audioStream.join(audioGroup);
    }

    public void startStreaming() {
        streamingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //int minBufSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
                    DatagramSocket socket = new DatagramSocket();

                    //byte[] buffer = new byte[minBufSize];
                    byte[] buffer = new byte[2200];

                    DatagramPacket packet;

                    //final InetAddress destination = selectedInetAddress;
                    final InetAddress destination = InetAddress.getByName("192.168.43.234");

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING,/*minBufSize*/ 2200);

                    while(streaming) {
                        //reading data from MIC into buffer
                        int byteReaded = recorder.read(buffer, 0, buffer.length);
                        Log.d("VS", "Reading data from mic");

                        //putting buffer in the packet
                        //packet = new DatagramPacket (buffer,byteReaded,destination,RTP_PORT);
                        packet = new DatagramPacket (buffer,byteReaded,destination,22223);
                        Log.d("VS", "Writing to packet");

                        socket.send(packet);
                        Log.d("VS", "Sending packet");

                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        });
        streamingThread.start();
    }
    public void stopStreaming(){
        this.streaming = false;
        if (streamingThread != null)
            streamingThread.interrupt();
        endCall();
    }

    public void startReceiving(){
        Thread receiveThread = new Thread (new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(RTP_PORT);
                    Log.d("VR", "Socket Created");

                    //minimum buffer size. need to be careful. might cause problems. try setting manually if any problems faced
                    int minBufSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
                    System.out.println(minBufSize);
                    byte[] buffer = new byte[minBufSize];
                    /*
                    speaker = new AudioTrack(AudioManager.STREAM_MUSIC,RECORDER_SAMPLE_RATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING,minBufSize,AudioTrack.MODE_STREAM);
                    speaker.play();
                    */

                    AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(RECORDER_SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
                    speaker = new AudioTrack(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build(), audioFormat, minBufSize, AudioTrack.MODE_STREAM, 0);
                    speaker.play();

                    while(receiving) {
                        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                        socket.receive(packet);
                        Log.d("VR", "Packet Received");

                        //reading content from packet
                        buffer=packet.getData();
                        Log.d("VR", "Packet data read into buffer");

                        //sending data to the Audiotrack obj i.e. speaker
                        speaker.write(buffer, 0, minBufSize);
                        Log.d("VR", "Writing buffer content to speaker");

                        //playSound(buffer);
                        // playMp3(buffer); //TODO : Try this

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        receiveThread.start();
    }
    public void stopReceiving(){
        receiving = false;
        if (receivingThread != null)
            receivingThread.interrupt();
        endCall();
    }

    private void endCall(){
        if (audioManager == null)
            return;

        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioStream = null;
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (audioManager != null)
            audioManager.setMode(AudioManager.MODE_NORMAL);
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
                        //startCall(selectedInetAddress, RTP_PORT);
                        startStreaming();
                        informations.setText("Streaming");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //endCall();
                //informations.setText("");
                //selectedInetAddress = null;
                stopStreaming();
                informations.setText("Stop Streaming");
            }
        });

        startReceiveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startReceiving();
                informations.setText("Receiving Thread launched");
            }
        });

        endReceiveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopReceiving();
                informations.setText("Receiving Thread Stopped");
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

    private void initAudioManager(){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        audioManager =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);
    }

    public static byte[] getLocalIPAddress () {
        byte ip[]=null;
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface)en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ip= inetAddress.getAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.i("SocketException ", ex.toString());
        }
        return ip;

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        super.onConnectionInfoAvailable(wifiP2pInfo);
        if (!wifiP2pInfo.groupFormed){
            //endCall();
            //finish();  // TODO : uncomment
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        super.onPeersAvailable(wifiP2pDeviceList);
        updateConnectedDeviceList();

    }

    private void updateListView(ListView listView, List<WifiP2pDevice> devices){
        List<String> connectedDeviceName = new ArrayList<>();
        for (WifiP2pDevice device : devices) {
            connectedDeviceName.add(device.deviceName);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, connectedDeviceName);
        listView.setAdapter(adapter);
    }

    @Override
    public void finish() {
        super.finish();
        updateListViewThread.interrupt();
        if(audioManager != null)
            audioManager.setMode(AudioManager.MODE_NORMAL);
    }
}
