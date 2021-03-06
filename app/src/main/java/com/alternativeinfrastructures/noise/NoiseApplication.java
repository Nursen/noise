package com.alternativeinfrastructures.noise;

import android.app.Application;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import com.alternativeinfrastructures.noise.sync.bluetooth.BluetoothSyncService;

public class NoiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FlowManager.init(new FlowConfig.Builder(this).build());

        BluetoothSyncService.startOrPromptBluetooth(this);
    }
}
