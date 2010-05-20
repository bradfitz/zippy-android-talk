//; -*- mode: Java; c-basic-offset: 4;  -*-

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.io2010.zippy;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This is the sample code to accompany the Google I/O 2010 talk
 * on "Writing Zippy Android Apps".  It mostly shows what _not_ to do,
 * so I wouldn't use this code for anything.... :)
 *
 * @author Brad Fitzpatrick, bradfitz@android.com
 */
public class ZippyAndroidDemoActivity extends Activity
{
    private static final String TAG = "ZippyAndroid";

    private static final int DB_ROWS = 500;

    // Constants for Dialogs:
    private static final int DIALOG_PROGRESS = 0;

    // Constants for Handler Message 'what' values:
    private static final int MESSAGE_BAD_SLEEP = 0;

    ProgressDialog mProgressDialog = null;
    View mBackgroundView;

    // Handler on the main thread, and also called by the main thread.
    // Typically you make Handlers to be called from non-UI threads to
    // send a message to the UI thread, but we're making this one
    // solely for the weird purpose of flashing the background red first,
    // and then showing what _NOT_ to do:  stalling the main thread,
    // with our sleep below.
    private final Handler mHandler = new Handler() {
	    @Override public void handleMessage (Message m) {
		switch (m.what) {
		case MESSAGE_BAD_SLEEP:
		    Log.v(TAG, "Sleeping on the UI thread for " + m.arg1 + " ms.");
		    try {
			Thread.sleep(m.arg1);
		    } catch (InterruptedException e) {}
		    mBackgroundView.setBackgroundColor(Color.BLACK);
		    break;
		default:
		    Log.v(TAG, "Bogus message: " + m.what);
		}
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);

	mBackgroundView = (View) findViewById(R.id.view_root);

        Button janky = (Button) findViewById(R.id.janky_button);
        janky.setOnClickListener(onClickDelay(600));

        Button anr1 = (Button) findViewById(R.id.anr1);
        anr1.setOnClickListener(onClickDelay(6000));

        final Button asyncTask = (Button) findViewById(R.id.asynctask);
        asyncTask.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    asyncTask.setEnabled(false);
                    ZippyAndroidDemoActivity.this.setProgressBarIndeterminateVisibility(true);

                    new AsyncTask<Void, Void, Void>() {
                        @Override protected Void doInBackground(Void... unused) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {}
                            return null;
                        }

                        @Override protected void onPostExecute(Void result) {
                            ZippyAndroidDemoActivity.this.setProgressBarIndeterminateVisibility(false);
                            asyncTask.setEnabled(true);
                        }
                    }.execute();
                }
            });

        final Button smoothListView = (Button) findViewById(R.id.start_smoothlistview_button);
        smoothListView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), JankyListViewActivity.class);
                    intent.putExtra("latency", 0);
                    startActivity(intent);
                }
            });

        final Button jankyListView = (Button) findViewById(R.id.start_jankylistview_button);
        jankyListView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), JankyListViewActivity.class);
                    intent.putExtra("latency", 60);
                    startActivity(intent);
                }
            });

        final Button asyncListView = (Button) findViewById(R.id.start_asynclistview_button);
        asyncListView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), JankyListViewActivity.class);
                    intent.putExtra("latency", 0);
                    intent.putExtra("async", true);  // stars loading in
                    startActivity(intent);
                }
            });

        final Button lockFightButton = (Button) findViewById(R.id.start_lock_fight_button);
        lockFightButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    lockFightButton.setEnabled(false);
		    final ProgressDialog progress =
			ProgressDialog.show(
			    ZippyAndroidDemoActivity.this,
			    "Please wait",
			    "Mutexes are fighting...",
			    false /* indeterminate */,
			    false /* cancelable */);

                    new AsyncTask<Void, Void, Void>() {
                        @Override protected Void doInBackground(Void... unused) {
                            final Object someLock = new Object();
                            Thread t1 = new LockFighterThread(someLock);
                            Thread t2 = new LockFighterThread(someLock);
                            t1.start();
                            t2.start();
                            try {
                                t1.join();
                                t2.join();
                            } catch (InterruptedException e) {}
                            return null;  // Void
                        }

                        @Override protected void onPostExecute(Void result) {
                            ZippyAndroidDemoActivity.this.setProgressBarIndeterminateVisibility(false);
			    progress.dismiss();
			    lockFightButton.setEnabled(true);
                        }
                    }.execute();
                }
            });

        final Button initDbButton = (Button) findViewById(R.id.init_db_button);
        initDbButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    initDbButton.setEnabled(false);

		    showDialog(DIALOG_PROGRESS);
		    mProgressDialog.setProgress(0);

                    new AsyncTask<Void, Integer, Void>() {
                        @Override protected Void doInBackground(Void... unused) {
			    publishProgress(1);
			    StringBuffer sb = new StringBuffer(1024);
			    for (int i = 0; i < 1024*10; ++i) {
				sb.append("a");
			    }
			    String junkString = sb.toString();

			    publishProgress(2);
			    deleteDatabase("demo.db");
			    publishProgress(3);
			    SQLiteDatabase db = openOrCreateDatabase("demo.db",
								     MODE_PRIVATE,
								     null);
			    publishProgress(4);
			    db.execSQL("DROP TABLE IF EXISTS foo");
			    publishProgress(5);
			    db.execSQL("CREATE TABLE foo (pk INT PRIMARY KEY, prop1 INT, prop2 INT, junk TEXT)");
			    publishProgress(6);
			    db.execSQL("CREATE INDEX prop1idx ON foo (prop1)");
			    publishProgress(7);
			    db.beginTransaction();
			    try {
				for (int i = 0; i < DB_ROWS; ++i) {
				    ContentValues cv = new ContentValues();
				    cv.put("pk", i);
				    cv.put("prop1", (int) (Math.random() * 100));
				    cv.put("prop2", (int) (Math.random() * 100));
				    cv.put("junk", junkString);
				    long rowId = db.insertOrThrow("foo", "pk", cv);
				    Log.v(TAG, "Inserted row: " + rowId);
				    publishProgress(i);
				}
				db.setTransactionSuccessful();
			    } finally {
				db.endTransaction();
			    }
			    db.close();
			    return null; // void
			}

			@Override protected void onProgressUpdate(Integer... progressValue) {
			    mProgressDialog.setProgress(progressValue[0]);
			}


                        @Override protected void onPostExecute(Void result) {
			    mProgressDialog.dismiss();
			    initDbButton.setEnabled(true);
                        }
		    }.execute();
		}
	    });

        final Button slowUpdateButton = (Button) findViewById(R.id.slow_update_button);
        slowUpdateButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    SQLiteDatabase db = openOrCreateDatabase("demo.db",
							     MODE_PRIVATE,
							     null);
                    // Append a character to every row's junk column.
                    // This will be slow on big databases.
                    db.execSQL("UPDATE foo SET junk = junk || 'a'");
                    db.close();
                }
            });

        final Button slowDbButton = (Button) findViewById(R.id.start_slow_db_button);
        slowDbButton.setOnClickListener(newDbClicker("prop2"));

        final Button fastDbButton = (Button) findViewById(R.id.start_fast_db_button);
        fastDbButton.setOnClickListener(newDbClicker("prop1"));
    }

    @Override protected Dialog onCreateDialog(int id) {
	switch (id) {
	case DIALOG_PROGRESS:
	    mProgressDialog = new ProgressDialog(this);
	    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    mProgressDialog.setMessage("Initializing database...");
	    mProgressDialog.setMax(DB_ROWS);
	    return mProgressDialog;
	default:
	    return null;
	}
    }

    // Returns an onclick handler to sleep on the UI thread for the
    // provided number of milliseconds.  Instead of doing the sleep
    // directly, we instead turn the background red, then send
    // ourselves a message (back to the same thread) to do the bogus
    // event-loop-blocking sleep (and turn screen back black), just so
    // in-between the view system will repaint.  Had we just done
    // red+sleep+black in one callback, the red would never be seen.
    private View.OnClickListener onClickDelay(final int millis) {
            return new View.OnClickListener() {
                public void onClick(View view) {
		    mBackgroundView.setBackgroundColor(Color.RED);
		    Message m = mHandler.obtainMessage(MESSAGE_BAD_SLEEP);
		    m.arg1 = millis;
		    mHandler.sendMessage(m);
		}
            };
    }

    private View.OnClickListener newDbClicker(final String column) {
	return new View.OnClickListener() {
                public void onClick(View view) {
		    SQLiteDatabase db = openOrCreateDatabase("demo.db",
							     MODE_PRIVATE,
							     null);
		    Cursor c = db.rawQuery("SELECT pk, junk FROM foo WHERE " + column + "=?",
					   new String[]{"57"});
		    try {
			int rows = 0;
			while (c.moveToNext()) {
			    rows++;
			}
			Log.v(TAG, column + " matching rows: " + rows);
		    } finally {
			c.close();
		    }
		    db.close();
		}
	};
    }

    private static class LockFighterThread extends Thread {
        private final Object mLock;

        public LockFighterThread(Object lockToFightOver) {
            mLock = lockToFightOver;
        }

        @Override public void run() {
            for (int i = 0; i < 20; ++i) {
                synchronized (mLock) {
                    randomSleep(250);
                }
                randomSleep(250);
            }
        }

        private void randomSleep(int max) {
            try {
                Thread.sleep((int) (Math.random() * max));
            } catch (InterruptedException e) {}
        }
    }
}
