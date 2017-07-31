package com.firebase.ui.auth.util.accountlink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Task;

public abstract class ManualMergeService extends Service {
    private final IBinder mBinder = new ManualMergeUtils.MergeBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Nullable
    public abstract Task<Void> onLoadData();

    @Nullable
    public abstract Task<Void> onTransferData(IdpResponse response);
}
