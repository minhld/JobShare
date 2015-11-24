package com.minhld.jobs;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;

import com.minhld.jobshare.R;
import com.minhld.supports.Utils;
import com.minhld.supports.WifiBroadcaster;
import com.minhld.supports.WifiPeerListAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * this class handles
 *
 * Created by minhd on 11/23/2015.
 */
public class JobHandler {
    Activity context;
    Handler uiHandler;

    JobDataParser dataParser;
    JobClientHandler clientHandler;
    JobServerHandler serverHandler;

    WifiBroadcaster mReceiver;
    IntentFilter mIntentFilter;

    WifiPeerListAdapter deviceListAdapter;
    List<WifiP2pDevice> peerArrayList = new ArrayList<>();

    JobListener jobListener;
    public void setJobListener(JobListener jobListener) {
        this.jobListener = jobListener;
    }

    public JobHandler(Activity c, Handler uiHandler, JobDataParser dataParser) {
        this.context = c;
        this.uiHandler = uiHandler;
        this.dataParser = dataParser;

        clientHandler = new JobClientHandler(uiHandler);
        serverHandler = new JobServerHandler(this.context, uiHandler, clientHandler, dataParser);

        // configure wifi receiver
        mReceiver = new WifiBroadcaster(this.context);
        mReceiver.setBroadCastListener(new BroadcastUpdatesHandler());
        mReceiver.setSocketHandler(serverHandler);

        clientHandler.setBroadcaster(mReceiver);

        discoverPeers();

        // configure the device list
        deviceListAdapter = new WifiPeerListAdapter(this.context, R.layout.row_devices, peerArrayList, mReceiver);

    }

    /**
     * discover the peers in the WiFi peer-to-peer mobile network
     */
    public void discoverPeers() {
        // start discovering
        mReceiver.discoverPeers();
        mIntentFilter = mReceiver.getSingleIntentFilter();
    }

    /**
     * split the job into tasks, and dispatch to other peers
     *
     * @param useCluster
     */
    public void dispatchJob(boolean useCluster) {
        new JobDispatcher(context, mReceiver, serverHandler, dataParser, useCluster).execute();
    }

    /**
     * this should be added at the end of onPause on main activity
     */
    public void actOnPause() {
        if (mReceiver != null && mIntentFilter != null) {
            this.context.unregisterReceiver(mReceiver);
        }
    }

    /**
     * this should be added at the end of onResume on main activity
     */
    public void actOnResume() {
        if (mReceiver != null && mIntentFilter != null) {
            this.context.registerReceiver(mReceiver, mIntentFilter);
        }
    }

    public WifiPeerListAdapter getDeviceListAdapter() {
        return deviceListAdapter;
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
            if (jobListener != null) {
                jobListener.socketUpdated(socketType == Utils.SocketType.SERVER, connected);
            }
        }
    }

    public interface JobListener {
        public void socketUpdated(boolean isServer, boolean isConnected);
    }
}
