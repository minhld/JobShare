package com.minhld.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.supports.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobExecutor extends ClassLoader implements Runnable {
    public static final int JOB_OK = 0;
    public static final int JOB_FAILED = -1;
    private Bitmap orgBmp;
    JobData jobData;
    private Handler handler;

    public JobExecutor(Handler handler, ByteArrayOutputStream byteStream) {
        this.handler = handler;

        try {
            this.jobData = (JobData) Utils.deserialize(byteStream.toByteArray());
            this.orgBmp = BitmapFactory.decodeByteArray(jobData.data, 0, jobData.data.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            //InputStream fileInputStream = this.getClass().getClassLoader().getResourceAsStream("Job.class");
            InputStream fileInputStream = new ByteArrayInputStream(this.jobData.jobClass);

            byte rawBytes[] = new byte[fileInputStream.available()];
            fileInputStream.read(rawBytes);

            Class<?> regeneratedClass = this.defineClass(rawBytes, 0, rawBytes.length);
            Bitmap result = (Bitmap)regeneratedClass.getMethod("exec", Bitmap.class).
                                        invoke(regeneratedClass.newInstance(), this.orgBmp);

            JobData resultJob = new JobData();
            resultJob.data = Utils.serialize(result);
            resultJob.index = this.jobData.index;

            // send this result to server
            handler.obtainMessage(JOB_OK, resultJob);

        } catch (Exception e) {
            handler.obtainMessage(JOB_FAILED, e);
        }
    }

}
