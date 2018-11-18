package com.brandon.wifip2p.peeridentification.doesntwork;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;

import com.brandon.wifip2p.Utils.DataHolder;
import com.brandon.wifip2p.activity.MainPanelActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class P2pIdentificationConnexion {

    private Socket socket;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;

    private InetAddress address;
    private int port;
    private WifiP2pDevice device;

    public static final String IDENTIFICATION_REQUEST = "ID?";
    public static final String ERROR = "ERROR\n";

    public P2pIdentificationConnexion(InetAddress address, int port, WifiP2pDevice device,  Handler handler){
        this.handler = handler;
        this.address = address;
        this.port = port;
        this.device = device;
    }

    public P2pIdentificationConnexion(int port, Handler handler){
        this.port = port;
        this.handler = handler;
    }

    public void startIdentificationServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessageTextToHandler("Starting identification server");
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(port));
                    socket = serverSocket.accept();
                    sendMessageTextToHandler("Incoming connexion received");
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    outputStream.write((IDENTIFICATION_REQUEST + "\n").getBytes());
                    sendMessageTextToHandler("Sent ID REQUEST");
                    String id = reader.readLine();
                    sendMessageTextToHandler("Response Received : " + id.replace("\n", "/n"));
                    Message message = new Message();
                    if (id.equals(ERROR)){
                        message.what = MainPanelActivity.ERROR;
                    }
                    else {
                        message.what = MainPanelActivity.IDENTIFICATION;
                        message.obj = new StringBuilder(id).append("#").append(socket.getInetAddress()).toString();
                    }
                    handler.dispatchMessage(message);
                    serverSocket.close();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Server method
     */
    public void requestIdentification(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessageTextToHandler("Starting request server");
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(port));
                    socket = serverSocket.accept();
                    sendMessageTextToHandler("Incoming connexion received");
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    outputStream.write((IDENTIFICATION_REQUEST + "\n").getBytes());
                    sendMessageTextToHandler("Sent ID REQUEST");
                    String id = reader.readLine();
                    sendMessageTextToHandler("Response Received : " + id.replace("\n", "/n"));
                    Message message = new Message();
                    if (id.equals(ERROR)){
                        message.what = MainPanelActivity.ERROR;
                    }
                    else {
                        message.what = MainPanelActivity.IDENTIFICATION;
                        message.obj = new StringBuilder(id).append("#").append(socket.getInetAddress()).toString();
                    }
                    handler.dispatchMessage(message);
                    serverSocket.close();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Client Method
     */
    public void sendIdentification(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessageTextToHandler("SendIdentification called");
                    if (socket == null) {
                        socket = new Socket();
                        socket.setReuseAddress(true);
                        socket.connect(new InetSocketAddress(address, port), 10000);
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        sendMessageTextToHandler("Socket connected : IP " + socket.getInetAddress() +" \tIP Local : " + socket.getLocalAddress());
                    }
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    Message message = new Message();
                    message.what = MainPanelActivity.MESSAGE_TYPE;
                    String temp = bufferedReader.readLine();
                    sendMessageTextToHandler("Received : " + temp);
                    if (temp.equals(IDENTIFICATION_REQUEST)){
                        outputStream.write(new StringBuilder(device.deviceName).append("\n").toString().getBytes());
                        message.obj = "Sent identification successfully";
                    }
                    else {
                        outputStream.write(ERROR.getBytes());
                        message.obj = "Error during identification";
                    }
                    handler.dispatchMessage(message);

                    socket.close();
                    socket = null;

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void sendMessageTextToHandler(String messageString){
        Message message = new Message();
        message.what = MainPanelActivity.MESSAGE_TYPE;
        message.obj = messageString;
        handler.dispatchMessage(message);
    }
}
