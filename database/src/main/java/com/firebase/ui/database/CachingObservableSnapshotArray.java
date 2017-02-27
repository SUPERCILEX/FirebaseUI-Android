package com.firebase.ui.database;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(samstern): Document
 */
public abstract class CachingObservableSnapshotArray<T> extends ObservableSnapshotArray<T> {

    private Map<String, T> mObjectCache = new HashMap<>();

    public CachingObservableSnapshotArray() {
        super();
    }

    public CachingObservableSnapshotArray(SnapshotParser<T> parser) {
        super(parser);
    }

    @Override
    public T getObject(int index) {
        String key = get(index).getKey();

        // Return from the cache if possible, otherwise populate the cache and return
        if (mObjectCache.containsKey(key)) {
            return mObjectCache.get(key);
        } else {
            T object = super.getObject(index);
            mObjectCache.put(key, object);
            return object;
        }
    }

    protected void clearData() {
        getSnapshots().clear();
        mObjectCache.clear();
    }

    protected DataSnapshot removeData(int index) {
        DataSnapshot snapshot = getSnapshots().remove(index);
        if (snapshot != null) {
            mObjectCache.remove(snapshot.getKey());
        }

        return snapshot;
    }

    protected void updateData(int index, DataSnapshot snapshot) {
        getSnapshots().set(index, snapshot);
        mObjectCache.remove(snapshot.getKey());
    }


}