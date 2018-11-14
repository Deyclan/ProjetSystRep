package com.brandon.wifip2p.tcp;


import android.os.Handler;
import android.os.Message;

import com.brandon.wifip2p.activity.MainPanelActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnexionTcp implements Runnable {

    private Socket socket;
    private Handler handler;
    private InputStream reader;
    private OutputStream writer;

    private InetAddress address;
    private int port;

    public ConnexionTcp(Socket socket, Handler handler){
        this.socket = socket;
        this.handler = handler;
        try {
            reader = this.socket.getInputStream();
            writer = this.socket.getOutputStream();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ConnexionTcp(InetAddress address, int port, Handler handler){
        this.handler = handler;
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            if (socket == null) {
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), MainPanelActivity.TCP_TIMEOUT);
            }
            reader = this.socket.getInputStream();
            writer = this.socket.getOutputStream();
            while (socket.isConnected() && !socket.isInputShutdown()){
                int data;
                StringBuilder builder = new StringBuilder();    // TODO : Généraliser
                while ((data = reader.read()) != 10) {          // TODO : -1 = stream closed, \n = 10
                    builder.append((char)data);
                }
                postMessage(builder.toString());                // TODO : Généraliser
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void send(String s){
        if (!s.endsWith("\n"))
            s += "\n";
        send(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void send(final byte[] bytes){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (writer != null) {
                        writer.write(bytes);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    private void postMessage(String stringMessage){
        Message message = new Message();
        message.what = 0;
        message.obj = stringMessage;
        handler.dispatchMessage(message);
    }

    private void postData(byte[] datas){
        Message message = new Message();
        message.what = 1;
        message.obj = datas;
        handler.dispatchMessage(message);
    }

    public InetAddress getAddress(){
        if (this.address != null)
            return address;
        else
            return socket.getInetAddress();
    }
}
