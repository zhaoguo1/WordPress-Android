package org.wordpress.android.util.widgets;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {
    private boolean mRefreshing;

    public CustomSwipeRefreshLayout(Context context) {
        super(context);
    }

    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try{
            return super.onTouchEvent(event);
        } catch(IllegalArgumentException e) {
            // Fix for https://github.com/wordpress-mobile/WordPress-Android/issues/2373
            // Catch IllegalArgumentException which can be fired by the underlying SwipeRefreshLayout.onTouchEvent()
            // method.
            // When android support-v4 fixes it, we'll have to remove that custom layout completely.
            AppLog.e(T.UTILS, e);
            return true;
        }
    }

    @BindingAdapter("isRefreshing")
    public static void setRefreshing(View view, final boolean refreshing) {
        final CustomSwipeRefreshLayout swipeRefreshLayout = (CustomSwipeRefreshLayout) view;
        swipeRefreshLayout.mRefreshing = refreshing;

        // Delayed refresh, it fixes https://code.google.com/p/android/issues/detail?id=77712
        // 50ms seems a good compromise (always worked during tests) and fast enough so user can't notice the delay
        if (refreshing) {
            swipeRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // use mRefreshing so if the refresh takes less than 50ms, loading indicator won't show up.
                    swipeRefreshLayout.setRefreshing(swipeRefreshLayout.mRefreshing);
                }
            }, 50);
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
