package org.voltdb.restclient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.ConnectException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class VoltClientTest {

    private static final String VALID_LOCALHOST = "http://127.0.0.1:8080/";
    private static final String INVALID_LOCALHOST = "http://127.0.0.1:8081/";
    private static final String VOLTDB_CMD = System.getenv("VOLTDB_HOME") + File.separator + "voltdb";
    private static final String[] VOLTDB_START = {VOLTDB_CMD, "create",  "-H", "localhost", "--new"};
    private static final String VOTE_PROCEDURE = "@SystemInformation";

    private static Process VOLTDB_PROCESS = null;

    @BeforeClass
    public static void startVolrDB() throws Exception {
        VOLTDB_PROCESS = new ProcessBuilder(VOLTDB_START).redirectErrorStream(true).start();
        // Wait a few seconds to give the server a chance to start up
        Thread.sleep(5000);
    }

    @AfterClass
    public static void shutdownVoltDB() {
        if (VOLTDB_PROCESS != null) {
            VOLTDB_PROCESS.destroy();
        }
    }

    @Test
    public void testSyncConnectionSuccess() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifySuccessResponse(response, VoltStatus.SUCCESS);
    }

    @Test
    public void testSyncConnectionFailure() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(INVALID_LOCALHOST), 1000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifyErrorResponse(response, VoltStatus.CONNECTION_ERROR, ConnectException.class);
    }

    @Test
    public void testSyncConnectionTimeout() throws Exception {
        // Very short timeout
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), 1);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifyErrorResponse(response, VoltStatus.CONNECTION_TIMEOUT, TimeoutException.class);
    }

    @Test
    public void testAsyncConnectionSuccess() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), 5000);
        VoltCallback callback = voltClient.callProcedureAsync(VOTE_PROCEDURE);
        VoltResponse response = callback.await();
        assertTrue(callback.hasResult());
        verifySuccessResponse(response, VoltStatus.SUCCESS);
    }

    @Test
    public void testAsyncConnectionFailure() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(INVALID_LOCALHOST), 1000);
        VoltCallback callback = voltClient.callProcedureAsync(VOTE_PROCEDURE);
        VoltResponse response = callback.await();
        assertTrue(callback.hasResult());
        verifyErrorResponse(response, VoltStatus.CONNECTION_ERROR, ConnectException.class);
    }

    @Test
    public void testAsyncConnectionTimeout() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(INVALID_LOCALHOST), 1);
        VoltCallback callback = voltClient.callProcedureAsync(VOTE_PROCEDURE);
        VoltResponse response = callback.await();
        assertTrue(!callback.hasResult());
        verifyErrorResponse(response, VoltStatus.CONNECTION_TIMEOUT, TimeoutException.class);
    }

    private void verifySuccessResponse(VoltResponse response, int status) {
        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertEquals(status, response.getStatus().byteValue());
    }

    private void verifyErrorResponse(VoltResponse response, int status, Class errorClass) {
        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertEquals(status, response.getStatus().byteValue());
        Throwable t = response.getCallError();
        assertTrue(t != null);
        assertEquals(errorClass, t.getClass());
    }

}