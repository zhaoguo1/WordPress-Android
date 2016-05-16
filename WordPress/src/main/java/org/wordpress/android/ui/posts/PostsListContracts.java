package org.wordpress.android.ui.posts;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.BaseView;
import org.wordpress.android.ui.EmptyViewMessageType;

public interface PostsListContracts {
    interface PostsView extends BaseView {
        void setPostsActionHandler(PostsActionHandler postsActionHandler);

        void newPost();

        void setPosts(PostsListPostList posts, boolean isFetchingPosts);

        void mediaUpdated(long mediaId, String mediaUrl);

        void setRefreshing(boolean refreshing);

        void showLoadMoreProgress();

        void hideLoadMoreProgress();

        void updateEmptyView(EmptyViewMessageType emptyViewMessageType);

        void hideEmptyView();
    }

    interface PostsActionHandler {

        void onLoadMore();
        void requestPosts(boolean loadMore);
        void onFabClick();
    }

    interface Undoable {
        String getText();

        void onUndo();

        void onDismiss();
    }

    interface PostView extends BaseView {
        void setPostActionHandler(PostActionHandler postActionHandler);

        void editBlogPostOrPageForResult(long postOrPageId, boolean isPage);

        void publishPost(Post post);

        void browsePostOrPage(Blog blog, Post post);

        void viewPostPreviewForResult(Post post, boolean isPage);

        void viewStatsSinglePostDetails(Post post, boolean isPost);

        void hidePost(PostsListPost postsListPost);

        void withUndo(Undoable undoable);
    }

    interface PostActionHandler {

        void onPostSelected();

        void onPostButtonClick(android.view.View view);
    }
}
