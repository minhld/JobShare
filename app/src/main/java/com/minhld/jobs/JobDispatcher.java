package com.minhld.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;

import java.io.File;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobDispatcher extends AsyncTask {
    private WifiBroadcaster broadcaster;
    private Handler socketHandler;
    String jobPath = "";
    String dataPath = "";
    int bmpWidth = 0, bmpHeight = 0;

    public JobDispatcher(WifiBroadcaster broadcaster, Handler socketHandler) {
        this.broadcaster = broadcaster;
        this.socketHandler = socketHandler;

        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        jobPath = downloadPath + "/Job.class";
        dataPath = downloadPath + "/mars.jpg";
    }

    @Override
    protected Object doInBackground(Object[] params) {
        // start sending data
        if (new File(dataPath).exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            // read the bitmap from the binary data
            Bitmap orgBmp = BitmapFactory.decodeFile(dataPath, options);
            this.bmpWidth = orgBmp.getWidth();
            this.bmpHeight = orgBmp.getHeight();

            // depend on number of clients, we will split the number
            JobData jobData;
            Bitmap splitBmp;
            int deviceNum = Utils.connectedDevices.size() + 1;
            // get width of each slice
            int pieceWidth = this.bmpWidth / deviceNum;

            for (int i = 0; i < deviceNum; i++) {
                // create job data
                splitBmp = Bitmap.createBitmap(orgBmp, (pieceWidth * i), 0, pieceWidth, orgBmp.getHeight());
                jobData = new JobData(i, splitBmp, new File(jobPath));

                // and send to all the clients
                // however it will skip the client 0, server will handle this
                if (i == 0) {
                    // do it at server
                    new Thread(new JobExecutor(this.socketHandler, jobData)).start();
                } else {
                    // dispatch this one to client to resolve it
                    this.broadcaster.sendObject(jobData.toByteArray(), i);
                }
            }

            publishProgress(orgBmp);
        } else {
            // no file available
            publishProgress(null);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);

        // sending completed
        if (values.length > 0 && values[0] != null) {
            socketHandler.obtainMessage(Utils.MESSAGE_READ_JOB_SENT, "{ 'width': " + bmpWidth + ", 'height': " + bmpHeight + " }");
        } else {
            socketHandler.obtainMessage(Utils.MESSAGE_READ_NO_FILE, "data file unavailable");
        }
    }
}
