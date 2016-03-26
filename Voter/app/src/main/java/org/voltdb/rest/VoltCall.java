package org.voltdb.rest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;
import voltdb.org.voter.BuildConfig;

/**
 * Created by mikealexeev on 3/23/16.
 */
public interface VoltCall {

    public static final String BASE_URL = BuildConfig.volt_base_url;
    @FormUrlEncoded
    @Headers({
         "Accept: application/json",
    })
    @POST()
    Call<VoltResponse> callProcedure(@Url String url, @FieldMap Map<String, Object> params);
}
