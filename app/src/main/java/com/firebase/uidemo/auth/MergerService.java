package com.firebase.uidemo.auth;

import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.accountlink.ManualMergeService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MergerService extends ManualMergeService {
    private Iterable<DataSnapshot> mChatKeys;

    @Override
    public Task<Void> onLoadData() {
        final TaskCompletionSource<Void> loadTask = new TaskCompletionSource<>();
        FirebaseDatabase.getInstance()
                .getReference()
                .child("chatIndices")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        mChatKeys = snapshot.getChildren();
                        loadTask.setResult(null);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("TAG", "message", error.toException());
                    }
                });
        return loadTask.getTask();
    }

    @Override
    public Task<Void> onTransferData(IdpResponse response) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatIndices = FirebaseDatabase.getInstance()
                .getReference()
                .child("chatIndices")
                .child(uid);
        for (DataSnapshot snapshot : mChatKeys) {
            chatIndices.child(snapshot.getKey()).setValue(true);
            DatabaseReference chat = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chats")
                    .child(snapshot.getKey());
            chat.child("uid").setValue(uid);
            chat.child("name").setValue("User " + uid.substring(0, 6));
        }
        return null;
    }
}
