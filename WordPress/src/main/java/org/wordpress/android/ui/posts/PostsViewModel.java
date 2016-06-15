package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DataBindingUtils.AnimationTrigger;
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

import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

public class PostsViewModel {
    public final ObservableInt fabVisibility = new ObservableInt(View.GONE);
    public final ObservableBoolean isRefreshing = new ObservableBoolean(false);
    public final ObservableInt loadMoreProgressVisibility = new ObservableInt(View.GONE);
    public final ObservableInt emptyViewVisibility = new ObservableInt(View.GONE);
    public final ObservableInt emptyViewImageVisibility = new ObservableInt(View.VISIBLE);
    public final ObservableField<Integer> emptyViewTitle = new ObservableField<>(R.string.empty_list_default);
    public final ObservableList<BasePostPresenter<?>> postPresenters = new ObservableArrayList<>();
    public final ObservableBoolean isPage;

    public AnimationTrigger animTriggered = new AnimationTrigger();

    public PostsViewModel(boolean isPage) {
        this.isPage = new ObservableBoolean(isPage);
    }

    @BindingAdapter({"slideInDelayMS", "animTriggered"})
    public static void visibilityAdapter(final FloatingActionButton fab, long delayMS, AnimationTrigger animTriggered) {
        if (!animTriggered.isTriggered()) {
            return;
        }

        animTriggered.clearTrigger();

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

    @BindingAdapter("postsActionHandler")
    public static void setSwipeToRefreshHelper(CustomSwipeRefreshLayout ptrLayout, final PostsActionHandler
            postsActionHandler) {
        new SwipeToRefreshHelper(
                ptrLayout.getContext(),
                ptrLayout,
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        postsActionHandler.onRefreshRequested();
                    }
                });
    }
}
