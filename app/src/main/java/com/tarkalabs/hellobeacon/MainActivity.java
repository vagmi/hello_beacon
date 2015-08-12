package com.tarkalabs.hellobeacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    public static final String TAG = "MonitoringActivity";
    public static final String RANGING = "RANGING";
    private BeaconManager beaconManager;
    private Region allBeaconsRegion;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtMessage = (TextView) findViewById(R.id.message);
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                txtMessage.setText(intent.getStringExtra("message"));
            }
        },new IntentFilter(RANGING));
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        BeaconParser urlParser = new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20");
        beaconManager.getBeaconParsers().add(urlParser);
        beaconManager.bind(this);
        txtMessage.setText("Waiting for bluetooth to init ...");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new MyRangingNotifier());
        allBeaconsRegion = new Region("all-beacons-region", null, null, null);
        try {
            beaconManager.startRangingBeaconsInRegion(allBeaconsRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private class UrlBeacon {
        public String url;
        public Double distance;

        public UrlBeacon(String url, Double distance) {
            this.url = url;
            this.distance = distance;
        }
    }
    private  class MyRangingNotifier implements RangeNotifier {

        public static final String PREFIX = "http://tarkalabs.com/";

        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
            String message = "";
            int beaconCount = collection.size();
            if(beaconCount > 0) {
                ArrayList<UrlBeacon> beacons = new ArrayList<>();
                for(Beacon beacon : collection) {
                    if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                        String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                        String roomName = url.substring(PREFIX.length(), url.length() - 1);
                        Log.i(TAG, "found beacon " + url + " " + beacon.getDistance() + " meters away.");
                        beacons.add(new UrlBeacon(roomName, beacon.getDistance()));
                    }
                }
                Collections.sort(beacons, new Comparator<UrlBeacon>() {
                    @Override
                    public int compare(UrlBeacon lhs, UrlBeacon rhs) {
                        return lhs.distance.compareTo(rhs.distance);
                    }
                });
                if(beacons.size() > 0) {
                    message = beacons.get(0).url;
                } else {
                    message = "No beacons found";
                }
            } else {
                message = "No beacons found";
            }
            Intent intent = new Intent(RANGING);
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

}