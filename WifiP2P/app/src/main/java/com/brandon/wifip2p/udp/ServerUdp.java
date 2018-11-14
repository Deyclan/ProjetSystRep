package com.brandon.wifip2p.udp;

import android.os.Handler;
import android.os.Message;

import com.brandon.wifip2p.activity.MainPanelActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class ServerUdp implements Runnable {

    private int port;
    private DatagramSocket serverSocket;
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private Handler handler;

    private boolean running;

    public ServerUdp(int port, Handler handler){
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new DatagramSocket(port);
            running = true;
            while (running) {
                byte[] buffer = new byte[1024];
                receivePacket = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(receivePacket);
                handler.dispatchMessage(createDescriptionMessage(receivePacket));

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private Message createDescriptionMessage(DatagramPacket datagramPacket){
        Message message = new Message();
        message.what = MainPanelActivity.MESSAGE_TYPE;
        message.obj = new StringBuilder("Client connected.")
                .append("\tIP : ")
                .append(datagramPacket.getAddress().toString().replace("/", ""))
                .append("\n")
                .append("\tPort : ")
                .append(datagramPacket.getPort())
                .append("\n");
        return message;
    }

}
