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

import java.util.concurrent.CountDownLatch;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mikealexeev on 3/24/16.
 */
public class VoltCallback implements Callback<VoltResponse> {
    private CountDownLatch mlatch = new CountDownLatch(1);
    private VoltResponse mvoltResponse;
    private Throwable mvoltError;

    @Override
    public void onResponse(Call<VoltResponse> call, Response<VoltResponse> response) {
        mvoltResponse = response.body();
        // Release the latch
        mlatch.countDown();
    }

   @Override
    public void onFailure(Call<VoltResponse> call, Throwable t) {
        mvoltError = t;
        mlatch.countDown();
    }

    public VoltResponse getVoltResponse() {
        return mvoltResponse;
    }

    public Throwable getVoltError() {
        return mvoltError;
    }

    public void await() throws InterruptedException {
        mlatch.await();
    }

}
