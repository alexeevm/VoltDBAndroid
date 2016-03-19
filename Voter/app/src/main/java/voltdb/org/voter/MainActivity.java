package voltdb.org.voter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google_voltpatches.common.base.Strings;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ProcCallException;
import org.voltdb.types.GeographyPointValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String LOCALHOST = "10.0.2.2";
    private static final int PORT = 21212;

    private static final String VALIDATION_SUCCESS= "VALID";

    private static final int MAX_NUM_VOTES = 2;

    private LocationManager mLocationManager;
    private String mLocationProvider;

    private TelephonyManager mTelephonyManager;

    Location mLocation;
    long mPhoneNumber;
    int mContestantId;
    String mVoltDBHost = LOCALHOST;
    int mVoltDBPort = PORT;


    TextView mLongitude;
    TextView mLatitude;
    TextView mStatus;
    TextView mPhone;
    EditText mVoltDBURL;
    EditText mContestant;
    EditText mPhoneEdit;

    Client mVoltClient = null;

    AtomicBoolean mCallInProgress = new AtomicBoolean(false);

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
                    showToastOnUIThread("Another call is in progress", Toast.LENGTH_SHORT);
                    return;
                }
                setStatusOnUIThread("Status: Validating Input");
                String validationResult = validateInput();
                if (!VALIDATION_SUCCESS.equals(validationResult))   {
                    showToastOnUIThread(validationResult, Toast.LENGTH_SHORT);
                    return;
                }
                VoltDBConnectTask connectTask = new VoltDBConnectTask();
                connectTask.execute();
                VoltDBVoteTask voteTask = new VoltDBVoteTask();
                // Both tasks runs on the same thread and the select task is guaranteed to run after the connect finishes
                voteTask.execute();
            }
        });

        mLongitude = (TextView) findViewById(R.id.lon_id);
        mLatitude = (TextView) findViewById(R.id.lat_id);
        mStatus = (TextView) findViewById(R.id.status_id);
        mVoltDBURL = (EditText) findViewById(R.id.voltdb_url_id);
        mContestant = (EditText) findViewById(R.id.contestant_id);
        mPhone = (TextView) findViewById(R.id.identified_phone_id);
        mPhoneEdit = (EditText) findViewById(R.id.enter_phone_id);

        mTelephonyManager = (TelephonyManager) getApplicationContext()
                        .getSystemService(Context.TELEPHONY_SERVICE);
        String phoneStr = mTelephonyManager.getLine1Number();
        if (phoneStr != null) {
            TextView phonePrompt = (TextView) findViewById(R.id.phone_prompt_id);
            phonePrompt.setText("Your phone number");
            phoneStr = mTelephonyManager.getLine1Number();
            String normilizedPhone = PhoneNumberUtils.formatNumber(phoneStr.substring(1));
            mPhone.setText(normilizedPhone);

            mPhone.setVisibility(View.VISIBLE);
            mPhoneEdit.setVisibility(View.GONE);
        }  else {
            mPhone.setVisibility(View.GONE);
            mPhoneEdit.setVisibility(View.VISIBLE);
        }


        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // check if enabled and if not send user to the GSP settings
            // Better solution would be to display a dialog and suggesting to
            // go to the settings
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // Define the criteria how to select the locatioin provider -> use default
        Criteria criteria = new Criteria();
        mLocationProvider = mLocationManager.getBestProvider(criteria, false);
        try {
            mLocationManager.requestLocationUpdates(mLocationProvider, 400, 1, this);
            // Update UI just in case
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            onLocationChanged(mLocation);
        } catch(SecurityException se) {
            Toast toast = Toast.makeText(getApplicationContext(), "Don't have Geo Location Permissions ", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private String validateInput() {
        // Validate phone number
        String phoneStr = (mPhone.getVisibility() == View.VISIBLE) ?
                mPhone.getText().toString() : mPhoneEdit.getText().toString();
        if (Strings.isNullOrEmpty(phoneStr))  {
            return "Phone String is empty";
        }
        try {
            mPhoneNumber = Long.parseLong(PhoneNumberUtils.normalizeNumber(phoneStr)) % 10000000000l;
        } catch (NumberFormatException e) {
            return "Invalid Phone Number";
        }

        // Validate Contestant id
        try {
            mContestantId = Integer.parseInt(mContestant.getText().toString());
        } catch (NumberFormatException e) {
            return "Invalid Contestant ID";
        }

        // Locatiom
        if (mLocation == null) {
            return "Invalid Location";
        }

        // VoltDB URL
        String voltDBURL = getConnectionURL();
        String[] voltDBAddress =  voltDBURL.split(":");
        mVoltDBHost = voltDBAddress[0];
        if (voltDBAddress.length > 1) {
            try {
                mVoltDBPort = Integer.parseInt(voltDBAddress[1]);
            } catch (NumberFormatException e) {
                return "Invalid VoltDB Port";
            }
        }

        return VALIDATION_SUCCESS;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (hasGeoPermissions()) {
                mLocationManager.requestLocationUpdates(mLocationProvider, 400, 1, this);
            }
        } catch (SecurityException se) {
            Toast toast = Toast.makeText(getApplicationContext(), "Don't have Geo Location Permissions ", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (hasGeoPermissions()) {
                mLocationManager.removeUpdates(this);
            }
        } catch (SecurityException se) {
            Toast toast = Toast.makeText(getApplicationContext(), "Don't have Geo Location Permissions ", Toast.LENGTH_SHORT);
            toast.show();
        }
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

    // Location Listener Interface
    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        if (mLocation != null)  {
            mLongitude.setText("Longitude: " + Double.toString(mLocation.getLongitude()));
            mLatitude.setText("Latitude: " + Double.toString(mLocation.getLatitude()));
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

    private String getConnectionURL() {
        if (mVoltDBURL.getText() == null || Strings.isNullOrEmpty(mVoltDBURL.getText().toString()) ) {
            return LOCALHOST;
        }
        return mVoltDBURL.getText().toString();
    }

    private boolean needToReconnectVoltDB() {
        if (mVoltClient == null) {
            return true;
        } else {
            List<InetSocketAddress> connections = mVoltClient.getConnectedHostList();
            if (connections != null)   {
                for (InetSocketAddress addr : connections) {
                    if(addr.getHostName().equals(mVoltDBHost) && addr.getPort() == mVoltDBPort) {
                        return false;
                    }
                }
            }
        }
        return true;
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
    class VoltDBConnectTask extends AsyncTask<Void, Void, Client> {

        @Override
        protected  void onPreExecute() {
        }
        @Override
        protected Client doInBackground(Void... params) {
            setStatusOnUIThread("Status: Connection to VoltDB");

            if (!needToReconnectVoltDB()) {
                return mVoltClient;
            }
            mVoltClient = null;

            if (mCallInProgress.compareAndSet(false, true)) {
                try {
                    mVoltClient = org.voltdb.client.ClientFactory.createClient();
                    if (mVoltClient != null) {
                        String connectionStr = mVoltDBHost + ":" + Integer.toString(mVoltDBPort);
                        mVoltClient.createConnection(connectionStr);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mCallInProgress.set(false);
                }
            }
            if (mVoltClient == null) {
                showToastOnUIThread("Failed to connect to VoltDB at " + mVoltClient + ". Please verify the URL", Toast.LENGTH_SHORT);
            } else {
                setStatusOnUIThread("Status: Connected to VoltDB");
            }
            return mVoltClient;
        }

        @Override
        protected void onPostExecute(Client result) {
        }

        @Override
        protected void onCancelled() {
            mCallInProgress.set(false);
            setStatusOnUIThread("Status: Connection cancelled");
        }

    }

    class VoltDBVoteTask extends AsyncTask<Void, Void, Integer> {

        static final int ERR_VOTER = -1;
        static final int ERR_CALL_INPROGRESS = -2;
        static final int ERR_CONNECTION = -3;
        static final int ERR_INVALID_CONTESTANT = 1;
        static final int ERR_VOTER_OVER_VOTE_LIMIT = 2;
        static final int SUCCESS = 0;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground(Void... params) {
            setStatusOnUIThread("Status: Calling VoltDB");
            if (mCallInProgress.compareAndSet(false, true)) {
                try {
                    return vote(mPhoneNumber, mLocation, mContestantId, MAX_NUM_VOTES);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mCallInProgress.set(false);
                }
                return ERR_VOTER;
            } else {
                return ERR_CALL_INPROGRESS;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            String callStatus = null;
            switch (result) {
                case ERR_CALL_INPROGRESS: callStatus = "Another call is in progress"; break;
                case ERR_INVALID_CONTESTANT: callStatus = "Invalid contestant"; break;
                case ERR_VOTER_OVER_VOTE_LIMIT: callStatus = "Vote limit is exceeded"; break;
                case ERR_CONNECTION: callStatus = "Failed to connect to VoltDB"; break;
                case SUCCESS: callStatus = "Success"; break;
                default:
                    callStatus = "VoltDB procedure error";
                    mVoltClient = null;
                    break;
            }
            setStatusOnUIThread(String.format("Status: VoltDB call %s.", callStatus));
        }

        @Override
        protected void onCancelled() {
            mCallInProgress.set(false);
            setStatusOnUIThread("Status: Call cancelled");
        }

        @Override
        protected void onCancelled(Integer result) {
            mCallInProgress.set(false);
            setStatusOnUIThread("Status: Call cancelled");
        }

        private int vote(long phoneNumber, Location location, int contestantNumber, long maxVotesPerPhoneNumber) throws
                ProcCallException, IOException {
            if (mVoltClient == null) {
                return ERR_CONNECTION;
            }
            GeographyPointValue geoLocation = new GeographyPointValue(location.getLongitude(), location.getLatitude());
            VoltTable vt = mVoltClient.callProcedure("Vote", phoneNumber, geoLocation, contestantNumber, maxVotesPerPhoneNumber).getResults()[0];
            if (vt.getRowCount() == 1) {
                vt.advanceRow();
                return (int) vt.getLong(0);
            }
            return ERR_VOTER;
        }
    }

}
