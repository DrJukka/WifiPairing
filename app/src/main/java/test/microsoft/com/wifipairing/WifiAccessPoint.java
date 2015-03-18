// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package test.microsoft.com.wifipairing;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiAccessPoint implements WifiP2pManager.ConnectionInfoListener,WifiP2pManager.GroupInfoListener{

    WifiAccessPoint that = this;

    Context context;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    WifiBase.WifiStatusCallBack callback;

    String mNetworkName = "";
    String mPassphrase = "";
    String mInetAddress = "";

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    public WifiAccessPoint(Context Context,WifiP2pManager Manager,WifiP2pManager.Channel Channel,WifiBase.WifiStatusCallBack Callback) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = Callback;
    }

    public void Start() {

        receiver = new AccessPointReceiver();
        filter = new IntentFilter();
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.context.registerReceiver(receiver, filter);

        p2p.createGroup(channel,new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                    debug_print("Creating Local Group ");
                }

            public void onFailure(int reason) {
                debug_print("Local Group failed, error code " + reason);
            }
        });
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
        stopLocalServices();
        removeGroup();
    }


    public void removeGroup() {
        p2p.removeGroup(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Cleared Local Group ");
            }

            public void onFailure(int reason) {
                debug_print("Clearing Local Group failed, error code " + reason);
            }
        });
    }



    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        try {
            callback.GroupInfoAvailable(group);

            if(mNetworkName.equals(group.getNetworkName()) && mPassphrase.equals(group.getPassphrase())){
                debug_print("Already have local service for " + mNetworkName + " ," + mPassphrase);
            }else {
                mNetworkName = group.getNetworkName();
                mPassphrase = group.getPassphrase();
                startLocalService("NI:" + group.getNetworkName() + ":" + group.getPassphrase() + ":" + mInetAddress);
            }
        } catch(Exception e) {
            debug_print("onGroupInfoAvailable, error: " + e.toString());
        }
    }

    private void startLocalService(String instance) {

        Map<String, String> record = new HashMap<String, String>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance( instance, WifiBase.SERVICE_TYPE, record);

        debug_print("Add local service :" + instance);
        p2p.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Added local service");
            }

            public void onFailure(int reason) {
                debug_print("Adding local service failed, error code " + reason);
            }
        });
    }


    private void stopLocalServices() {
        mNetworkName = "";
        mPassphrase = "";

        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Cleared local services");
            }

            public void onFailure(int reason) {
                debug_print("Clearing local services failed, error code " + reason);
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            callback.ConnectionInfoAvailable(info);

            if (info.isGroupOwner) {
                p2p.requestGroupInfo(channel,this);
            } else {
                debug_print("we are client !! group owner address is: " + info.groupOwnerAddress.getHostAddress());
            }
        } catch(Exception e) {
            debug_print("onConnectionInfoAvailable, error: " + e.toString());
        }
    }

    private void debug_print(String buffer) {

        callback.debug("AP",buffer);
        Log.d("WifiAccessPoint",buffer);
    }

    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    debug_print("We are connected, will check info now");
                    p2p.requestConnectionInfo(channel, that);
                } else{
                    debug_print("We are DIS-connected");
                }
            }
        }
    }
}
