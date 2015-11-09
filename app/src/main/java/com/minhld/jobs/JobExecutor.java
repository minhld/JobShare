package com.minhld.jobs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.minhld.supports.Utils;

import java.io.ByteArrayOutputStream;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobExecutor extends ClassLoader implements Runnable {
    Activity context;
    JobData jobData;
    private Handler handler;

    /**
     * this constructor is used in client mode, when client receives a package
     * from server. it then saves the job jar file into Download folder, and
     * execute the inside class to get the result and send back to server
     *
     * @param c
     * @param handler
     * @param jobDataBytes
     */
    public JobExecutor(Activity c, Handler handler, ByteArrayOutputStream jobDataBytes) {
        this.context = c;
        this.handler = handler;

        try {
            this.jobData = (JobData) Utils.deserialize(jobDataBytes.toByteArray());
            // save the job class to Download folder
            String outputJobFilePath = Utils.getDownloadPath() + "/" + Utils.JOB_FILE_NAME;
            Utils.writeFile(outputJobFilePath, this.jobData.jobClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this constructor is used by server mode, when server try to use it for running
     * the first piece of an object. the app will check if the job jar file exists in
     * the Download folder, and use it to run the very first result
     *
     * @param c
     * @param handler
     * @param jobData
     */
    public JobExecutor(Activity c, Handler handler, JobData jobData) {
        this.context = c;
        this.handler = handler;
        this.jobData = jobData;
    }

    @Override
    public void run() {
        try {
            // get the original bitmap
            Bitmap orgBmp = BitmapFactory.decodeByteArray(jobData.byteData, 0, jobData.byteData.length);

            // initiate the Job algorithm class & execute it
            String jobPath = Utils.getDownloadPath() + "/" + Utils.JOB_FILE_NAME;
            Bitmap result = (Bitmap) Utils.runRemote(this.context, jobPath, orgBmp);

            // send this result to server
            JobData jobResult = new JobData(this.jobData.index, result, new byte[0]);
            handler.obtainMessage(Utils.JOB_OK, jobResult).sendToTarget();

        } catch (Exception e) {
            handler.obtainMessage(Utils.JOB_FAILED, e);
        }
    }

}
