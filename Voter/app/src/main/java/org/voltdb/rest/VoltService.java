package org.voltdb.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import voltdb.org.voter.BuildConfig;

/**
 * Created by mikealexeev on 3/23/16.
 */
public class VoltService {

    public static VoltCall createVoltService(final String voltBaseUrl) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        HttpLoggingInterceptor.Level level = BuildConfig.http_debug_level;
        logging.setLevel(level);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        // add logging as last interceptor
        httpClient.addInterceptor(logging);

        Retrofit.Builder builder = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build());
        if (voltBaseUrl != null && !voltBaseUrl.isEmpty()) {
            builder.baseUrl(voltBaseUrl);
        }
        return builder.build().create(VoltCall.class);
    }

}
