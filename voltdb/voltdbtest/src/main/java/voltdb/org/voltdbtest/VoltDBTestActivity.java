package voltdb.org.voltdbtest;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.voltdb.restclient.VoltProcedure;
import org.voltdb.restclient.VoltResponse;
import org.voltdb.restclient.VoltStatus;

import java.util.concurrent.atomic.AtomicBoolean;

public class VoltDBTestActivity extends AppCompatActivity {

    private static final String LOCALHOST = "http://10.0.2.2:8080";

    private static final String VOTE_PROCEDURE = "@SystemInformation";

    private EditText mVoltDBURL;

    AtomicBoolean mCallInProgress = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voltdbtest);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mVoltDBURL = (EditText) findViewById(R.id.voltdb_url_id);

        Button voltDBButton = (Button) findViewById(R.id.button_id);
        voltDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallInProgress.get()) {
                    showToastOnUIThread(getString(R.string.another_call_in_progress), Toast.LENGTH_SHORT);
                    return;
                }
                VoltDBTestTask testTask = new VoltDBTestTask();
                // Both tasks runs on the same thread and the select task is guaranteed to run after the connect finishes
                testTask.execute();
            }
        });

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
                Toast.makeText(VoltDBTestActivity.this, toastMsg, duration).show();
            }
        };
        runOnUiThread(show_toast);
    }


    class VoltDBTestTask extends AsyncTask<Void, Void, VoltResponse> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected VoltResponse doInBackground(Void... params) {
            if (mCallInProgress.compareAndSet(false, true)) {
                try {
                    return testConnection();
                } catch (Exception e) {
                    return new VoltResponse(e);
                } finally {
                    mCallInProgress.set(false);
                }
            } else {
                return new VoltResponse(new Exception(getString(R.string.another_call_in_progress))) ;
            }
        }

        @Override
        protected void onPostExecute(VoltResponse response) {
            String callStatus = null;
            Throwable error = response.getCallError();
            if (error != null) {
                callStatus = error.getMessage();
            }  else {
                callStatus = VoltStatus.toString(response.getStatus());
            }
            showToastOnUIThread(String.format("%s %s.", getString(R.string.status_voltdb), callStatus), Toast.LENGTH_SHORT);
        }

        @Override
        protected void onCancelled() {
            mCallInProgress.set(false);
            showToastOnUIThread(String.format("%s %s.", getString(R.string.status_voltdb), getString(R.string.status_call_cancelled)),
                    Toast.LENGTH_SHORT);
        }

        @Override
        protected void onCancelled(VoltResponse response) {
            mCallInProgress.set(false);
            showToastOnUIThread(String.format("%s %s.", getString(R.string.status_voltdb), getString(R.string.status_call_cancelled)),
                    Toast.LENGTH_SHORT);
        }

        private VoltResponse testConnection() {
            // Init Volt Service
            String voltURL = getBaseURL();
            // Make a call
            return VoltProcedure.callProcedure(voltURL, VOTE_PROCEDURE);
        }
    }


}