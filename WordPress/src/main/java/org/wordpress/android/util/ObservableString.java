package org.wordpress.android.util;

import android.databinding.ObservableField;

public class ObservableString extends ObservableField<String> {
    public ObservableString() {
    }

    public ObservableString(String value) {
        super(value);
    }

    public void set(String value) {
        if (!StringUtils.equals(get(), value)) {
            super.set(value);
        }
    }
}
