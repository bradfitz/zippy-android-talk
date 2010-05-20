package com.google.android.iodroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class IODroidActivity extends Activity {
    private static final String TAG = "IODroidActivity";

    private static final String DUMMY_FILE = "dummy_file";
   
    private IIODroidService serviceStub = null;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    
    private TextView statusText;
    private LinearLayout viewRoot;
    private Button readButton;
    private Button writeButton;

    private final ScheduledThreadPoolExecutor backgroundExecutor =
        new ScheduledThreadPoolExecutor(1);
	
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceStub = IIODroidService.Stub.asInterface(service);
        	Log.v(TAG, "Service bound");
        	uiThreadHandler.post(new Runnable() {
        	    public void run() {
        	        updateUIFromServiceState();
        	    }
        	});
        	try {
        	    serviceStub.registerCallback(serviceCallback);
        	} catch (RemoteException e) {
        	    e.printStackTrace();
        	}
        }

        public void onServiceDisconnected(ComponentName name) {
            serviceStub = null;
        };
    };
    
    private final static int FOO = 1;
    
    private Handler uiThreadHandler = new Handler() {
        @Override
        public void handleMessage (Message msg) {
            if (msg.what == FOO) {
            }
        }
    };

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        viewRoot = (LinearLayout) findViewById(R.id.root_layout);
        statusText = (TextView) findViewById(R.id.status);
        readButton = (Button) findViewById(R.id.read_button);
        writeButton = (Button) findViewById(R.id.write_button);

        readButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    new ActivityReadTask(DUMMY_FILE).execute();
                }
	    });

        writeButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    new ActivityWriteTask(DUMMY_FILE).execute();
                }
	    });

        final CheckBox serviceReadsCheckbox =
            (CheckBox) findViewById(R.id.background_reads);
        final CheckBox serviceWritesCheckbox =
            (CheckBox) findViewById(R.id.background_writes);

        serviceReadsCheckbox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    final boolean checked = serviceReadsCheckbox.isChecked();
                    Log.d(TAG, "reads checkbox: " + checked);
                    if (serviceStub == null) {
                        Log.d(TAG, "not bound to service!");
                        return;
                    }
                    try {
                        serviceStub.setMaxOutstandingReads(checked ? 5 : 0);
                    } catch (RemoteException e) {
                        Log.e(TAG, "error registering callback: " + e);
                    }
                }
            });

        serviceWritesCheckbox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    final boolean checked = serviceWritesCheckbox.isChecked();
                    Log.d(TAG, "writes checkbox: " + checked);
                    if (serviceStub == null) {
                        Log.d(TAG, "not bound to service!");
                        return;
                    }
                    try {
                        serviceStub.setMaxOutstandingWrites(checked ? 5 : 0);
                    } catch (RemoteException e) {
                        Log.e(TAG, "error registering callback: " + e);
                    }
                }
            });


    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume...");

        // Start it and have it run forever (until it shuts itself down).
        // This is required so swapping out the activity (and unbinding the
        // service connection in onPause) doesn't cause the service to be
        // killed due to zero refcount.  This is our signal that we want
        // it running in the background.
        startService(new Intent(this, IODroidService.class));
        
        bindService(new Intent(this, IODroidService.class),
                    serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + serviceStub);
        
        if (serviceStub != null) {
            updateUIFromServiceState();
            try {
                serviceStub.registerCallback(serviceCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "error registering callback: " + e);
            }
        }
    }

    // Should only be called from the UI thread.
    private void updateUIFromServiceState() {
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (serviceStub != null) {
            try {
                serviceStub.unregisterCallback(serviceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.iodroid, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	// Only show one of connect and disconnect:
    	//MenuItem connect = menu.findItem(R.id.menu_item_connect);
    	//connect.setVisible(!connected);
    	return true;	
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
      	case R.id.menu_item_settings:
            //SettingsActivity.show(this);
            return true;
        case R.id.menu_item_about:
            //showDialog(DIALOG_ABOUT);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    // To be run only from UI thread.
    private void setUIEnabled(boolean status) {
        readButton.setEnabled(status);
        writeButton.setEnabled(status);
    }

    private IServiceCallback serviceCallback = new IServiceCallback.Stub() {
            public void onStatsChanged(int writes, int writesPerSecond) {
            }
    };

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

    private class ActivityReadTask extends ReadTask {
        ActivityReadTask(String baseFilename) {
            super(getFileStreamPath(baseFilename),
                  getFileInputStreamOrNull(baseFilename));
        }

        @Override protected void onPreExecute() {
            setUIEnabled(false);
            viewRoot.setBackgroundColor(Color.BLUE);
        }

        @Override protected void onPostExecute(Integer duration) {
            viewRoot.setBackgroundColor(Color.BLACK);
            setUIEnabled(true);
            statusText.setText("Read took: " + duration + " ms");
        }
    };

    private class ActivityWriteTask extends WriteTask {
        ActivityWriteTask(String baseFilename) {
            super(getFileOutputStream(baseFilename));
        }

        @Override protected void onPreExecute() {
            setUIEnabled(false);
            viewRoot.setBackgroundColor(Color.RED);
        }

        @Override protected void onPostExecute(Integer duration) {
            viewRoot.setBackgroundColor(Color.BLACK);
            setUIEnabled(true);
            statusText.setText("Write took: " + duration + " ms");
        }
    };
}
