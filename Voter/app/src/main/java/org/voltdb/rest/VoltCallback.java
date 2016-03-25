package org.voltdb.rest;

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
