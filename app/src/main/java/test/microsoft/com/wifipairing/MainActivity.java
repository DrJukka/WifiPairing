
// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package test.microsoft.com.wifipairing;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity implements WifiBase.WifiStatusCallBack {

    MainActivity that = this;
    WifiBase mWifiBase = null;
    WifiConnection mWifiConnection = null;
    WifiServiceSearcher mWifiServiceSearcher = null;
    WifiAccessPoint mWifiAccessPoint = null;

    List<ServiceItem> serviceItemList = new ArrayList<ServiceItem>();
    ProgressDialog mProgressDlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button reFreshBut = (Button) findViewById(R.id.reFreshBut);
        reFreshBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWifiBase != null) {

                    //Reset The listview
                    ListView listView = (ListView) findViewById(R.id.list);
                    List<String> txtList = new ArrayList<String>();
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(that,android.R.layout.simple_list_item_1, android.R.id.text1, txtList);
                    listView.setAdapter(adapter);

                    serviceItemList.clear();

                    if(mWifiServiceSearcher != null) {;
                        mWifiServiceSearcher.Stop();
                    }else {
                        mWifiServiceSearcher = new WifiServiceSearcher(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
                    }
                    mWifiServiceSearcher.Start();
                }
            }
        });

        ListView listView = (ListView) findViewById(R.id.list);
        List<String> txtList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(that,android.R.layout.simple_list_item_1, android.R.id.text1, txtList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {

                ListView listView = (ListView) findViewById(R.id.list);
                ArrayAdapter<String> adapter = (ArrayAdapter<String>)listView.getAdapter();
                if(adapter.getCount() > position) {
                    String selIttem= adapter.getItem(position);

                    for(int i = 0; i < serviceItemList.size(); i++) {
                        if (selIttem.equals(serviceItemList.get(i).deviceName)){
                            if(mWifiAccessPoint != null){
                                mWifiAccessPoint.Stop();
                                mWifiAccessPoint = null;
                            }
                            if(mWifiConnection != null) {
                                mWifiConnection.Stop();
                                mWifiConnection = null;
                            }
                            mWifiConnection = new WifiConnection(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
                            mWifiConnection.Start();
                            mWifiConnection.Connect(serviceItemList.get(i).deviceAddress);
                            mProgressDlg.show();
                            break;
                        }
                    }
                }
            }
        });

        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Connecting...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mWifiConnection != null) {
                    mWifiConnection.Stop();
                    mWifiConnection = null;
                }

                mWifiAccessPoint = new WifiAccessPoint(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
                mWifiAccessPoint.Start();
            }
        });

        mWifiBase = new WifiBase(this,this);
        mWifiBase.Start();

        mWifiAccessPoint = new WifiAccessPoint(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
        mWifiAccessPoint.Start();

        mWifiServiceSearcher = new WifiServiceSearcher(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
        mWifiServiceSearcher.Start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWifiAccessPoint != null){
            mWifiAccessPoint.Stop();
            mWifiAccessPoint = null;
        }

        if(mWifiConnection != null) {
            mWifiConnection.Stop();
            mWifiConnection = null;
        }
        if(mWifiBase != null) {
            mWifiBase.Stop();
            mWifiBase = null;
        }
    }

    @Override
    public void WifiStateChanged(int state) {

    }

    @Override
    public void gotService(ServiceItem  serviceItem) {
        ListView listView = (ListView) findViewById(R.id.list);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>)listView.getAdapter();
        serviceItemList.add(serviceItem);
        adapter.add(serviceItem.deviceName);
        adapter.notifyDataSetChanged();

        print_line("lisisi"," added service : " + serviceItem.deviceName);
    }

    @Override
    public void GroupInfoAvailable(WifiP2pGroup group) {

    }

    @Override
    public void ConnectionInfoAvailable(WifiP2pInfo info) {

        if (info.isGroupOwner) {
            print_line("", "We are GROUP owner !!");
        } else {
            mProgressDlg.dismiss();
            print_line("", "Connected as Client !!");
            Toast.makeText(getApplicationContext(),"Pairing successful, Do remember to pair from the other device as well !", Toast.LENGTH_LONG).show();

            if(mWifiConnection != null) {
                mWifiConnection.Stop();
                mWifiConnection = null;
            }

            mWifiAccessPoint = new WifiAccessPoint(that, mWifiBase.GetWifiP2pManager(), mWifiBase.GetWifiChannel(), that);
            mWifiAccessPoint.Start();
        }
    }

    @Override
    public void debug(String who, String line) {
        print_line(who,line);
    }

    public void print_line(String who,String line) {
        ((TextView)findViewById(R.id.statusBox)).setText(who + " : " + line);
    }
}
