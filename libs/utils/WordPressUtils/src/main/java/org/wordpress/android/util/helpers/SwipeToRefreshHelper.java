package org.wordpress.android.util.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.TypedValue;

import org.wordpress.android.util.R;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

public class SwipeToRefreshHelper implements OnRefreshListener {
    private CustomSwipeRefreshLayout mSwipeRefreshLayout;
    private RefreshListener mRefreshListener;

    public interface RefreshListener {
        public void onRefreshStarted();
    }

    public SwipeToRefreshHelper(Context context, CustomSwipeRefreshLayout swipeRefreshLayout, RefreshListener listener) {
        init(context, swipeRefreshLayout, listener);
    }

    public void init(Context context, CustomSwipeRefreshLayout swipeRefreshLayout, RefreshListener listener) {
        mRefreshListener = listener;
        mSwipeRefreshLayout = swipeRefreshLayout;
        mSwipeRefreshLayout.setOnRefreshListener(this);
        final TypedArray styleAttrs = obtainStyledAttrsFromThemeAttr(context, R.attr.swipeToRefreshStyle,
                R.styleable.RefreshIndicator);
        int color = styleAttrs.getColor(R.styleable.RefreshIndicator_refreshIndicatorColor, context.getResources()
                .getColor(android.R.color.holo_blue_dark));
        mSwipeRefreshLayout.setColorSchemeColors(color, color, color, color);
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    @Override
    public void onRefresh() {
        mRefreshListener.onRefreshStarted();
    }

    public void setEnabled(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
    }

    public static TypedArray obtainStyledAttrsFromThemeAttr(Context context, int themeAttr, int[] styleAttrs) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(themeAttr, outValue, true);
        int styleResId = outValue.resourceId;
        return context.obtainStyledAttributes(styleResId, styleAttrs);
    }
}
