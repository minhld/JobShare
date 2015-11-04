package com.minhld.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;

import com.minhld.supports.Utils;

import java.io.File;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobDispatcher extends AsyncTask {
    String jobPath = "";
    String dataPath = "";

    public JobDispatcher() {
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
            Bitmap orgBmp = BitmapFactory.decodeFile(dataPath, options);

            // depend on number of clients, we will split the number
            JobData jobData;
            Bitmap splitBmp;
            int deviceNum = Utils.connectedDevices.size();
            for (int i = 0; i < deviceNum; i++) {
                // create job data
                splitBmp = Bitmap.createBitmap(orgBmp, 0, 0, orgBmp.getWidth() / deviceNum, orgBmp.getHeight());
                jobData = new JobData(splitBmp, new File(jobPath));

                // and send

            }
            publishProgress();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);

        // sending completed
    }
}
