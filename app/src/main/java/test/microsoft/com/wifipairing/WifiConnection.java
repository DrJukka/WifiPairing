// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package test.microsoft.com.wifipairing;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiConnection implements WifiP2pManager.ConnectionInfoListener{

    WifiConnection that = this;

    Context context;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    WifiBase.WifiStatusCallBack callback;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    public WifiConnection(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack Callback) {
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
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);

        p2p.cancelConnect(channel,new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                debug_print("cancelConnect successfull" );
            }

            @Override
            public void onFailure(int errorCode) {
                debug_print("Failed cancelling the Connection : " + errorCode);
            }
        });
    }

    public void Connect(String address) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.wps.setup = WpsInfo.PBC;

        p2p.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                debug_print("Connecting to service" );
            }

            @Override
            public void onFailure(int errorCode) {
                debug_print("Failed connecting to service : " + errorCode);
            }
        });
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
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            callback.ConnectionInfoAvailable(info);
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
