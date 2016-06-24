package org.wordpress.android.ui.posts;

import org.wordpress.android.util.ObservableString;

import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class BasePostViewModel {
    public final ObservableInt disabledOverlayVisibility = new ObservableInt(View.GONE);
    public final ObservableString title = new ObservableString();
    public final ObservableInt statusTextVisibility = new ObservableInt(View.GONE);
    public final ObservableString statusText = new ObservableString();
    public final ObservableInt statusTextColor = new ObservableInt();
    public final ObservableField<Drawable> statusTextLeftDrawable = new ObservableField<>();
}
