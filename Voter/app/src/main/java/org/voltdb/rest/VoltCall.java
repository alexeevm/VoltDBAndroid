package org.voltdb.rest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by mikealexeev on 3/23/16.
 */
public interface VoltCall {

    @FormUrlEncoded
    @Headers({
         "Accept: application/json",
    })
    @POST("/api/1.0/")
    Call<VoltResponse> callProcedure(@FieldMap Map<String, Object> params);
}
