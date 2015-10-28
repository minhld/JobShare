package com.minhld.supports;

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
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;


/**
 * Created by minhld on 9/17/2015.
 */
public class WifiBroadcaster extends BroadcastReceiver {
    private static final int SOCKET_TIMEOUT = 8000;
    private static final int GROUP_PORT = 8888;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter mIntentFilter;

    PeerDeviceListChangeListener peerDeviceListChangeListener;
    InetAddress groupOwnerAddress;

    TextView logTxt;
    ListView deviceList;

    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    public WifiBroadcaster(Context c, ListView deviceList, TextView logTxt){
        this.mManager = (WifiP2pManager)c.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(c, c.getMainLooper(), null);

        this.deviceList = deviceList;
        this.logTxt = logTxt;
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
                        new ExchangeDataAsyncTask().execute();
                    } else if (info.groupFormed) {
                        // if current device is a client
                        writeLog("this device takes role of client, click on SAY HI button to send a message");
                        groupOwnerAddress = info.groupOwnerAddress;
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
    public void writeLog(String msg){
        this.logTxt.append(sdf.format(new Date()) + ": " + msg + "\r\n");
    }

    public class SendDataTask extends AsyncTask {
        private String sendingData = "";

        public SendDataTask(String sendingData){
            this.sendingData = sendingData;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            Socket socket = new Socket();

            String deviceName = "unknown";

            try {
                socket.bind(null);
                String hostName = groupOwnerAddress.getHostAddress();
                socket.connect(new InetSocketAddress(hostName, GROUP_PORT), SOCKET_TIMEOUT);
                OutputStream stream = socket.getOutputStream();

                String sendingData = "sending: " + this.sendingData + " from " + deviceName;
                stream.write(sendingData.getBytes());
                stream.close();
                stream = null;

                return "data sent: " + sendingData;
            } catch(IOException ex) {
                return ("exception (" + ex.getClass() + "): " + ex.getMessage());
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

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            if (result != null) {
                writeLog(result.toString());
            }
        }
    }

    /**
     * this function will send an object through socket to the server
     *
     * @param st
     */
    public void sendObject(Object st) {
        new SendDataTask("").execute();
    }

    /**
     * this class will implement a server socket to listen to client sockets
     * to receive message
     */
    public class ExchangeDataAsyncTask extends AsyncTask {

        public ExchangeDataAsyncTask() {
        }

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(GROUP_PORT);
                Socket client = serverSocket.accept();

                InputStream inputStream = client.getInputStream();
                StringWriter writer = new StringWriter();
                IOUtils.copy(inputStream, writer, "UTF-8");
                String theString = writer.toString();

                serverSocket.close();

                return theString;
            } catch (IOException e) {
                writeLog("error: " + e.getMessage());
                return null;
            }
        }

        /**
         * Start activity that can handle the JPEG image
         */
        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            if (result != null) {
                writeLog("data received: " + result);
            }
        }
    }
}
