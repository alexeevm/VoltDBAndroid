package org.voltdb.restclient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

/**
 * Created by mikealexeev on 3/23/16.
 */
public class VoltProcedure {

    private static final String PARAM_PROCEDURE = "Procedure";
    private static final String PARAM_PARAMETERS = "Parameters";

    public static Map<String, Object> buildParameters(String procName, Object... params) {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(PARAM_PROCEDURE, procName);
        paramsMap.put(PARAM_PARAMETERS, Arrays.asList(params));
        return paramsMap;
    }

    public static VoltResponse callProcedure(String voltURL, String procName, Object... params) {
        try {
            // Build the parameter map
            Map<String, Object> procParams = buildParameters(procName, params);

            // Init Volt Service
            VoltCall voltCall = VoltService.createVoltService(voltURL);

            // Make the REST call
            Call<VoltResponse> call = voltCall.callProcedure(BuildConfig.volt_base_url, procParams);
            VoltCallback callback = new VoltCallback();
            call.enqueue(callback);

            // Wait unitl call is finished
            callback.await();

            VoltResponse voltResponse = callback.getVoltResponse();
            if (voltResponse == null) {
                voltResponse = new VoltResponse(callback.getVoltError());
            }
            return voltResponse;
        } catch (Exception e) {
            return new VoltResponse(e);
        }
    }


}
