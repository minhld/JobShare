package com.minhld.supports;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by minhld on 10/28/2015.
 */
public class SendManager extends Thread {
    final String TAG = "SendManager";
    final int SOCKET_TIMEOUT = 8000;
    final int GROUP_PORT = 8881;


    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;

    private DataListener listener;

    public SendManager(Socket sk, DataListener listener) {
        this.socket = sk;
        this.listener = listener;
        System.out.println("new connection with client " + this.socket);
    }

    public SendManager(String hostAddress, DataListener listener) {
        this.listener = listener;

        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect(new InetSocketAddress(hostAddress, GROUP_PORT), SOCKET_TIMEOUT);
        } catch(IOException e) {
            System.out.println("new connection with client " + this.socket);
        }
    }

    public void run() {

        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();

            if (listener != null) {
                listener.socketReady(true);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

            while (true) {
                String input = in.readLine();
                if (input.equals(".")) {
                    break;
                }

                // send data to UI thread
                if (listener != null) {
                    listener.dataAvailalbe(input);
                }
            }
            in.close();
        }catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "[send manager] error at run(): " + e.getMessage());
        }finally {
            try {
                socket.close();
            }catch(IOException ex) {
                Log.d(TAG, "[send manager] error at close() in main: " + ex.getMessage());
            }
        }
    }

    /**
     * write data to output to send to other peers
     *
     * @param st
     */
    public void write(Object st) {
        try {
            outStream.write(st.toString().getBytes());
        } catch (IOException e) {
            Log.d(TAG, "[send manager] error at write(): " + e.getMessage());
        }
    }

    public void close(){
        try{
            socket.close();
        }catch(IOException e){
            Log.d(TAG, "[send manager] error at close(): " + e.getMessage());
        }
    }

    public interface DataListener {
        public void socketReady(boolean ready);
        public void dataAvailalbe(Object data);
    }
}
