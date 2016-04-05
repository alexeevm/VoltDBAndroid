package org.voltdb.restclient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.voltdb.restclient.util.ClientAuthScheme;
import org.voltdb.restclient.util.VoltConnectionUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private static final String VOTE_PROCEDURE = "@SystemInformation";
    private static final String USERNAME = "developer";
    private static final String PASSWORD = "tech";

    private static Process VOLTDB_PROCESS = null;
    private static List<String> VOLTDB_OUTPUT = new ArrayList<>();

    @BeforeClass
    public static void startVolrDB() throws Exception {
        String deployment = "<?xml version=\"1.0\"?>\n" +
                "<deployment>\n" +
                "    <cluster hostcount=\"1\" kfactor=\"0\" />\n" +
                "    <httpd enabled=\"true\">\n" +
                "        <jsonapi enabled=\"true\" />\n" +
                "    </httpd>\n" +
                "    <users>\n" +
                "      <user name=\"" + USERNAME + "\" password=\"" + PASSWORD + "\" roles=\"dbuser\" />\n" +
                "   </users>\n" +
                "</deployment>\n";

        String fileName = "deployment-" + Long.toString(new Date().getTime());
        File deploymentFile = File.createTempFile(fileName, ".xml");

        BufferedWriter bw = new BufferedWriter(new FileWriter(deploymentFile));
        bw.write(deployment);
        bw.close();

        deploymentFile.deleteOnExit();
        String[] voltdbArgs = {VOLTDB_CMD, "create",  "-d", deploymentFile.getPath(), "-H", "localhost"};

        VOLTDB_PROCESS = new ProcessBuilder(voltdbArgs).redirectErrorStream(true).start();
        InputStream stdout = VOLTDB_PROCESS.getInputStream ();

        final BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
        Runnable collectOutput = new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        VOLTDB_OUTPUT.add(line);
                    }
                } catch (Exception e) {
                    VOLTDB_OUTPUT.add(e.getMessage());
                }
            }
        };

        Thread outputThread = new Thread(collectOutput);
        outputThread.start();

        // Wait a few seconds to give the server a chance to start up
        Thread.sleep(5000);
    }

    @AfterClass
    public static void shutdownVoltDB() {
        if (VOLTDB_PROCESS != null) {
            VOLTDB_PROCESS.destroy();
        }
        System.out.println("---------------------------VoltDB Output----------------------------");
        for (String line : VOLTDB_OUTPUT) {
            System.out.println(line);
        }
        System.out.println("---------------------------End of VoltDB Output----------------------");
    }

    @Test
    public void testSyncConnectionSuccess() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifySuccessResponse(response, VoltStatus.SUCCESS);
    }

    @Test
    public void testSyncConnectionUserPwdSuccess() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), USERNAME, PASSWORD, null, 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifySuccessResponse(response, VoltStatus.SUCCESS);
    }

    @Test
    public void testSyncConnectionUserHashePwdSuccess() throws Exception {
        String hexhashedPwd = VoltConnectionUtil.getHexEncodedHashedPassword(ClientAuthScheme.HASH_SHA1, PASSWORD);
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), USERNAME, null, hexhashedPwd, 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifySuccessResponse(response, VoltStatus.SUCCESS);
    }

    @Test
    public void testSyncConnectionUserPwdFailure() throws Exception {
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), USERNAME, PASSWORD + "1", null, 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifyErrorResponse(response, VoltStatus.UNEXPECTED_FAILURE, "Incorrect authorization credentials.");
    }

    @Test
    public void testSyncConnectionUserHashePwdFailure() throws Exception {
        String hexhashedPwd = VoltConnectionUtil.getHexEncodedHashedPassword(ClientAuthScheme.HASH_SHA1, PASSWORD + 1);
        VoltClient voltClient = new VoltClient(new URL(VALID_LOCALHOST), USERNAME, null, hexhashedPwd, 5000);
        VoltResponse response = voltClient.callProcedureSync(VOTE_PROCEDURE);
        verifyErrorResponse(response, VoltStatus.UNEXPECTED_FAILURE, "Incorrect authorization credentials.");
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

    private void verifyErrorResponse(VoltResponse response, int status, String statusString) {
        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertEquals(status, response.getStatus().byteValue());
        assertEquals(statusString, response.getStatusstring());
    }

}