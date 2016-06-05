package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.ui.posts.PostsListContracts.PostActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.widgets.PostListButton;

import android.view.View;

public class PostPresenter implements BasePresenter, PostActionHandler {

    private final PostView mPostView;
    private final PostViewModel mPostViewModel;
    private final PostsListPost mPostsListPost;

    private final PostsActionHandler mPostsActionHandler;

    public PostPresenter(PostView postView, PostViewModel postViewModel, PostsListPost postsListPost,
            PostsActionHandler postsActionHandler) {
        mPostView = postView;
        mPostViewModel = postViewModel;
        mPostsListPost = postsListPost;

        mPostsActionHandler = postsActionHandler;
    }

    @Override
    public void onPostSelected() {
        onPostButtonClick(PostListButton.BUTTON_PREVIEW);
    }

    @Override
    public void onPostButtonClick(View view) {
        int buttonType = ((PostListButton) view).getButtonType();
        onPostButtonClick(buttonType);
    }

    public void onPostButtonClick(int buttonType) {
        Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(mPostsListPost.getPostId());
        if (fullPost == null) {
            mPostView.showToast(R.string.post_not_found);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                mPostView.editBlogPostOrPageForResult(mPostsListPost.getPostId(), false);
                break;
            case PostListButton.BUTTON_PUBLISH:
                mPostView.publishPost(fullPost);
                break;
            case PostListButton.BUTTON_VIEW:
                mPostView.browsePostOrPage(WordPress.getCurrentBlog(), fullPost);
                break;
            case PostListButton.BUTTON_PREVIEW:
                mPostView.viewPostPreviewForResult(fullPost, false);
                break;
            case PostListButton.BUTTON_STATS:
                mPostView.viewStatsSinglePostDetails(fullPost, false);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                if (!mPostsListPost.isUploading()) {
                    mPostsActionHandler.onTrashPost(mPostsListPost);
                }
                break;
            case PostListButton.BUTTON_MORE:
                mPostViewModel.animateButtonRows(false);
                break;
            case PostListButton.BUTTON_BACK:
                mPostViewModel.animateButtonRows(true);
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
