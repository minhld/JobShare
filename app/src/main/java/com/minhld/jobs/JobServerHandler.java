package com.minhld.jobs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;

import com.minhld.jobshare.MainActivity;
import com.minhld.supports.Utils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobServerHandler extends Handler {

    Activity parent;
    JobClientHandler clientHandler;
    Handler mainUiHandler;
    Bitmap finalBitmap;

    public JobServerHandler(Activity parent, Handler uiHandler, JobClientHandler clientHandler) {
        this.parent = parent;
        this.mainUiHandler = uiHandler;
        this.clientHandler = clientHandler;
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
                new Thread(new JobExecutor(parent, clientHandler, readBuf)).start();
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

                    int imgIndex = clientJobResult.index;
                    Bitmap partBmp = BitmapFactory.decodeByteArray(
                            clientJobResult.byteData, 0, clientJobResult.byteData.length);
                    drawBitmap(partBmp, finalBitmap, imgIndex);

                    // also display it partially
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server] received data from client [" + imgIndex + "]").sendToTarget();
                    mainUiHandler.obtainMessage(Utils.MAIN_JOB_DONE, finalBitmap).sendToTarget();
                } catch (Exception e) {
                    ((MainActivity) parent).writeLog("server-error", e);
                }
                break;
            }
            case Utils.MESSAGE_READ_JOB_SENT: {
                // when job is dispatched, a placeholder bitmap will be created
                // to accumulate the results from clients
                String jsonData = (String) msg.obj;
                try {
                    JSONObject resultObj = new JSONObject(jsonData);
                    int width = resultObj.getInt("width"), height = resultObj.getInt("height");
                    finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (Exception e) {
                    ((MainActivity) parent).writeLog("server-error", e);
                }
                break;
            }
            case Utils.JOB_FAILED: {
                String exStr = (String) msg.obj;
                ((MainActivity) parent).writeLog(exStr);
                break;
            }
            case Utils.MY_HANDLE: {
                // self instruction, don't care
                Object obj = msg.obj;
                // disable printing out me recognition
                //Utils.writeLog(parent, infoText, "me: " + obj);
                break;
            }

        }
    }

    /**
     * draw the piece bitmap on our canvas. This one will also remove the source
     * bitmap once it is drawn
     *
     * @param source
     * @param dest
     * @param index
     */
    private void drawBitmap(Bitmap source, Bitmap dest, int index) {
        int pieceWidth = source.getWidth();
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, index * pieceWidth, 0, null);
    }
}
