// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package test.microsoft.com.wifipairing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */

public class WifiServiceSearcher {

    private Context context;

    private final WifiBase.WifiStatusCallBack callback;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener;


    public WifiServiceSearcher(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack handler) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = handler;

        serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {
                debug_print("Service found");
                if (serviceType.startsWith(WifiBase.SERVICE_TYPE)) {
                    callback.gotService(new ServiceItem(instanceName, serviceType, device.deviceAddress, device.deviceName));
                }
            }
        };

        p2p.setDnsSdResponseListeners(channel, serviceListener, null);

    }

    public void Start() {
        Stop();
        startServiceDiscovery();
    }

    public void Stop() {
        stopDiscovery();
    }

    private void startServiceDiscovery() {

        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(WifiBase.SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            public void onSuccess() {
                debug_print("Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                debug_print("Started service discovery");
                            }
                            public void onFailure(int reason) {
                                stopDiscovery();
                                debug_print("Starting service discovery failed");
                            }
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                debug_print("Adding service request failed");
            }
        });
    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {debug_print("Cleared service requests");}
            public void onFailure(int reason) {debug_print("Clearing service requests failed");}
        });
    }
    private void debug_print(String buffer) {
        if(callback != null) {
            callback.debug("SS",buffer);
        }
        Log.i("Service searcher", buffer);
    }
}
