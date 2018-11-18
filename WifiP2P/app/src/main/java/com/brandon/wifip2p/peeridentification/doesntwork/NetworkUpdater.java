package com.brandon.wifip2p.peeridentification.doesntwork;

import android.util.Log;

import com.brandon.wifip2p.Utils.DataHolder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NetworkUpdater implements Runnable {

    public static final int NETWORK_UPDATER_PORT = 50008;
    private static NetworkUpdater instance;

    public static NetworkUpdater getInstance(){
        if (instance == null)
            instance = new NetworkUpdater();
        return instance;
    }

    private NetworkUpdater(){}

    @Override
    public void run() {
        try {
            //ServerSocket serverSocket = new ServerSocket(NETWORK_UPDATER_PORT);
            //serverSocket.setReuseAddress(true);
            //serverSocket.bind(new InetSocketAddress(NETWORK_UPDATER_PORT));
            DatagramSocket serverSocket = new DatagramSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(NETWORK_UPDATER_PORT));
            byte[] buffer = new byte[500];
            while (true){
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                serverSocket.receive(packet);
                String network = new String(packet.getData(), StandardCharsets.UTF_8);
                Log.i("NetworkUpdater", "Strigyfied network received : " + network);
                DataHolder dataHolder = DataHolder.getInstance();
                dataHolder.buildStringyfiedNetwork(network);
                /*
                Socket socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String network = bufferedReader.readLine();
                Log.i("NetworkUpdater", "Strigyfied network received : " + network);
                DataHolder dataHolder = DataHolder.getInstance();
                dataHolder.buildStringyfiedNetwork(network);
                */
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendNetworkUpdate(){
        final DataHolder dataHolder = DataHolder.getInstance();
        for (final Map.Entry<String, String> entry : dataHolder.getNetworkInetAdressMap().entrySet()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] buffer = new byte[500];
                        DatagramSocket socket = new DatagramSocket();
                        socket.setReuseAddress(true);
                        socket.bind(new InetSocketAddress(entry.getValue().replace("/", ""), NETWORK_UPDATER_PORT));
                        buffer = dataHolder.stringifyNetwork().getBytes("UTF-8");
                        DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, buffer.length);
                        socket.send(datagramPacket);
                        //Socket socket = new Socket(InetAddress.getByName(entry.getValue().replace("/", "")), NETWORK_UPDATER_PORT);
                        //socket.setReuseAddress(true);
                        //socket.connect(new InetSocketAddress(entry.getValue(), NETWORK_UPDATER_PORT), 2000);
                        //socket.getOutputStream().write(dataHolder.stringifyNetwork().getBytes());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private Thread timedUpdate;
    public void sendNetworkUpdate(final long timelapsInSecond){
        if (timedUpdate != null)
            return;

        timedUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    sendNetworkUpdate();
                    Thread.sleep(timelapsInSecond * 1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        timedUpdate.start();
    }
    public void stopSendingNetworkUpdate(){
        if (timedUpdate != null){
            if (timedUpdate.isAlive()){
                timedUpdate.interrupt();
            }
            timedUpdate = null;
        }
    }

    /*
    private byte[] ipStringToByte(String ip){
        ip = ip.replace("/", "");
        String[] bytesStrings = ip.split(".");
        int[] ints = new int[]{Integer.parseInt(bytesStrings[0]),Integer.parseInt(bytesStrings[1]),Integer.parseInt(bytesStrings[2]),Integer.parseInt(bytesStrings[3])};
        return new byte[]{ints[0]}
    }
    */
}
