package com.despectra.android.journal.model;

import android.content.Entity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Dmitry on 01.06.14.
 */
public class EntityIds {
    private long localId;
    private long remoteId;

    public EntityIds(long localId, long remoteId) {
        this.localId = localId;
        this.remoteId = remoteId;
    }

    public static EntityIds fromBundle(Bundle bundle) {
        return new EntityIds(bundle.getLong("local"), bundle.getLong("remote"));
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putLong("local", localId);
        b.putLong("remote", remoteId);
        return b;
    }

    public EntityIds(Parcel parcel) {
        parcel.readParcelable(EntityIds.class.getClassLoader());
        localId = parcel.readLong();
        remoteId = parcel.readLong();
    }

    public long getLocalId() {
        return localId;
    }

    public long getRemoteId() {
        return remoteId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EntityIds)) {
            return false;
        }
        EntityIds that = (EntityIds) o;
        return that.localId == localId && that.remoteId == remoteId;
    }
}
