//; -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil -*-

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
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

/**
 *
 * @author Brad Fitzpatrick, bradfitz@android.com
 */
public class JankyListViewActivity extends Activity
{
    ListView mJankyList;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jankylist);

        mJankyList = (ListView) findViewById(R.id.jankylistview);
    }

    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        mJankyList.setAdapter(new JankableListAdapter(
            getLayoutInflater(),
            intent.getExtras().getInt("latency", 0),
            intent.getExtras().getBoolean("async", false)));
    }
}
