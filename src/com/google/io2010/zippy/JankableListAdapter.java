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
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView;

/**
 *
 * @author Brad Fitzpatrick, bradfitz@android.com
 */
public class JankableListAdapter implements ListAdapter
{
    private static final String TAG = "JankableListAdapter";

    private static final String[] CODE_NAMES = new String[] {
        "1.0",
        "Petit Four",
        "Cupcake",
        "Donut",
        "Eclair",
        "Froyo",
        "Gingerbread",
        "Haggis",
        "Icelandic Icing",
        "Jalape\u00f1o",
        "Koala Krisps",
        "Liver",
        "Minced Meat",
        "Nuts",
        "Otter",
        "Penguin",
        "Quail",
        "Rabbit",
        "Salad",
        "Taco",
        "Umbilical Cord",
        "Vodka",
        "Wurst",
        "Xiaodianxin",
        "Yoghurt",
        "Zatar",
    };

    private final int mJankMillis;
    private final LayoutInflater mInflater;
    private final boolean mAsyncEffect;

    public JankableListAdapter(LayoutInflater inflater,
                               int jankMillis,
                               boolean asyncEffect) {
        super();
        mInflater = inflater;
        mJankMillis = jankMillis;
	mAsyncEffect = asyncEffect;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = convertView != null
	    ? convertView
	    : mInflater.inflate(R.layout.list_item, null);

        // Note which position this View is currently being used for,
        // so we can change it later in our Async callback, to see if
        // it's still the same one then.  (so it doesn't get recycled
        // to be somebody else's then...)
        view.setTag(R.string.tag_async_list_pos, position);

        final TextView tv = (TextView) view.findViewById(R.id.textview_in_list_item);
	final ImageView iv = (ImageView) view.findViewById(R.id.star_in_list_item);
        final String newText = CODE_NAMES[position];
        tv.setText(newText);
	iv.setImageResource(android.R.drawable.btn_star_big_on);

	if (mJankMillis > 0) {
	    try {
		Thread.sleep(mJankMillis);
	    } catch (InterruptedException e) {}
	}

	if (!mAsyncEffect) {
	    return view;
	}

        tv.setTextSize(15.0f);
        tv.setTypeface(Typeface.create(tv.getTypeface(), Typeface.NORMAL));
	iv.setImageResource(android.R.drawable.btn_star_big_off);

	new AsyncTask<Void, Void, Void>() {
            @Override protected Void doInBackground(Void... unused) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                return null;
            }

            @Override protected void onPostExecute(Void result) {
                if (Integer.valueOf(position).equals(view.getTag(R.string.tag_async_list_pos))) {
                    // The view (and its children) is still the same,
                    // and hasn't been re-used by the ListView.
                    iv.setImageResource(android.R.drawable.btn_star_big_on);
                    tv.setTypeface(Typeface.create(tv.getTypeface(), Typeface.BOLD));
                    tv.setTextSize(25.0f);
                } else {
                    Log.v(TAG, "async callback done, but View's been re-used. ignoring.");
                }
            }
	}.execute();

        return view;
    }

    public boolean areAllItemsEnabled() { return true; }
    public boolean isEnabled(int position) { return true; }
    public int getCount() { return CODE_NAMES.length; }
    public boolean isEmpty() { return false; }
    public Object getItem(int position) { return CODE_NAMES[position]; }
    public long getItemId(int position) { return position; }
    public int getItemViewType(int position) { return 42; }
    public boolean hasStableIds() { return true; }
    public void registerDataSetObserver(DataSetObserver observer) {}
    public void unregisterDataSetObserver(DataSetObserver observer) {}
    public int getViewTypeCount() { return 1; }
}
