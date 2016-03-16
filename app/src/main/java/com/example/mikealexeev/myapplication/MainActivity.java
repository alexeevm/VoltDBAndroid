package com.example.mikealexeev.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import org.voltdb.client.Client;
import org.voltdb.VoltTable;
import org.voltdb.client.ProcCallException;


import java.io.IOException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    Client voltclient = null;
    TextView text;
    boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        text = (TextView) findViewById(R.id.text_id);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button voltDBButton = (Button) findViewById(R.id.button_id);
        voltDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoltDBSelectTask select = new VoltDBSelectTask();
                select.execute(new Integer(2));
            }
        });

        VoltDBConnectTask connectTask = new VoltDBConnectTask();
        connectTask.execute();



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

    class VoltDBConnectTask extends AsyncTask<Object, Void, Client> {

        @Override
        protected Client doInBackground(Object... params) {

            voltclient = org.voltdb.client.ClientFactory.createClient();
            if (voltclient != null) {
                try {
                    voltclient.createConnection("10.0.2.2");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return voltclient;
        }

        @Override
        protected void onPostExecute(Client result) {
        }

    }

    class VoltDBSelectTask extends AsyncTask<Integer, Void, String> {

            @Override
        protected void onPreExecute() {
            if (!busy) {
                text.setText("About to call");
            }
        }

        @Override
        protected String doInBackground(Integer... params) {
            if (busy) {
                return "Another call in progress";
            }
            try {
                busy = true;
                VoltTable vt = voltclient.callProcedure("@AdHoc", "SELECT DESC FROM P1 WHERE ID=?", params[0]).getResults()[0];

                int count = vt.getRowCount();
                if (count > 0) {
                    vt.advanceRow();
                    return vt.getString(0);
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            busy = false;
            if (result != null) {
                text.setText(result);
            } else {
                text.setText("Null result");
            }
        }

    }

}
