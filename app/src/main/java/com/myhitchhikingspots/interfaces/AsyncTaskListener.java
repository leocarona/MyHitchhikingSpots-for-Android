package com.myhitchhikingspots.interfaces;

/**
 * Created by leoboaventura on 31/07/2017.
 */

public interface AsyncTaskListener<T> {
    void notifyTaskFinished(Boolean success, T message);
}
