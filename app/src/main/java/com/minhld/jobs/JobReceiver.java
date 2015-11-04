package com.minhld.jobs;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobReceiver extends ClassLoader implements Runnable {
    public static final int JOB_OK = 0;
    public static final int JOB_FAILED = -1;
    private Bitmap orgBmp;
    private Handler handler;

    public JobReceiver(Handler handler, Bitmap orgBitmap) {
        this.handler = handler;
        this.orgBmp = orgBitmap;
    }

    @Override
    public void run() {
        try {
            InputStream fileInputStream = this.getClass().getClassLoader().getResourceAsStream("Job.class");
            byte rawBytes[] = new byte[fileInputStream.available()];
            fileInputStream.read(rawBytes);

            Class<?> regeneratedClass = this.defineClass(rawBytes, 0, rawBytes.length);
            Bitmap result = (Bitmap)regeneratedClass.getMethod("exec", Bitmap.class).
                                        invoke(regeneratedClass.newInstance(), this.orgBmp);
            handler.obtainMessage(JOB_OK, result);
        } catch (Exception e) {
            handler.obtainMessage(JOB_FAILED, e);
        }
    }

}
