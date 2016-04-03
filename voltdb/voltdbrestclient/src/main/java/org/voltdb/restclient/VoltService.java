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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Helper class to instantiate a VoltCall instance
 */
class VoltService {

    /**
     * Return a VoltCall object. If timeout is set to zero, the default OkHTTPClient timeout will be used
     *
     * @param voltBaseUrl VoltDB URL
     * @param timeout  timeout in milliseconds
     * @return VoltCall
     */
    public static VoltCall createVoltService(final HttpUrl voltBaseUrl, int timeout) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        HttpLoggingInterceptor.Level level = BuildConfig.http_debug_level;
        logging.setLevel(level);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        // add logging as last interceptor
        httpClientBuilder.addInterceptor(logging);

        // Set timeout
        httpClientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS);

        Retrofit.Builder builder = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClientBuilder.build())
                .baseUrl(voltBaseUrl);
        return builder.build().create(VoltCall.class);
    }

}
