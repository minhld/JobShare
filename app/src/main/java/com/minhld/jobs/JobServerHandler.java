package com.minhld.jobs;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import com.minhld.supports.Utils;

import java.io.ByteArrayOutputStream;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobServerHandler extends Handler {

    Activity parent;
    JobClientHandler clientHandler;
    Handler mainUiHandler;
    JobDataParser dataParser;
    Object finalObject;

    public JobServerHandler(Activity parent, Handler uiHandler, JobClientHandler clientHandler, JobDataParser dataParser) {
        this.parent = parent;
        this.mainUiHandler = uiHandler;
        this.clientHandler = clientHandler;
        this.dataParser = dataParser;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Utils.MESSAGE_READ_CLIENT: {
                // client received job, will send the result here
                ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;

                // print out that it received a job from server
                this.mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[client] received a job from server. running... ").sendToTarget();

                // run the job, result will be thrown to client executor handler
                new Thread(new JobExecutor(parent, clientHandler, dataParser, readBuf)).start();
                break;
            }
            case Utils.MESSAGE_READ_SERVER: {
                // server received client result, will merge the results here
                JobData clientJobResult = null;

                try {
                    if (msg.obj instanceof JobData) {
                        // this case happens when server finishes its own job
                        clientJobResult = (JobData) msg.obj;
                    } else {
                        // this case happens when server receives a result from client - in
                        // binary array
                        ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;
                        clientJobResult = (JobData) Utils.deserialize(readBuf.toByteArray());
                    }

                    dataParser.mergeParts(finalObject, clientJobResult.byteData, clientJobResult.index);

                    // also display it partially
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server] received data from client [" + clientJobResult.index + "]").sendToTarget();
                    mainUiHandler.obtainMessage(Utils.MAIN_JOB_DONE, finalObject).sendToTarget();
                } catch (Exception e) {
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server-error] " + e.getMessage()).sendToTarget();
                }
                break;
            }
            case Utils.MESSAGE_READ_JOB_SENT: {
                // when job is dispatched, a placeholder bitmap will be created
                // to accumulate the results from clients
                String jsonData = (String) msg.obj;
                finalObject = dataParser.buildFinalObjectFromMetadata(jsonData);
                break;
            }
            case Utils.JOB_FAILED: {
                String exStr = (String) msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, exStr).sendToTarget();
                break;
            }
            case Utils.MESSAGE_INFO: {
                // self instruction, don't care
                Object obj = msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, msg.obj + "").sendToTarget();
                break;
            }

        }
    }

}
