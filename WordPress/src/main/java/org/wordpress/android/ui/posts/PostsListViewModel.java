package org.wordpress.android.ui.posts;

import org.wordpress.android.BR;
import org.wordpress.android.R;
import org.wordpress.android.databinding.PostListFragmentBinding;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Handler;
import android.view.View;

public class PostsListViewModel extends BaseObservable implements PostsListContracts.PostsViewModel {
    private final Context mContext;
    private final PostListFragmentBinding mPostListFragmentBinding;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private boolean mFabVisible = true;
    private boolean mIsRefreshing;

    public PostsListViewModel(Context context, PostListFragmentBinding postListFragmentBinding) {
        mContext = context;
        mPostListFragmentBinding = postListFragmentBinding;
    }

    public void setSwipeToRefreshHelper(SwipeToRefreshHelper swipeToRefreshHelper) {
        mSwipeToRefreshHelper = swipeToRefreshHelper;
    }

    @Bindable
    public int getFabVisibility() {
        return mFabVisible ? View.VISIBLE : View.GONE;
    }

    @Override
    public void hideFab() {
        setFabVisibility(false);
    }

    @Override
    public void slideFabInIfHidden() {
        // scale in the fab after a brief delay
        long delayMs = mContext.getResources().getInteger(R.integer.fab_animation_delay);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AniUtils.scaleIn(mPostListFragmentBinding.fabButton, AniUtils.Duration.MEDIUM, new
                        AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setFabVisibility(true);
                    }
                });
            }
        }, delayMs);
    }

    private void setFabVisibility(boolean visible) {
        mFabVisible = visible;
        notifyPropertyChanged(BR.fabVisibility);
    }

    @Bindable
    public boolean getIsRefreshing() {
        return mIsRefreshing;
    }

    @Override
    public void setIsRefreshing(boolean refreshing) {
        mIsRefreshing = refreshing;
        notifyPropertyChanged(BR.isRefreshing);
    }
}
