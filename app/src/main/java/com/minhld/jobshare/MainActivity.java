package com.minhld.jobshare;

import android.content.IntentFilter;
import android.graphics.Bitmap;
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

import com.minhld.jobs.JobClientHandler;
import com.minhld.jobs.JobDispatcher;
import com.minhld.jobs.JobServerHandler;
import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;
import com.minhld.supports.WifiPeerListAdapter;

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

    @Bind(R.id.closeViewBtn)
    Button closeViewBtn;

    WifiBroadcaster mReceiver;
    IntentFilter mIntentFilter;

    WifiPeerListAdapter deviceListAdapter;
    List<WifiP2pDevice> peerArrayList = new ArrayList<>();

    // this bitmap will be used as the placeholder to merge the parts sent from clients
    Bitmap finalBitmap = null;

    // this will listen to the client job executor
    JobClientHandler clientExecutorHandler = null;
    // this will listen to the events happened with the main socket (server or client)
    JobServerHandler socketHandler = null;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MAIN_JOB_DONE: {
                    Bitmap bmp = (Bitmap) msg.obj;
                    mPreviewImage.setImageBitmap(bmp);
                    //mViewFlipper.showNext();
                    mViewFlipper.setDisplayedChild(1);
                    break;
                }
                case Utils.MAIN_INFO: {
                    String strMsg = (String) msg.obj;
                    Utils.writeLog(MainActivity.this, infoText, strMsg);
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

        // handlers registration
        clientExecutorHandler = new JobClientHandler(mainUiHandler);
        socketHandler = new JobServerHandler(this, mainUiHandler, clientExecutorHandler);

        // configure wifi receiver
        mReceiver = new WifiBroadcaster(this, deviceList, infoText);
        mReceiver.setBroadCastListener(new BroadcastUpdatesHandler());
        mReceiver.setSocketHandler(socketHandler);

        clientExecutorHandler.setBroadcaster(mReceiver);

        // start discovering
        mReceiver.discoverPeers();
        mIntentFilter = mReceiver.getSingleIntentFilter();

        // configure the device list
        deviceListAdapter = new WifiPeerListAdapter(this, R.layout.row_devices, peerArrayList, mReceiver);
        deviceList.setAdapter(deviceListAdapter);

        sayHiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. dispatch jobs to clients
                new JobDispatcher(MainActivity.this, mReceiver, socketHandler).execute();
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

        closeViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewFlipper.showNext();
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
