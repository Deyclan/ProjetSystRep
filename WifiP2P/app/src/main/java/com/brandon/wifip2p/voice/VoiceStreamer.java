package com.brandon.wifip2p.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VoiceStreamer {

    private int port = 50005;
    private InetAddress destination;

    private final int sampleRate = 44100 ; // 44100 for music, 16000 voice
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private AudioRecord recorder;

    private boolean isStreaming = false;
    private Thread streamingThread;

    public VoiceStreamer(int port, InetAddress destination) {
        this.port = port;
        this.destination = destination;
    }

    public void startStreaming(){
        if(isInitialized()) {
            isStreaming = true;
            streamingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramSocket socket = new DatagramSocket();
                        socket.setReuseAddress(true);
                        byte[] buffer = new byte[minBufSize];
                        DatagramPacket packet;

                        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                        recorder.startRecording();

                        while (isStreaming) {
                            minBufSize = recorder.read(buffer, 0, buffer.length);
                            packet = new DatagramPacket(buffer, buffer.length, destination, port);
                            socket.send(packet);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            streamingThread.start();
        }
        else {
            Log.e("VoiceStreamer", "Voice Streamer isn't initialized.");
        }
    }

    public void stopStreaming(){
        isStreaming = false;
        try {
            Thread.sleep(100);
            if(streamingThread != null && streamingThread.isAlive())
                streamingThread.interrupt();
            if (recorder != null)
                recorder.release();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean isInitialized(){
        return destination != null;
    }
}
