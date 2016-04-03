package org.voltdb.restclient;

/**
 * Created by mikealexeev on 3/29/16.
 */
public class VoltStatus {
    /**
     * Status code indicating the store procedure executed successfully
     */
    public static final byte SUCCESS = 1;

    /**
     * Status code indicating the stored procedure executed successfully and was voluntarily aborted and rolled
     * back by the stored procedure code
     */
    public static final byte USER_ABORT = -1;

    /**
     * Status code indicating the stored procedure failed and was rolled back. There are no negative server side
     * side effects.
     */
    public static final byte GRACEFUL_FAILURE = -2;

    /**
     * Status code indicating the stored procedure failed (or may never have been successfully invoked)
     * and that there may have been negative side effects on the server
     */
    public static final byte UNEXPECTED_FAILURE = -3;

    /**
     * Status code indicating the connection to the database that the invocation was queued at
     * was lost before a response was received. It is possible that the invocation was sent, executed, and successfully
     * committed before a response could be returned or the invocation may never have been sent.
     */
    public static final byte CONNECTION_LOST = -4;

    /**
     * Status code indicating that the server is currently unavailable for stored procedure invocations.
     * The invocation for which this is a response was never executed.
     */
    public static final byte SERVER_UNAVAILABLE = -5;

    /**
     * Status code indicating that the request didn't receive a response before the per-client timeout.
     */
    public static final byte CONNECTION_TIMEOUT = -6;

    /**
     * Status code indicating that the response was lost, and the outcome of the invocation is unknown.
     */
    public static final byte RESPONSE_UNKNOWN = -7;

    /**
     * Status code indicating that the transaction completed and did not roll back, but some part
     * of the operation didn't succeed. For example, this is returned when a snapshot restore operation
     * fails to restore one table out of many.
     */
    public static final byte OPERATIONAL_FAILURE = -9;

    /**
     * Status code indicating that the client wasn't able to establish network connection to the database.
     */
    public static final byte CONNECTION_ERROR = -10;

    /**
     * Default value for the user specified app status code field
     */
    public static final byte UNINITIALIZED_APP_STATUS_CODE = Byte.MIN_VALUE;

    public static String toString(int status) {
        switch (status) {
            case SUCCESS:                       return "SUCCESS";
            case USER_ABORT:                    return "USER_ABORT";
            case GRACEFUL_FAILURE:              return "GRACEFUL_FAILURE";
            case UNEXPECTED_FAILURE:            return "UNEXPECTED_FAILURE";
            case CONNECTION_LOST:               return "CONNECTION_LOST";
            case SERVER_UNAVAILABLE:            return "SERVER_UNAVAILABLE";
            case CONNECTION_TIMEOUT:            return "CONNECTION_TIMEOUT";
            case RESPONSE_UNKNOWN:              return "RESPONSE_UNKNOWN";
            case OPERATIONAL_FAILURE:           return "OPERATIONAL_FAILURE";
            case CONNECTION_ERROR:               return "CONNECTION_ERROR";
            case UNINITIALIZED_APP_STATUS_CODE: return "UNINITIALIZED";
            default:                            return "UNKNOWN";
        }
    }

}
