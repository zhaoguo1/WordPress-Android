package org.wordpress.android.ui.posts;

import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ObservableList;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

public class PostsViewModel {
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    public final ObservableInt fabVisibility = new ObservableInt(View.GONE);
    public final ObservableBoolean isRefreshing = new ObservableBoolean(false);
    public final ObservableInt loadMoreProgressVisibility = new ObservableInt(View.GONE);
    public final ObservableInt emptyViewVisibility = new ObservableInt(View.GONE);
    public final ObservableInt emptyViewImageVisibility = new ObservableInt(View.VISIBLE);
    public final ObservableField<String> emptyViewTitle = new ObservableField<>("");
    public final ObservableList<BasePostViewModel> postViewModels = new ObservableArrayList<>();
    public final ObservableBoolean isPage;

    public PostsViewModel(boolean isPage) {
        this.isPage = new ObservableBoolean(isPage);
    }

    public void setSwipeToRefreshHelper(SwipeToRefreshHelper swipeToRefreshHelper) {
        mSwipeToRefreshHelper = swipeToRefreshHelper;
    }

    @BindingAdapter({"slideInDelayMS", "android:visibility"})
    public static void visibilityAdapter(final FloatingActionButton fab, long delayMS, int visibility) {
        if (visibility != View.VISIBLE) {
            fab.setVisibility(visibility);
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AniUtils.scaleIn(fab, AniUtils.Duration.MEDIUM, new
                        AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                fab.setVisibility(View.VISIBLE);
                            }
                        });
            }
        }, delayMS);
    }
}
