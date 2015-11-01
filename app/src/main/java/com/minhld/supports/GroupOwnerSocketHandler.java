package com.minhld.supports;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends SocketHandler {
    ServerSocket socket = null;
    private final int THREAD_COUNT = 10;
    private Handler handler;
    private static final String TAG = "GroupOwnerSocketHandler";

    // A ThreadPool for client sockets.
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public GroupOwnerSocketHandler(Activity c, TextView t, Handler handler) throws IOException {
        super(c, t);

        try {
            socket = new ServerSocket(Utils.SERVER_PORT);
            this.handler = handler;
            writeLog("[server] socket started");
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a ChatManager instance when
                // there is a new connection
                pool.execute(new ChatManager(socket.accept(), handler));
                writeLog("[server] launching I/O handler");
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) { }

                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

    @Override
    public void dispose() {
        try {
            // shutdown the thread pool and socket
            pool.shutdownNow();
            socket.close();

        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
