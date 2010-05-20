package com.google.android.iodroid;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

class ReadTask extends AsyncTask<Void, Void, Integer> {
    private final static String TAG = "ReadTask";

    private static final Random RANDOM = new Random();

    private long startMillis;
    private long endMillis;
    private final FileInputStream fis;
    private final File file;

    public ReadTask(File file, FileInputStream fis) {
        this.file = file;
        this.fis = fis;
    }

    @Override protected Integer doInBackground(Void... v) {
        if (!file.isFile()) {
            return -1;
        }
        synchronized (this) {
            startMillis = System.currentTimeMillis();
        }
        long endMillis;
        try {
            int seek = RANDOM.nextInt((int) file.length());
            fis.skip((long) seek);
            int byteRead = fis.read();
            endMillis = System.currentTimeMillis();
            Log.d(TAG, "Read byte: " + byteRead + " at " + seek);
        } catch (IOException e) {
            return -1;
        }
        synchronized (this) {
            return new Long(endMillis - startMillis).intValue();
        }
    }
}
