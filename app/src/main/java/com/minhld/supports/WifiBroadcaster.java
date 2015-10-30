package com.minhld.supports;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Created by minhld on 9/17/2015.
 */
public class WifiBroadcaster extends BroadcastReceiver {
    private static final int SOCKET_TIMEOUT = 8000;
    private static final int GROUP_PORT = 8881;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter mIntentFilter;

    PeerDeviceListChangeListener peerDeviceListChangeListener;

    Activity mContext;
    TextView logTxt;
    ListView deviceList;

    SendManager sendManager;

    public WifiBroadcaster(Activity c, ListView deviceList, TextView logTxt){
        this.mContext = c;
        this.deviceList = deviceList;
        this.logTxt = logTxt;

        this.mManager = (WifiP2pManager)c.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(c, c.getMainLooper(), null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                writeLog("wifi p2p is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                writeLog("wifi p2p is disabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
                    writeLog(deviceList.size() + " devices was found");
                    if (peerDeviceListChangeListener != null){
                        peerDeviceListChangeListener.peerDeviceListUpdated(deviceList);
                    }
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            writeLog("device's connection changed");

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    if (info.groupFormed && info.isGroupOwner) {
                        // if current device is a server
                        writeLog("this device takes role of server, start listening...");
                        new ServerTask().start();
                    } else if (info.groupFormed) {
                        // if current device is a client
                        writeLog("this device takes role of client...");
                        new ClientTask(info.groupOwnerAddress.getHostAddress()).start();
                    }
                }
            });

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            writeLog("device's wifi state changed");
        }

    }

    /**
     * send the request
     */
    public void discoverPeers(){
        this.mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                writeLog("discovery called successfully");
            }

            @Override
            public void onFailure(int reason) {
                switch (reason){
                    case 0: {
                        writeLog("discovery failed: operation failed due to an internal error");
                        break;
                    }
                    case 1: {
                        writeLog("discovery failed: p2p is unsupported on the device");
                        break;
                    }
                    case 2: {
                        writeLog("discovery failed: framework is busy and unable to service the request");
                        break;
                    }
                }
            }
        });
    }

    /**
     * this function creates an intent filter to only select the intents the broadcast
     * receiver checks for
     *
     * @return
     */
    public IntentFilter getSingleIntentFilter(){
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }

        return mIntentFilter;
    }

    /**
     * disconnect with a peer
     *
     * @param deviceName
     * @param listener
     */
    public void disconnect(final String deviceName, final WifiP2pConnectionListener listener){
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                writeLog("disconnected with " + deviceName + " successfully");
                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("disconnected from " + deviceName + " failed");
            }
        });
    }

    public void connectToADevice(final WifiP2pDevice device, final WifiP2pConnectionListener listener) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                writeLog("connection established with " + device.deviceName + " successfully");

                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("connection with " + device.deviceName + " failed");
                if (listener != null) {
                    listener.connectInfoReturned(reason);
                }
            }
        });
    }

    public WifiP2pManager getWifiP2pManager() {
        return this.mManager;
    }

    public void setPeerDeviceListChangeListener(PeerDeviceListChangeListener pdlcListener){
        this.peerDeviceListChangeListener = pdlcListener;
    }

    public interface PeerDeviceListChangeListener {
        public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList);
    }

    /**
     * write log to an output
     *
     * @param msg
     */
    public void writeLog(final String msg){
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTxt.append(sdf.format(new Date()) + ": " + msg + "\r\n");
            }
        });
    }

    /**
     * this class will implement a server socket to listen to client sockets
     * to receive message
     */
    public class ServerTask extends Thread {
        ServerSocket socket = null;
        private final int THREAD_COUNT = 10;
        /**
         * A ThreadPool for client sockets.
         */
        private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
                        THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());

        public ServerTask() { }

        @Override
        public void run() {
            try {
                socket = new ServerSocket(GROUP_PORT);
                writeLog("server socket started");

                while (true) {

                    // A blocking operation. Initiate a ChatManager instance when
                    // there is a new connection
                    sendManager = new SendManager(socket.accept(), new SendManager.DataListener() {
                        @Override
                        public void available(Object data) {
                            // code will be added here
                            writeLog("server received " + data.toString());
                        }
                    });
                    pool.execute(sendManager);
                }
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {
                }
                e.printStackTrace();
                writeLog("error: " + e.getMessage());
                pool.shutdownNow();
            }
        }

        public ServerSocket getServerSocket() {
            return socket;
        }
    }

    public class ClientTask extends Thread {
        String hostAddress;

        public ClientTask(String address){
            this.hostAddress = address;
        }

        public void run() {
            Socket socket = new Socket();

            try {
                // open client socket and
                socket.bind(null);
                socket.connect(new InetSocketAddress(hostAddress, GROUP_PORT), SOCKET_TIMEOUT);

                // listen on new client socket on a new thread
                sendManager = new SendManager(socket, new SendManager.DataListener() {
                    @Override
                    public void available(Object data) {
                        // when data is available at client
                        writeLog("client received " + data.toString());
                    }
                });
                sendManager.start();
            } catch(IOException e) {
                writeLog("error: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                        socket = null;
                    } catch(IOException e) {
                        // noway it goes this line
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * this function will send an object through socket to the server
     *
     * @param st
     */
    public void sendObject(Object st) {
        sendManager.write("hello");
    }


}
