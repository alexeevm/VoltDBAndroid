package org.voltdb;

import android.content.Context;
import android.location.Location;

import org.voltdb.restclient.VoltClient;
import org.voltdb.restclient.VoltResponse;
import org.voltdb.restclient.VoltStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by mikealexeev on 5/9/16.
 */
public class VoltHelper {

    static final int ERR_VOTER = -1;
    static final int ERR_CALL_INPROGRESS = -2;
    static final int ERR_CONNECTION = -3;
    static final int ERR_INVALID_CONTESTANT = 1;
    static final int ERR_VOTER_OVER_VOTE_LIMIT = 2;
    static final int SUCCESS = 0;

    private static final String VOTE_PROCEDURE = "Vote";
    private static final int MAX_NUM_VOTES = 2;

    private VoltHelper() {
    }

    public static Callable<VoltResponse> createVoltCall(final String voltURL, final long phoneNumber, final Location location, final int contestantNumber) {
        return new Callable<VoltResponse>() {
            @Override
            public VoltResponse call() throws Exception {
                return vote(voltURL, phoneNumber, location, contestantNumber);
            }
        };
    }

    public static Callable<VoltResponse> createVoltCall(final String voltURL, final long phoneNumber, final int contestantNumber) {
        return new Callable<VoltResponse>() {
            @Override
            public VoltResponse call() throws Exception {
                return vote(voltURL, phoneNumber, contestantNumber);
            }
        };
    }

    public static String parseResponse(Context context, VoltResponse response) {
        String callStatus = null;
        Throwable error = response.getCallError();
        if (error != null) {
            callStatus = error.getMessage();
        }  else {
            callStatus = VoltStatus.toString(response.getStatus());
            if ("SUCCESS".equals(callStatus)) {
                List<VoltResponse.VoltTable> results = response.getResults();
                assert(results != null && !results.isEmpty());
                VoltResponse.VoltTable table = results.get(0);
                List<Object> data = table.getData();
                assert(data != null && !data.isEmpty());
                try {
                    List<Double> dataElement = (List<Double>) data.get(0);
                    int status = dataElement.get(0).intValue();
                    switch ((int) status) {
                        case ERR_CALL_INPROGRESS:
                            callStatus = context.getString(R.string.another_call_in_progress);
                            break;
                        case ERR_INVALID_CONTESTANT:
                            callStatus = context.getString(R.string.invalid_contestant);
                            break;
                        case ERR_VOTER_OVER_VOTE_LIMIT:
                            callStatus = context.getString(R.string.vote_limit_exceeded);
                            break;
                        case ERR_CONNECTION:
                            callStatus = context.getString(R.string.failed_to_connect);
                            break;
                        case SUCCESS:
                            callStatus = context.getString(R.string.success);
                            break;
                        default:
                            callStatus = context.getString(R.string.voltdb_error);
                            break;
                    }
                } catch (Exception e) {
                    callStatus = context.getString(R.string.voltdb_error);
                }
            } else if (response.getStatusstring() != null ){
                callStatus = response.getStatusstring();
            }
        }
        return callStatus;
    }

    public static VoltResponse vote(String voltURL, long phoneNumber, Location location, int contestantNumber) throws MalformedURLException {
        // Init Volt Service
        String locationStr = "POINT(" + Double.toString(location.getLongitude()) + " " + Double.toString(location.getLatitude()) + ")";
        // Make a call
        VoltClient voltClient = new VoltClient(new URL(voltURL), 5000);
        return voltClient.callProcedureSync(VOTE_PROCEDURE, phoneNumber, locationStr, contestantNumber, MAX_NUM_VOTES);
    }

    public static VoltResponse vote(String voltURL, long phoneNumber, int contestantNumber) throws  MalformedURLException{
        // Init Volt Service
        VoltClient voltClient = new VoltClient(new URL(voltURL), 5000);
        return voltClient.callProcedureSync(VOTE_PROCEDURE, phoneNumber, contestantNumber, MAX_NUM_VOTES);
    }
}
