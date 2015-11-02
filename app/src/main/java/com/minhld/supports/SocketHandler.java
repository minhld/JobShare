package com.minhld.supports;

import android.app.Activity;
import android.widget.TextView;

/**
 * Created by minhld on 11/1/2015.
 */
public abstract class SocketHandler extends Thread {
    protected enum SocketType {
        SERVER,
        CLIENT
    }
    protected Activity mContext;
    protected TextView mLogText;
    protected SocketType socketType;

    public SocketHandler() {}

    public SocketHandler(Activity c, TextView t) {
        this.mContext = c;
        this.mLogText = t;
        this.socketType = SocketType.SERVER;
    }

    /**
     * write some piece of data into the socket to dispatch to peers
     *
     * @param data
     */
    public abstract void write(Object data);

    /**
     * to dispose current socket. this only works on server socket.
     */
    public abstract void dispose();

    /**
     * check if socket is still working.
     * this only works on server socket. on client socket, it will always return true
     *
     * @return
     */
    public abstract boolean isSocketWorking();

    public void writeLog(String msg) {
        utils.writeLog(mContext, mLogText, msg);
    }
}
