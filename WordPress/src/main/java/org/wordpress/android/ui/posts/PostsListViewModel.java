package org.wordpress.android.ui.posts;

import android.content.Context;
import android.databinding.BaseObservable;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PostsView}.
 */
public class PostsListViewModel extends BaseObservable {
    private final Context mContext;

    public PostsListViewModel(Context context) {
        mContext = context;
    }

//    public int getFabVisibility() {
//        return mPostsListPost.isLocalDraft() ? View.GONE : View.VISIBLE;
//    }
}
