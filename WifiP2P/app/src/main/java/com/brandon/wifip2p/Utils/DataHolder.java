package com.brandon.wifip2p.Utils;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHolder {
    private static final DataHolder ourInstance = new DataHolder();

    public static DataHolder getInstance() {
        return ourInstance;
    }

    private DataHolder() {
    }

    private Map<String, String> networkInetAdressMap = new HashMap<>();

    public void addPeerToNetwork(String name, String address){
        networkInetAdressMap.put(name, address);
    }

    public void updateNetworkPeers(String name, String address){
        if(!networkInetAdressMap.containsKey(name) && !networkInetAdressMap.containsValue(address)){
            addPeerToNetwork(name, address);
        }
        else if (networkInetAdressMap.containsKey(name) && !networkInetAdressMap.containsValue(address)){
            networkInetAdressMap.remove(name);
            addPeerToNetwork(name,address);
        }
        else if (!networkInetAdressMap.containsKey(name) && networkInetAdressMap.containsValue(address)) {
            Map.Entry<String, String> toRemove = null;
            for (Map.Entry<String, String> entry : networkInetAdressMap.entrySet()) {
                if (entry.getValue().equals(address)) {
                    toRemove = entry;
                }
            }
            if (toRemove != null) {
                networkInetAdressMap.entrySet().remove(toRemove);
                addPeerToNetwork(name, address);
            }
        }
    }


    public String getInetAddressByName(String name){
        return networkInetAdressMap.get(name);
    }

    public Map<String, String> getNetworkInetAdressMap(){
        return networkInetAdressMap;
    }

    public String stringifyNetwork(){
        StringBuilder stringNetwork = new StringBuilder();
        stringNetwork.append("{");
        for (Map.Entry<String, String> entry: networkInetAdressMap.entrySet()) {
            stringNetwork.append(entry.getKey())
                    .append(",")
                    .append(entry.getValue())
                    .append(";");
        }
        stringNetwork.append("}\n");
        return stringNetwork.toString();
    }

    public void buildStringyfiedNetwork(String stringifiedNetwork) throws Exception {
        if (stringifiedNetwork.charAt(0) != "{".charAt(0) && stringifiedNetwork.charAt(stringifiedNetwork.length()) != "}".charAt(0)){
            throw new Exception("Error, not a stringyfied network");
        }
        Log.i("DataHolder", "Building Network");
        String s = stringifiedNetwork.substring(1, stringifiedNetwork.length()-1);
        String[] entries = s.split(";");
        networkInetAdressMap = new HashMap<>();
        for (String string: entries) {
            if (string.length() >= 10) {
                String[] entry = string.split(",");
                networkInetAdressMap.put(entry[0],entry[1]);
            }
        }
        Log.i("DataHolder", "Network built");
    }

    public InetAddress getCurrentIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while(nias.hasMoreElements()) {
                    InetAddress ia= (InetAddress) nias.nextElement();
                    if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                        return ia;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }


}
