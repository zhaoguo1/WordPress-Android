package org.wordpress.android.ui.posts;

import org.wordpress.android.util.DisplayUtils;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.ObservableFloat;
import android.view.View;
import android.view.ViewGroup;

public class EndlistIndicatorViewModel {
    public final ObservableFloat layoutHeight = new ObservableFloat();

    public EndlistIndicatorViewModel(Context context, boolean isPage) {
        // endlist indicator height is hard-coded here so that its horz line is in the middle of the fab
        layoutHeight.set(DisplayUtils.dpToPx(context, isPage ? 82 : 74));
    }

    @BindingAdapter("android:layout_height")
    public static void setLayoutHeight(View view, float height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)height;
        view.setLayoutParams(layoutParams);
    }
}
