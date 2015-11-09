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
    Handler mainUiHandler;
    WifiBroadcaster mReceiver;

    public JobClientHandler(Handler uiHandler) {
        this.mainUiHandler = uiHandler;
    }

    public void setBroadcaster(WifiBroadcaster mReceiver) {
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

                try {
                    Bitmap pieceBmp = BitmapFactory.decodeByteArray(jobData.byteData, 0, jobData.byteData.length);
                    mainUiHandler.obtainMessage(Utils.MAIN_JOB_DONE, pieceBmp).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO,
                                "exception: " + e.getMessage()).sendToTarget();
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
