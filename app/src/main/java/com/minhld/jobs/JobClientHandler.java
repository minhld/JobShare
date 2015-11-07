package com.minhld.jobs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobClientHandler extends Handler {
    WifiBroadcaster mReceiver;

    public JobClientHandler(WifiBroadcaster mReceiver) {
        this.mReceiver = mReceiver;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Utils.JOB_OK: {
                // finish
                // when client completed the job and send back to server
                JobData jobData = (JobData) msg.obj;
                mReceiver.sendObject(jobData.toByteArray(), jobData.index);

                // display it on client image view
                try {
                    Bitmap pieceBmp = BitmapFactory.decodeByteArray(jobData.byteData, 0, jobData.byteData.length);
                    //mPreviewImage.setImageBitmap(pieceBmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case Utils.JOB_FAILED: {
                // send some error data
                break;
            }
        }
    }
}
