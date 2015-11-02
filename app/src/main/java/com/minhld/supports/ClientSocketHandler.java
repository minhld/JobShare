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
    private Handler handler;
    private ChatManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Activity c, TextView t, Handler handler, InetAddress groupOwnerAddress) {
        super(c, t);
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
        this.socketType = socketType.CLIENT;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            // initiate client socket
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                            Utils.SERVER_PORT), Utils.SERVER_TIMEOUT);
            writeLog("[client] launching the I/O handler");

            // connect it to a chat manager
            chat = new ChatManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            writeLog("[client] exception: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    @Override
    public void write(Object data) {
        chat.write(data.toString().getBytes());
    }

    @Override
    public void dispose() {
        // close socket here

    }

    @Override
    public boolean isSocketWorking() {
        return true;
    }

    public ChatManager getChat() {
        return chat;
    }
}