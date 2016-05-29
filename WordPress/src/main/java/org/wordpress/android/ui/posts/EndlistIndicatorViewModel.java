package org.wordpress.android.ui.posts;

import org.wordpress.android.util.DisplayUtils;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.view.ViewGroup;

public class EndlistIndicatorViewModel extends BaseObservable {
    private final int mEndlistIndicatorHeight;

    public EndlistIndicatorViewModel(Context context, boolean isPage) {
        // endlist indicator height is hard-coded here so that its horz line is in the middle of the fab
        mEndlistIndicatorHeight = DisplayUtils.dpToPx(context, isPage ? 82 : 74);
    }

    public int getLayoutHeight() {
        return mEndlistIndicatorHeight;
    }

    @BindingAdapter("android:layout_height")
    public static void setLayoutHeight(View view, float height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)height;
        view.setLayoutParams(layoutParams);
    }
}
