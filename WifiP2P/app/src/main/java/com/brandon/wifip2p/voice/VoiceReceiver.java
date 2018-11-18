package com.brandon.wifip2p.voice;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VoiceReceiver {

    //private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private final int CHANNEL_CONFIG = 1;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int SAMPLE_RATE = 44100;

    private boolean receiving = true;
    private int port;

    private AudioTrack speaker;
    private Thread receiver;

    public VoiceReceiver(int port) {
        this.port = port;
    }

    public void startReceive() {
        receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("VoiceReceiver", "Starting Receiver Thread");
                    DatagramSocket datagramSocket = new DatagramSocket(port);
                    datagramSocket.setReuseAddress(true);
                    int i = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
                    byte[] receiveData = new byte[i];        // ( 1280 for 16 000Hz and 3584 for 44 100Hz (use AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING) to get the correct size)

                    speaker = initSpeaker(SAMPLE_RATE, ENCODING, CHANNEL_CONFIG, i/20+1, AudioTrack.MODE_STREAM);

                    DatagramPacket packet;
                    while (receiving){
                        packet = new DatagramPacket(receiveData, receiveData.length);
                        datagramSocket.receive(packet);
                        receiveData = packet.getData();
                        toSpeaker(receiveData);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        receiver.start();
    }

    public void stopReceive(){
        try {
            receiving = false;
            Thread.sleep(100);
            if (receiver != null && receiver.isAlive())
                receiver.interrupt();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private AudioTrack initSpeaker(int sampleRate, int encoding, int channelConfig, int bufferSizeInByte, int audioTrackMode){
        /*
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .build();

        AudioTrack speaker = new AudioTrack(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build(),
                format,
                bufferSizeInByte,
                audioTrackMode,
                0);
        */
        AudioTrack speaker = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, encoding, bufferSizeInByte, audioTrackMode);
        speaker.play();
        return speaker;
    }

    private void toSpeaker(byte[] soundBytes){
        if (speaker != null){
            speaker.write(soundBytes, 0, soundBytes.length);
        }
    }

    public boolean isReceiving() {
        return receiving;
    }
}
