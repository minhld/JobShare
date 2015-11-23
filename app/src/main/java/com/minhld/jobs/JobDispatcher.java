package com.minhld.jobs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;

import java.io.File;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobDispatcher extends AsyncTask {
    private Activity context;
    private WifiBroadcaster broadcaster;
    private Handler socketHandler;
    String jobPath = "";
    String dataPath = "";
    boolean useCluster = true;

    public JobDispatcher(Activity c, WifiBroadcaster broadcaster, Handler socketHandler, boolean useCluster) {
        this.context = c;
        this.broadcaster = broadcaster;
        this.socketHandler = socketHandler;
        this.useCluster = useCluster;

        String downloadPath = Utils.getDownloadPath();
        jobPath = downloadPath + "/Job.jar";
        dataPath = downloadPath + "/mars.jpg";
    }

    @Override
    protected Object doInBackground(Object[] params) {
        // start sending data
        Bitmap orgBmp = null;

        if (new File(dataPath).exists()) {
            try {
                // read the bitmap from the binary data
                orgBmp = BitmapFactory.decodeFile(dataPath);
                int bmpWidth = orgBmp.getWidth();
                int bmpHeight = orgBmp.getHeight();

                // depend on number of clients, we will split the number
                JobData jobData;
                Bitmap splitBmp;
                int deviceNum = Utils.connectedDevices.size() + 1;
                // get width of each slice
                int pieceWidth = bmpWidth / deviceNum;

                for (int i = 0; i < deviceNum; i++) {
                    // create job data
                    splitBmp = Bitmap.createBitmap(orgBmp, (pieceWidth * i), 0, pieceWidth, orgBmp.getHeight());
                    jobData = new JobData(i, splitBmp, new File(jobPath));

                    // no longer need this data
                    splitBmp.recycle();

                    // and send to all the clients
                    // however it will skip the client 0, server will handle this
                    if (this.useCluster) {
                        if (i == 0) {
                            // do it at server
                            this.socketHandler.obtainMessage(Utils.MESSAGE_INFO, "[server] do own job #" + i);
                            new Thread(new JobExecutor(this.context, this.socketHandler, jobData)).start();
                        } else {
                            // dispatch this one to client to resolve it
                            // it should be 32288 bytes to be sent
                            byte[] jobBytes = jobData.toByteArray();
                            this.broadcaster.sendObject(jobBytes, i);
                        }
                    } else {
                        // do all of the tasks at server
                        this.socketHandler.obtainMessage(Utils.MESSAGE_INFO, "[server] do own job #" + i).sendToTarget();
                        new Thread(new JobExecutor(this.context, this.socketHandler, jobData)).start();
                    }
                }

                // release the original image
                orgBmp.recycle();

                publishProgress(new Integer[] { bmpWidth, bmpHeight });
            } catch (Exception e) {
                socketHandler.obtainMessage(Utils.JOB_FAILED, "[server] " + e.getMessage());
            } finally {
                if (orgBmp != null && !orgBmp.isRecycled()) {
                    orgBmp.recycle();
                }
            }
        } else {
            // no file available
            publishProgress(null);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        //super.onProgressUpdate(values);

        // sending completed
        if (values.length > 0) {
            int bmpWidth = (int) values[0];
            int bmpHeight = (int) values[1];
            socketHandler.obtainMessage(Utils.MESSAGE_READ_JOB_SENT,
                        "{ 'width': " + bmpWidth + ", 'height': " + bmpHeight + " }").sendToTarget();
        } else {
            socketHandler.obtainMessage(Utils.MESSAGE_READ_NO_FILE, "[server] data file unavailable");
        }
    }
}
