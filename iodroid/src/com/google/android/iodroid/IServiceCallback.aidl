package com.google.android.iodroid;

oneway interface IServiceCallback {
  void onStatsChanged(int writes, int writesPerSecond);
}

