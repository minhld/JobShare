package com.minhld.supports;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends SocketHandler {
    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private ChatManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Activity c, TextView t, Handler handler, InetAddress groupOwnerAddress) {
        super(c, t);
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                            Utils.SERVER_PORT), Utils.SERVER_TIMEOUT);
            writeLog("Launching the I/O handler");
            chat = new ChatManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    @Override
    public void dispose() {
        // close socket here

    }

    public ChatManager getChat() {
        return chat;
    }
}