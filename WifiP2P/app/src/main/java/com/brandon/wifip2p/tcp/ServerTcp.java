package com.brandon.wifip2p.tcp;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.brandon.wifip2p.activity.MainPanelActivity;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerTcp implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private Handler handler;
    private ConnexionTcp current;
    private Map<InetAddress, ConnexionTcp> connexionPool = new HashMap<>();
    private List<Thread> connexionThreadList = new ArrayList<>();
    private boolean running = true;

    public ServerTcp(int port, Handler handler){
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            handler.dispatchMessage(createStartingMessage(port));
            while (running) {
                Socket incommingClientSocket = serverSocket.accept();
                handler.dispatchMessage(createDescriptionMessage(incommingClientSocket));
                current = new ConnexionTcp(incommingClientSocket, handler);
                Thread t = new Thread(current);
                t.start();
                connexionThreadList.add(t);
                connexionPool.put(current.getAddress(), current);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendBroadcast(String message){
        for(ConnexionTcp connexionTcp : connexionPool.values()) {
            if (connexionTcp != null) {
                connexionTcp.send(message);
            }
        }
    }

    public void send(String message, InetAddress address){
        ConnexionTcp connexionTcp;
        connexionTcp = connexionPool.get(address);
        if (connexionTcp != null){
            connexionTcp.send(message);
        }
        else {
            Log.e("ServerTcp.send", "No connexionTcp with this InetAdress");
            // TODO
        }
    }

    public void stop(){
        running = false;
        try{
            for (Thread thread : connexionThreadList){
                thread.interrupt();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        connexionPool.clear();
    }

    private Message createStartingMessage(int port){
        Message message = new Message();
        try {
            message.what = 0;
            message.obj = new StringBuilder("Server successfully launched\n")
                    .append("\tIP : ")
                    .append(InetAddress.getLocalHost())
                    .append("\n")
                    .append("\tPort : ")
                    .append(port)
                    .append("\n")
                    .toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }

    private Message createDescriptionMessage(Socket socket){
        Message message = new Message();
        message.what = MainPanelActivity.MESSAGE_TYPE;
        message.obj = new StringBuilder("Client connected.")
                .append("\n\tIP : ")
                .append(socket.getInetAddress().toString().replace("/", ""))
                .append("\n")
                .append("\tPort : ")
                .append(socket.getPort())
                .append("\n")
                .append("\tPort (Local) : ")
                .append(socket.getLocalPort());
        return message;
    }
}
