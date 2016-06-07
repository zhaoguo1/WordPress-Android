package org.wordpress.android.ui.posts;

import org.wordpress.android.BR;
import org.wordpress.android.R;
import org.wordpress.android.databinding.PostListFragmentBinding;
import org.wordpress.android.ui.posts.PostsListContracts.PostsViewModel;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableList;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

public class PostsListViewModel extends BaseObservable implements PostsViewModel {
    private final Context mContext;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private boolean mFabVisible;
    private boolean mIsRefreshing;
    private boolean mLoadMoreProgressVisible;
    private boolean mEmptyViewVisible;
    private boolean mEmptyViewImageVisible = true;
    private @StringRes int mEmptyViewTitleResId = R.string.empty_list_default;

    private boolean mIsPage;
    private ObservableList<BasePostViewModel> mPostViewModels;

    public PostsListViewModel(Context context, boolean isPage) {
        mContext = context;
        mIsPage = isPage;
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

    public void slideFabInIfHidden() {
        setFabVisibility(true);
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

    private void setFabVisibility(boolean visible) {
        if (visible != mFabVisible) {
            mFabVisible = visible;
            notifyPropertyChanged(BR.fabVisibility);
        }
    }

    @Bindable
    public boolean getIsRefreshing() {
        return mIsRefreshing;
    }

    @Override
    public void setIsRefreshing(boolean refreshing) {
        if (refreshing != mIsRefreshing) {
            mIsRefreshing = refreshing;
            notifyPropertyChanged(BR.isRefreshing);
        }
    }

    @Bindable
    public int getLoadMoreProgressVisibility() {
        return mLoadMoreProgressVisible ? View.VISIBLE : View.GONE;
    }

    @Override
    public void setLoadMoreProgressVisibility(boolean visible) {
        if (visible != mLoadMoreProgressVisible) {
            mLoadMoreProgressVisible = visible;
            notifyPropertyChanged(BR.loadMoreProgressVisibility);
        }
    }

    @Bindable
    public int getEmptyViewVisibility() {
        return mEmptyViewVisible ? View.VISIBLE : View.GONE;
    }

    @Override
    public void setEmptyViewVisibility(boolean visible) {
        if (visible != mEmptyViewVisible) {
            mEmptyViewVisible = visible;
            notifyPropertyChanged(BR.emptyViewVisibility);
        }
    }

    @Bindable
    public CharSequence getEmptyViewTitle() {
        return mContext.getText(mEmptyViewTitleResId);
    }

    @Override
    public void setEmptyViewTitle(@StringRes int emptyViewTitleResId) {
        if (emptyViewTitleResId != mEmptyViewTitleResId) {
            mEmptyViewTitleResId = emptyViewTitleResId;
            notifyPropertyChanged(BR.emptyViewTitle);
        }
    }

    @Bindable
    public int getEmptyViewImageVisibility() {
        return mEmptyViewImageVisible ? View.VISIBLE : View.GONE;
    }

    @Override
    public void setEmptyViewImageVisibility(boolean visible) {
        if (visible != mEmptyViewImageVisible) {
            mEmptyViewImageVisible = visible;
            notifyPropertyChanged(BR.emptyViewImageVisibility);
        }
    }

    @Bindable
    public boolean getIsPage() {
        return mIsPage;
    }

    @Bindable
    public ObservableList<BasePostViewModel> getPostViewModels() {
        return mPostViewModels;
    }

    @Override
    public void setPosts(ObservableList<BasePostViewModel> postViewModels) {
        mPostViewModels = postViewModels;
        notifyPropertyChanged(BR.postViewModels);
    }
}
