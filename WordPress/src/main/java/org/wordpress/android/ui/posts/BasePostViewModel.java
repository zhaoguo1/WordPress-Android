package org.wordpress.android.ui.posts;

import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.ObservableString;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class BasePostViewModel extends BaseObservable {
    protected Context mContext;
    private PostsListPost mPostsListPost;
    private PostPresenter mPostPresenter;

    public final ObservableString title = new ObservableString();
    public final ObservableInt statusTextVisibility = new ObservableInt(View.GONE);
    public final ObservableString statusText = new ObservableString();
    public final ObservableInt statusTextColor = new ObservableInt();
    public final ObservableField<Drawable> statusTextLeftDrawable = new ObservableField<>();

    public PostPresenter getPostPresenter() {
        return mPostPresenter;
    }

    public void setPostPresenter(PostPresenter postPresenter) {
        mPostPresenter = postPresenter;
        mPostsListPost = postPresenter.getPostsListPost();
        notifyChange();
    }

    public BasePostViewModel(PostsListPost postsListPost) {
        mPostsListPost = postsListPost;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public PostsListPost getPostsListPost() {
        return mPostsListPost;
    }
}
