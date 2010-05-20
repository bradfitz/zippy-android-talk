// -*-java-*-

package com.google.android.iodroid;

import com.google.android.iodroid.IServiceCallback;

interface IIODroidService {
    // For the activity to register itself with the service:
    void registerCallback(IServiceCallback callback);
    void unregisterCallback(IServiceCallback callback);

    void setMaxOutstandingWrites(int value);
    void setMaxOutstandingReads(int value);
}

