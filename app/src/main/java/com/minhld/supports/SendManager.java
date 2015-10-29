package com.minhld.supports;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by minhld on 10/28/2015.
 */
public class SendManager extends Thread {
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;

    private DataListener listener;

    public SendManager(Socket sk, DataListener listener) {
        this.socket = sk;
        this.listener = listener;
        System.out.println("new connection with client " + this.socket);
    }

    public void run() {

        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

            while (true) {
                String input = in.readLine();
                if (input == null || input.equals(".")) {
                    break;
                }

                // print this to output stream
                if (listener != null) {
                    listener.available(input);
                }
            }
            in.close();
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            }catch(IOException ioEx) {
                //
            }
        }
    }

    public void write(Object st) {
        try {
            outStream.write(st.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface DataListener {
        public void available(Object data);
    }
}
