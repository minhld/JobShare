package com.minhld.supports;

import android.os.Handler;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by minhld on 10/23/2015.
 */
public class ChatManager implements Runnable {
    private static final String TAG = "ChatHandler";

    private Utils.SocketType socketType;
    private Socket socket = null;
    private Handler handler;

    private InputStream iStream;
    private OutputStream oStream;

    public ChatManager(Utils.SocketType socketType, Socket socket, Handler handler) {
        this.socketType = socketType;
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int readCount = 0;
            handler.obtainMessage(Utils.MY_HANDLE, "OK").sendToTarget();
            ByteArrayOutputStream byteStream = null;

            while (true) {
                try {
                    byteStream = new ByteArrayOutputStream();

                    byte[] data = IOUtils.toByteArray(iStream);

//                    // read from the input stream
//                    while ((readCount = iStream.read(buffer)) >= 0) {
//                        byteStream.write(buffer, 0, readCount);
//                        if (readCount < buffer.length) { break; }
//                    }
//                    do {
//                        readCount = iStream.read(buffer);
//                        if (readCount == -1) {
//                            // when the connection is lost
//                            return;
//                        }
//                        byteStream.write(buffer, 0, readCount);
//                    } while (iStream.available() > 0);

                    // Send the obtained bytes to the UI Activity
                    if (socketType == Utils.SocketType.SERVER) {
                        handler.obtainMessage(Utils.MESSAGE_READ_SERVER, byteStream);
                    } else {
                        handler.obtainMessage(Utils.MESSAGE_READ_CLIENT, data);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
