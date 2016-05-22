package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.ui.posts.PostsListContracts.PageActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PageAdapterView;
import org.wordpress.android.ui.posts.PostsListContracts.PageView;
import org.wordpress.android.widgets.PostListButton;

import android.view.View;

public class PagePresenter implements BasePresenter, PageActionHandler {

    private final PageView mPageView;
    private PageAdapterView mPageAdapterView;
    private final PostsListPost mPostsListPost;

    public PagePresenter(PageView pageView, PostsListPost postsListPost) {
        mPageView = pageView;
        mPostsListPost = postsListPost;
    }

    public void setPageAdapterView(PageAdapterView pageAdapterView) {
        mPageAdapterView = pageAdapterView;
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    @Override
    public void onMoreButtonClick(View view) {
        mPageAdapterView.showPagePopupMenu(view);
    }

    @Override
    public void onPageButtonClick(int buttonType) {
        Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(mPostsListPost.getPostId());
        if (fullPost == null) {
            mPageView.showToast(R.string.post_not_found);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                mPageView.editBlogPostOrPageForResult(mPostsListPost.getPostId(), true);
                break;
            case PostListButton.BUTTON_PUBLISH:
                mPageView.publishPost(fullPost);
                break;
            case PostListButton.BUTTON_VIEW:
                mPageView.browsePostOrPage(WordPress.getCurrentBlog(), fullPost);
                break;
            case PostListButton.BUTTON_PREVIEW:
                mPageView.viewPostPreviewForResult(fullPost, true);
                break;
            case PostListButton.BUTTON_STATS:
                mPageView.viewStatsSinglePostDetails(fullPost, true);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                if (!mPostsListPost.isUploading()) {
//                    trashPost(mPostsListPost);
                }
                break;
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
