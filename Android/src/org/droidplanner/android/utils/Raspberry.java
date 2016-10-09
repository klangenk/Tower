package org.droidplanner.android.utils;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by Kevin Langenkämper
 *
 * This class represents the connection to the manager running on the raspberry pi
 */
public class Raspberry {
    private static Socket socket;
    private static boolean  connected = false;

    private static OnConnectListener listener;

    public static void SetOnConnectListener(OnConnectListener listerner){
        Raspberry.listener = listerner;
    }

    public interface OnConnectListener{
        void onConnect();
    }

    public enum CaptureType{
        SingleShot{
            @Override
            public String toString() {
                return "SINGLE_SHOT";
            }
        },
        Series{
            @Override
            public String toString() {
                return "SERIES";
            }
        },
        Video{
            @Override
            public String toString() {
                return "VIDEO";
            }
        }
    }

    /**
     * Connects to the Raspberry Pi in the current thread
     * @param ServerIP
     * @param ServerPort
     * @return
     */
    private  static boolean _connect(String ServerIP, int ServerPort){
        if(socket != null && connected)
            return true;
        try {
            socket = new Socket(InetAddress.getByName(ServerIP), ServerPort);
            if(listener != null)
                listener.onConnect();
            return true;
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Connects to the Raspberry Pi in a background thread
     * @param ServerIP
     * @param ServerPort
     * @return
     */
    public static void connect(final String ServerIP, final int ServerPort){
        new Thread(){
            @Override
            public void run() {
                _connect(ServerIP, ServerPort);
            }
        }.start();

    }

    /**
     * Disconnect from Raspberry Pi
     */
    public static void disconect(){
        try {
            connected = false;
            socket.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Sends given message to Raspberry Pi
     * @param message
     */
    private static void sendMessage(String message){
        try {
            if(socket == null)
                return;
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
            out.println(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends CaptureType to Raspberry Pi
     * @param captureType
     */
    public static void setCaptureType(CaptureType captureType){
        sendMessage(captureType.toString());
    }
}
