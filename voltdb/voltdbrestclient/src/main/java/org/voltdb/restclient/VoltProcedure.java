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
        // Init Volt Service
        VoltCall voltCall = VoltService.createVoltService(voltURL);
        return callProcedure(voltCall, procName, params);
    }

    public static VoltResponse callProcedure(VoltCall voltCall, String procName, Object... params) {
        try {
            // Build the parameter map
            Map<String, Object> procParams = buildParameters(procName, params);

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
