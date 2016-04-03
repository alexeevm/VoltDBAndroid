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

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import okhttp3.HttpUrl;
import retrofit2.Call;

/**
 *  <p>
 *  A <code>VoltClient</code> that connects to a VoltDB via its JSON HTTP interface
 *  and provides methods for invoking stored procedures and receiving
 *  responses.</p>
 */
public class VoltClient {

    // JSON parameters names
    private static final String PARAM_PROCEDURE = "Procedure";
    private static final String PARAM_PARAMETERS = "Parameters";
    private static final String PARAM_USERNAME = "User";
    private static final String PARAM_PASSWORD = "Password";
    private static final String PARAM_HASHEDPASSWORD = "Hashedpassword";
    private static final String PARAM_ISADMIN = "admin";
    private static final String PARAM_JSONP = "jsonp";

    private String mUsername;
    private String mPassword;
    private String mHashedPassword;
    private Boolean misAdmin;
    private String mJsonp;
    private int mQueryTimeout;

    private VoltCall mVoltCall;

    /**
     * Class Constructor.
     *
     * <p>WARNING: Use of a queryTimeout value that is greater than the global timeout value for your VoltDB configuration
     * will temporarily override that safeguard. Currently, non-privileged users (requiring only SQLREAD permissions)
     * can invoke this method, potentially degrading system performance with an uncontrolled long-running procedure.</p>
     *
     * A 0 queryTimeout means no timeout.
     *
     * @param voltURL           VoltDb URL
     * @param username          username for authentication
     * @param password          password for authentication
     * @param hashedpassword    Hashed password for authentication
     * @param isAdmin           true|false
     * @param jsonp             function-name
     * @param queryTimeout      call timeout in milliseconds
     */
    public VoltClient(URL voltURL, String username, String password, String hashedpassword, Boolean isAdmin, String jsonp, int queryTimeout) {
        mUsername = username;
        mPassword = password;
        mHashedPassword = hashedpassword;
        misAdmin = isAdmin;
        mJsonp = jsonp;
        mQueryTimeout = queryTimeout;

        mVoltCall = VoltService.createVoltService(HttpUrl.get(voltURL), queryTimeout);
    }

    /**
     * Class Constructor.
     *
     * @param voltURL           VoltDb URL
     * @param username          username for authentication
     * @param password          password for authentication
     * @param hashedpassword    Hashed password for authentication
     * @param timeout           call timeout in milliseconds
     */
    public VoltClient(URL voltURL, String username, String password, String hashedpassword, int timeout) {
        this(voltURL, username, password, hashedpassword, null, null, timeout);
    }
    /**
     * Class Constructor.
     *
     * @param voltURL           VoltDb URL
     * @param username          username for authentication
     * @param password          password for authentication
     * @param hashedpassword    Hashed password for authentication
     */
    public VoltClient(URL voltURL, String username, String password, String hashedpassword) {
        this(voltURL, username, password, hashedpassword, null, null, 0);
    }

    /**
     * Class Constructor.
     *
     * @param voltURL           VoltDb URL
     * @param timeout           call timeout in milliseconds
     */
    public VoltClient(URL voltURL, int timeout) {
        this(voltURL, null, null, null, null, null, timeout);
    }
    /**
     * Class Constructor.
     *
     * @param voltURL           VoltDb URL
     */
    public VoltClient(URL voltURL) {
        this(voltURL, null, null, null, null, null, 0);
    }

    /**
     * Helper method to build JSON parameters
     * @param procName procedure name
     * @param params  vararg list of procedure's parameter values.
     * @return name/value parameter map
     */
    private Map<String, Object> buildParameters(String procName, Object... params) {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(PARAM_PROCEDURE, procName);
        paramsMap.put(PARAM_PARAMETERS, Arrays.asList(params));
        if (mUsername != null) {
            paramsMap.put(PARAM_USERNAME, mUsername);
        }
        if (mPassword != null) {
            paramsMap.put(PARAM_PASSWORD, mPassword);
        }
        if (mHashedPassword != null) {
            paramsMap.put(PARAM_HASHEDPASSWORD, mHashedPassword);
        }
        if (misAdmin != null) {
            paramsMap.put(PARAM_ISADMIN, misAdmin);
        }
        if (mJsonp != null) {
            paramsMap.put(PARAM_JSONP, mJsonp);
        }
        return paramsMap;
    }

    /**
     * <p>Synchronously invoke a procedure with timeout. Blocks until a result is available or timeout is reached.
     *
     * @param procName <code>class</code> name (not qualified by package) of the procedure to execute.
     * @param parameters vararg list of procedure's parameter values.
     * @return VoltResponse object. In case of any exception, the response will contain the exception object thrown
     */
    public VoltResponse callProcedureSync(String procName, Object... parameters) {
        try {
            // Build the parameter map
            Map<String, Object> procParams = buildParameters(procName, parameters);

            // Make the REST call
            Call<VoltResponse> call = mVoltCall.callProcedure(BuildConfig.volt_base_url, procParams);
            VoltCallback callback = new VoltCallback(mQueryTimeout);
            call.enqueue(callback);

            // Wait unitl call returns or times out
            callback.await();
            VoltResponse voltResponse = callback.getVoltResponse();
            if (voltResponse == null) {
                Throwable t = null;
                if (!callback.hasResult()) {
                    // The callback returns because of the timeout
                    t = new TimeoutException(String.format("No response has received after the %d ms.", mQueryTimeout));
                } else {
                    t = callback.getVoltError();
                }
                voltResponse = getErrorResponse(t);
            }
            return voltResponse;
        } catch (Exception e) {
            return getErrorResponse(e);
        }
    }

    /**
     * <p>Asynchronously invoke a procedure. Returns a callback object without blocking
     *
     * @param procName <code>class</code> name (not qualified by package) of the procedure to execute.
     * @param parameters vararg list of procedure's parameter values.
     * @return VoltCallback object
     */
    public VoltCallback callProcedureAsync(String procName, Object... parameters) {
        // Build the parameter map
        Map<String, Object> procParams = buildParameters(procName, parameters);

        // Make the REST call
        Call<VoltResponse> call = mVoltCall.callProcedure(BuildConfig.volt_base_url, procParams);
        VoltCallback callback = new VoltCallback(mQueryTimeout);
        call.enqueue(callback);

        return callback;
    }

    private VoltResponse getErrorResponse(Throwable t) {
        VoltResponse voltResponse = new VoltResponse(t);
        voltResponse.setStatusstring(t.getMessage());
        if (t instanceof ConnectException) {
            voltResponse.setStatus(VoltStatus.CONNECTION_ERROR);
        } else if (t instanceof TimeoutException) {
            voltResponse.setStatus(VoltStatus.CONNECTION_TIMEOUT);
        }  else if (t instanceof SocketTimeoutException) {
            voltResponse.setStatus(VoltStatus.CONNECTION_TIMEOUT);
        } else {
            voltResponse.setStatus(VoltStatus.UNEXPECTED_FAILURE);
        }
        return voltResponse;
    }


}
