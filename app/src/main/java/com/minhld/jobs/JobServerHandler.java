package com.minhld.jobs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;

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
                this.mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[client] received a job from server").sendToTarget();

                // run the job, result will be thrown to client executor handler
                new Thread(new JobExecutor(parent, clientHandler, readBuf)).start();
                break;
            }
            case Utils.MESSAGE_READ_SERVER: {
                // server received client result, will merge the results here
                ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;

                try {
                    JobData clientJobResult = (JobData) Utils.deserialize(readBuf.toByteArray());
                    int imgIndex = clientJobResult.index;
                    Bitmap partBmp = BitmapFactory.decodeByteArray(
                            clientJobResult.byteData, 0, clientJobResult.byteData.length);
                    Canvas canvas = new Canvas(finalBitmap);
                    //canvas.drawBitmap(partBmp, imgIndex * partBmp.getWidth(), 0, partBmp.getWidth(), partBmp.getHeight());
                    // also display it partially
                    //mPreviewImage.setImageBitmap(finalBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case Utils.MESSAGE_READ_JOB_SENT: {
                // when job is dispatched, a placeholder bitmap will be created
                // to accumulate the results from clients
                String jsonData = (String) msg.obj;
                try {
                    JSONObject resultObj = new JSONObject(jsonData);
                    int width = resultObj.getInt("width"),
                            height = resultObj.getInt("height");
                    finalBitmap = Bitmap.createBitmap(width, height, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
}
