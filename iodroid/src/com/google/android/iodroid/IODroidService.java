package com.google.android.iodroid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class IODroidService extends Service {
    private static final String TAG = "IODroidService";
        
    private static final String DUMMY_FILE = "dummy_file";

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final AtomicReference<IServiceCallback> callback =
        new AtomicReference<IServiceCallback>();

    private final AtomicInteger maxOutstandingWrites = new AtomicInteger(0);
    private final AtomicInteger outstandingWrites = new AtomicInteger(0);

    private final AtomicInteger maxOutstandingReads = new AtomicInteger(0);
    private final AtomicInteger outstandingReads = new AtomicInteger(0);
  
    private boolean debugLogging = false;
    
    private SharedPreferences preferences;

    @Override
        public void onCreate() {
        super.onCreate();
        
        preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        //debugLogging = preferences.getBoolean(Preferences.KEY_DEBUG_LOGGING, false);
    }
        
    @Override
        public IBinder onBind(Intent intent) {
        return ioService;
    }
        
    @Override
        public void onDestroy() {
        super.onDestroy();
        callback.set(null);
    }

    private void startReads() {
        int max = maxOutstandingReads.get();
        Log.d(TAG, "startReads() with max=" + max + "; current out="
              + outstandingReads.get());
        while (outstandingReads.incrementAndGet() <= max) {
            new ServiceReadTask(DUMMY_FILE).execute();
        }

        outstandingReads.decrementAndGet();  // decrement the extra one
    }

    private void startWrites() {
        int max = maxOutstandingWrites.get();
        Log.d(TAG, "startWrites() with max=" + max + "; current out="
              + outstandingWrites.get());
        while (outstandingWrites.incrementAndGet() <= max) {
            new ServiceWriteTask(DUMMY_FILE).execute();
        }

        outstandingWrites.decrementAndGet();  // decrement the extra one
    }

    private FileInputStream getFileInputStreamOrNull(String baseFilename) {
        try {
            return openFileInput(baseFilename);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private FileOutputStream getFileOutputStream(String baseFilename) {
        try {
            return openFileOutput(baseFilename, MODE_APPEND);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private class ServiceReadTask extends ReadTask {
        ServiceReadTask(String baseFilename) {
            super(getFileStreamPath(baseFilename),
                  getFileInputStreamOrNull(baseFilename));
        }

        @Override protected void onPreExecute() {
        }

        @Override protected void onPostExecute(Integer duration) {
            Log.d(TAG, "Service read took: " + duration + " ms");
            outstandingReads.decrementAndGet();
            startReads();
        }
    };

    private class ServiceWriteTask extends WriteTask {
        ServiceWriteTask(String baseFilename) {
            super(getFileOutputStream(baseFilename));
        }

        @Override protected void onPreExecute() {
        }

        @Override protected void onPostExecute(Integer duration) {
            Log.d(TAG, "Service write took: " + duration + " ms");
            outstandingWrites.decrementAndGet();
            startWrites();
        }
    };

    private final IIODroidService.Stub ioService = new IIODroidService.Stub() {

            public void registerCallback(IServiceCallback callback) throws RemoteException {
                Log.v(TAG, "Callback attached.");
                IODroidService.this.callback.set(callback);
            }
            
            public void unregisterCallback(IServiceCallback callback) throws RemoteException {
                Log.v(TAG, "Callback detached.");
                IODroidService.this.callback.compareAndSet(callback, null);
            }

            public void setMaxOutstandingWrites(int value) {
                Log.d(TAG, "Setting max outstanding writes to: " + value);
                IODroidService.this.maxOutstandingWrites.set(value);
                startWrites();
            }

            public void setMaxOutstandingReads(int value) {
                Log.d(TAG, "Setting max outstanding reads to: " + value);
                IODroidService.this.maxOutstandingReads.set(value);
                startReads();
            }
        };
}
