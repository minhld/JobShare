package com.minhld.jobshare;

import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.minhld.jobs.JobData;
import com.minhld.jobs.JobDispatcher;
import com.minhld.jobs.JobExecutor;
import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;
import com.minhld.supports.WifiPeerListAdapter;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.broadcastBtn)
    Button broadcastBtn;

    @Bind(R.id.sayHiBtn)
    Button sayHiBtn;

    @Bind(R.id.infoTxt)
    TextView infoText;

    @Bind(R.id.deviceList)
    ListView deviceList;

    @Bind(R.id.viewFlipper)
    ViewFlipper mViewFlipper;

    @Bind(R.id.previewImage)
    ImageView mPreviewImage;

    WifiBroadcaster mReceiver;
    IntentFilter mIntentFilter;

    WifiPeerListAdapter deviceListAdapter;
    List<WifiP2pDevice> peerArrayList = new ArrayList<>();

    // this bitmap will be used as the placeholder to merge the parts sent from clients
    Bitmap finalBitmap = null;

    // this will listen to the events happened with the main socket (server or client)
    Handler socketHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_READ_CLIENT: {
                    // client received job, will send the result here
                    ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;

                    // run the job, result will be thrown to client executor handler
                    new Thread(new JobExecutor(clientExecutorHandler, readBuf)).start();
                    break;
                }
                case Utils.MESSAGE_READ_SERVER: {
                    // server received client result, will merge the results here
                    JobData clientJobResult = (JobData) msg.obj;
                    int imgIndex = clientJobResult.index;
                    try {
                        Bitmap partBmp = (Bitmap) Utils.deserialize(clientJobResult.data);
                        Canvas canvas = new Canvas(finalBitmap);
                        //canvas.drawBitmap(partBmp, imgIndex * partBmp.getWidth(), 0, partBmp.getWidth(), partBmp.getHeight());
                        // also display it partially
                        mPreviewImage.setImageBitmap(finalBitmap);
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
                    Utils.writeLog(MainActivity.this, infoText, "me: " + obj);
                    break;
                }

            }

        }
    };

    // this will listen to the client job executor
    Handler clientExecutorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case JobExecutor.JOB_OK: {
                    // when client completed the job and send back to server
                    JobData jobData = (JobData) msg.obj;
                    mReceiver.sendObject(jobData.data, jobData.index);
                    // display it on client image view
                    try {
                        Bitmap pieceBmp = (Bitmap) Utils.deserialize(jobData.data);
                        mPreviewImage.setImageBitmap(pieceBmp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case JobExecutor.JOB_FAILED: {
                    // send some error data
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        // configure wifi receiver
        mReceiver = new WifiBroadcaster(this, deviceList, infoText);
        mReceiver.setBroadCastListener(new BroadcastUpdatesHandler());
        mReceiver.setSocketHandler(socketHandler);

        // start discovering
        mReceiver.discoverPeers();
        mIntentFilter = mReceiver.getSingleIntentFilter();

        // configure the device list
        deviceListAdapter = new WifiPeerListAdapter(this, R.layout.row_devices, peerArrayList, mReceiver);
        deviceList.setAdapter(deviceListAdapter);

        sayHiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mReceiver.sendObject("hello!");
                // 1. dispatch jobs to clients
                new JobDispatcher(mReceiver, socketHandler).execute();

                // 2. listen to client responses

            }
        });

        // the status button will only be available when the socket is enabled
        sayHiBtn.setEnabled(false);

        broadcastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReceiver.writeLog("attempting discover...");
                mReceiver.discoverPeers();
                mIntentFilter = mReceiver.getSingleIntentFilter();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mReceiver != null && mIntentFilter != null) {
            registerReceiver(mReceiver, mIntentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReceiver != null && mIntentFilter != null) {
            unregisterReceiver(mReceiver);
        }
    }

    /**
     * this class handles the device list when it is updated
     */
    private class BroadcastUpdatesHandler implements WifiBroadcaster.BroadCastListener {
        @Override
        public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList) {
            deviceListAdapter.clear();
            deviceListAdapter.addAll(deviceList);
            deviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void socketUpdated(final Utils.SocketType socketType, final boolean connected) {
            // enable/disable the "Say Hi" button when its status changed
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // only server can send jobs to client, client cannot send job to server
                    // but client will send job result to server
                    if (socketType == Utils.SocketType.SERVER) {
                        sayHiBtn.setEnabled(connected);
                    }
                }
            });

        }
    }

}
