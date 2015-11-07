package com.minhld.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.minhld.supports.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobExecutor extends ClassLoader implements Runnable {
    JobData jobData;
    private Handler handler;

    public JobExecutor(Handler handler, ByteArrayOutputStream jobDataBytes) {
        this.handler = handler;

        try {
            this.jobData = (JobData) Utils.deserialize(jobDataBytes.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JobExecutor(Handler handler, JobData jobData) {
        this.handler = handler;
        this.jobData = jobData;
    }

    @Override
    public void run() {
        try {
            // initiate the Job algorithm class
            Class<?> regeneratedClass = this.defineClass(this.jobData.jobClass, 0, this.jobData.jobClass.length);

            // get the original bitmap
            Bitmap orgBmp = BitmapFactory.decodeByteArray(jobData.byteData, 0, jobData.byteData.length);


            // run the algorithm on the original bitmap
            Bitmap result = (Bitmap)regeneratedClass.getMethod("exec", Bitmap.class).
                                        invoke(regeneratedClass.newInstance(), orgBmp);

            JobData resultJob = new JobData();
            resultJob.index = this.jobData.index;
//            resultJob.byteData = ;

            // send this result to server
            handler.obtainMessage(Utils.JOB_OK, resultJob);

        } catch (Exception e) {
            handler.obtainMessage(Utils.JOB_FAILED, e);
        }
    }

}
