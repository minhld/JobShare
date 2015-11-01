package com.minhld.supports;

import android.app.Activity;
import android.widget.TextView;

/**
 * Created by minhld on 11/1/2015.
 */
public abstract class SocketHandler extends Thread {
    protected Activity mContext;
    protected TextView mLogText;

    public SocketHandler() {}

    public SocketHandler(Activity c, TextView t) {
        this.mContext = c;
        this.mLogText = t;
    }

    public abstract void dispose();

    public void writeLog(String msg) {
        Utils.writeLog(mContext, mLogText, msg);
    }
}
