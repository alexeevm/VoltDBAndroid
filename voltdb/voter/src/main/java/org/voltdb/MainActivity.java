/* This file is part of VoltDB.
 * Copyright (C) 2008-2016 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.voltdb.restclient.VoltResponse;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String LOCALHOST = "http://10.0.2.2:8080";

    private static final String VALIDATION_SUCCESS = "VALID";

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 100;

    private LocationManager mLocationManager;
    private String mLocationProvider;

    private TelephonyManager mTelephonyManager;

    private Location mLocation;
    private long mPhoneNumber;
    private int mContestantId;

    private TextView mLongitude;
    private TextView mLatitude;
    private TextView mStatus;
    private EditText mVoltDBURL;
    private EditText mContestant;
    private EditText mPhoneEdit;

    private AtomicBoolean mCallInProgress = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button voltDBButton = (Button) findViewById(R.id.button_id);
        voltDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallInProgress.get()) {
                    showToastOnUIThread(getString(R.string.another_call_in_progress), Toast.LENGTH_SHORT);
                    return;
                }
                setStatusOnUIThread(getString(R.string.status_validating_input));
                String validationResult = validateInput();
                if (!VALIDATION_SUCCESS.equals(validationResult))   {
                    showToastOnUIThread(validationResult, Toast.LENGTH_SHORT);
                    return;
                }

//                VoltDBVoteTask voteTask = new VoltDBVoteTask();
//                voteTask.execute();
                callVoltDB();
            }
        });

        mLongitude = (TextView) findViewById(R.id.lon_id);
        mLatitude = (TextView) findViewById(R.id.lat_id);
        mStatus = (TextView) findViewById(R.id.status_id);
        mVoltDBURL = (EditText) findViewById(R.id.voltdb_url_id);
        mContestant = (EditText) findViewById(R.id.contestant_id);
        mPhoneEdit = (EditText) findViewById(R.id.enter_phone_id);

        if (!BuildConfig.voter_has_location) {
            findViewById(R.id.location_id).setVisibility(View.INVISIBLE);
        }

        if(checkAndRequestPermissions()) {
            // carry on the normal flow, as the case of  permissions  granted.
            identifyPhoneNumber();
            identifyLocation();
        }

    }

    private  boolean checkAndRequestPermissions() {
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        int fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int phonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (BuildConfig.voter_has_location) {
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (phonePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void identifyPhoneNumber() {
        mTelephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        String phoneStr = null;
        try {
            phoneStr = mTelephonyManager.getLine1Number();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (phoneStr != null) {
            TextView phonePrompt = (TextView) findViewById(R.id.phone_prompt_id);
            phonePrompt.setText(getString(R.string.your_phone_number));
            phoneStr = mTelephonyManager.getLine1Number();
            String normilizedPhone = PhoneNumberUtils.formatNumber(phoneStr.substring(1));
            mPhoneEdit.setHint(normilizedPhone);
        }  else {
            mPhoneEdit.setHint(R.string.hint_phone);
        }
    }

    private void identifyLocation() {
        if (BuildConfig.voter_has_location) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // check if enabled and if not send user to the GSP settings
                // Better solution would be to display a dialog and suggesting to
                // go to the settings
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

            // Define the criteria how to select the locatioin provider -> use default
            Criteria criteria = new Criteria();
            mLocationProvider = mLocationManager.getBestProvider(criteria, false);
            if (mLocationProvider == null) {
                mLocationProvider = LocationManager.GPS_PROVIDER;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        // We assume that user has granted all the permissions
        identifyPhoneNumber();
        identifyLocation();
    }

    private String validateInput() {
        // Validate phone number
        String phoneStr = mPhoneEdit.getText().toString();
        if (phoneStr == null || phoneStr.isEmpty())  {
            phoneStr = mPhoneEdit.getHint().toString();
        }
        try {
            mPhoneNumber = Long.parseLong(PhoneNumberUtils.normalizeNumber(phoneStr)) % 10000000000l;
        } catch (NumberFormatException e) {
            return getString(R.string.invalid_phone);
        }

        // Validate Contestant id
        try {
            mContestantId = Integer.parseInt(mContestant.getText().toString());
        } catch (NumberFormatException e) {
            return getString(R.string.invalid_contestant_id);
        }

        // Location
        if (BuildConfig.voter_has_location && mLocation == null) {
            // Try one more time
            try {
                mLocation = mLocationManager.getLastKnownLocation(mLocationProvider);
                onLocationChanged(mLocation);
            } catch (SecurityException se) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_geo_location_permissions), Toast.LENGTH_SHORT);
                toast.show();
            }
            if (mLocation == null) {
                return getString(R.string.invalid_location);
            }
        }

        return VALIDATION_SUCCESS;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (BuildConfig.voter_has_location && hasGeoPermissions() && mLocationManager != null) {
                mLocationManager.requestLocationUpdates(mLocationProvider, 400, 1, this);
            }
        } catch (SecurityException se) {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_geo_location_permissions), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (BuildConfig.voter_has_location && hasGeoPermissions() && mLocationManager != null) {
                mLocationManager.removeUpdates(this);
            }
        } catch (SecurityException se) {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_geo_location_permissions), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Location Listener Interface
    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (mLocation != null)  {
            mLongitude.setText(getString(R.string.longitude) + " " + Double.toString(mLocation.getLongitude()));
            mLatitude.setText(getString(R.string.latitude) + " " + Double.toString(mLocation.getLatitude()));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private boolean hasGeoPermissions() {
        return ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String getBaseURL() {
        if (mVoltDBURL.getText() == null || mVoltDBURL.getText().length() == 0) {
            return LOCALHOST;
        }
        return mVoltDBURL.getText().toString();
    }

    private void showToastOnUIThread(final String toastMsg, final int duration) {
        Runnable show_toast = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, toastMsg, duration).show();
            }
        };
        runOnUiThread(show_toast);
    }

    private void setStatusOnUIThread(final String status) {
        Runnable setStatus = new Runnable() {
            @Override
            public void run() {
                mStatus.setText(status);
            }
        };
        runOnUiThread(setStatus);
    }

    // VoltDB Stuff
    private void callVoltDB() {
        if (mCallInProgress.compareAndSet(false, true)) {
            Callable<VoltResponse> voltCallable = (BuildConfig.voter_has_location) ?
                    VoltHelper.createVoltCall(getBaseURL(), mPhoneNumber, mLocation, mContestantId) :
                    VoltHelper.createVoltCall(getBaseURL(), mPhoneNumber, mContestantId);
            // Create an observable that will be executed off the Main UI thread but results will be processed
            // on the UI thread
            ObservableUtils.createObservable(voltCallable).subscribeOn(Schedulers.io()).
                    observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<VoltResponse>() {
                // Success
                @Override
                public void call(final VoltResponse response) {
                    mCallInProgress.set(false);
                    handleResponse(response);
                }
            }, new Action1<Throwable>() {
                // Failure
                @Override
                public void call(final Throwable error) {
                    mCallInProgress.set(false);
                    handleResponse(new VoltResponse(error));
                }
            });
        } else {
            handleResponse(new VoltResponse(new Exception(getString(R.string.another_call_in_progress))));
        }

    }

    protected void handleResponse(VoltResponse response) {
        String callStatus = VoltHelper.parseResponse(this, response);
        setStatusOnUIThread(String.format("%s %s.", getString(R.string.status_voltdb), callStatus));
    }

}
