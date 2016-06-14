package org.wordpress.android.util;

import android.databinding.BaseObservable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class DataBindingUtils {
    public static class AnimationTrigger extends BaseObservable implements Parcelable, Serializable {
        private int mTriggerCountOld;
        private int mTriggerCount;

        public AnimationTrigger() {
        }

        private AnimationTrigger(int triggerCountOld, int triggerCount) {
            mTriggerCountOld = triggerCountOld;
            mTriggerCount = triggerCount;
        }

        public void trigger() {
            mTriggerCount++;
            notifyChange();
        }

        public boolean isTriggered() {
            return mTriggerCount != mTriggerCountOld;
        }

        public void clearTrigger() {
            mTriggerCountOld = mTriggerCount;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mTriggerCountOld);
            dest.writeInt(mTriggerCount);
        }

        public static final Parcelable.Creator<AnimationTrigger> CREATOR = new Parcelable.Creator<AnimationTrigger>() {
            @Override
            public AnimationTrigger createFromParcel(Parcel source) {
                return new AnimationTrigger(source.readInt(), source.readInt());
            }

            @Override
            public AnimationTrigger[] newArray(int size) {
                return new AnimationTrigger[size];
            }
        };

    }
}
