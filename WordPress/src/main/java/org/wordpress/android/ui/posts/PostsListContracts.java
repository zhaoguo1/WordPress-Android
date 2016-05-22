package org.wordpress.android.ui.posts;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.BaseView;
import org.wordpress.android.ui.EmptyViewMessageType;

import android.view.View;

public interface PostsListContracts {
    interface PostsView extends BaseView {
        void newPost();

        void setPosts(PostsListPostList posts, boolean isFetchingPosts);

        void mediaUpdated(long mediaId, String mediaUrl);

        void setRefreshing(boolean refreshing);

        void showLoadMoreProgress();

        void hideLoadMoreProgress();

        void updateEmptyView(EmptyViewMessageType emptyViewMessageType);

        void hideEmptyView();

        void hidePost(PostsListPost postsListPost);

        void withUndo(Undoable undoable);
    }

    interface PostsActionHandler {

        void onLoadMore();

        void requestPosts(boolean loadMore);

        void onFabClick();

        void onTrashPost(PostsListPost postsListPost);
    }

    interface Undoable {
        String getText();

        void onUndo();

        void onDismiss();
    }

    interface PostView extends BaseView {
        void editBlogPostOrPageForResult(long postOrPageId, boolean isPage);

        void publishPost(Post post);

        void browsePostOrPage(Blog blog, Post post);

        void viewPostPreviewForResult(Post post, boolean isPage);

        void viewStatsSinglePostDetails(Post post, boolean isPost);
    }

    interface PostAdapterView {
        void animateButtonRows(boolean showRow1);
    }

    interface PostActionHandler {

        void onPostSelected();

        void onPostButtonClick(View view);

        void onPostButtonClick(int buttonType);
    }

    interface PagesActionHandler {

        void onTrashPost(PostsListPost postsListPost);
    }

    interface PageView extends PostView {
    }

    interface PageAdapterView {
        void showPagePopupMenu(View view);
    }

    interface PageActionHandler {
        void onMoreButtonClick(View view);

        void onPageButtonClick(int buttonType);
    }
}
