package com.google.android.iodroid;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

class WriteTask extends AsyncTask<Void, Void, Integer> {
    private final static String TAG = "WriteTask";

    private final FileOutputStream fos;

    public WriteTask(FileOutputStream fos) {
        this.fos = fos;
    }

    @Override protected Integer doInBackground(Void... v) {
        if (this.fos == null) {
            return -1;
        }
        final long startMillis = System.currentTimeMillis();
        try {
            fos.write((int) 'A');
            fos.flush();
            fos.getFD().sync();  // fsync
            fos.close();
        } catch (IOException e) {
            return -1;
        }
        return (int) (System.currentTimeMillis() - startMillis);
    }
}
