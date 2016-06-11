package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.ui.posts.PostsListContracts.PostActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.PostListButton;

import android.view.View;

public class PostPresenter implements BasePresenter, PostActionHandler {

    private final PostView mPostView;
    private final PostViewModel mPostViewModel;
    private final boolean mIsStatsSupported;

    private PostsListPost mPostsListPost;
    private boolean mAlwaysShowAllButtons;

    private final PostsActionHandler mPostsActionHandler;

    public PostPresenter(PostView postView, PostViewModel postViewModel, PostsListPost postsListPost, boolean
            isStatsSupported, PostsActionHandler postsActionHandler) {
        mPostView = postView;
        mPostViewModel = postViewModel;
        mPostsListPost = postsListPost;
        mIsStatsSupported = isStatsSupported;

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mPostView.getContext());
        // on larger displays we can always show all buttons
        mAlwaysShowAllButtons = (displayWidth >= 3080);

        mPostsActionHandler = postsActionHandler;
    }

    public PostsListPost getPostsListPost() {
        return mPostsListPost;
    }

    public void setPostsListPost(PostsListPost postsListPost) {
        mPostsListPost = postsListPost;
        displayPost();
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
                mPostViewModel.animateButtonRows(false, canShowStatsForPost());
                break;
            case PostListButton.BUTTON_BACK:
                mPostViewModel.animateButtonRows(true, canShowStatsForPost());
                break;
        }
    }

    @Override
    public void init() {
        displayPost();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    private void displayPost() {
        // set excerpt
        if (mPostsListPost.hasExcerpt()) {
            mPostViewModel.excerpt.set(PostUtils.collapseShortcodes(mPostsListPost.getExcerpt()));
        } else {
            mPostViewModel.excerpt.set(StringUtils.notNullStr(mPostsListPost.getExcerpt()));
        }

        // set excerpt visibility
        mPostViewModel.excerptVisibility.set(mPostsListPost.hasExcerpt() ? View.VISIBLE : View.GONE);

        // set featured image url
        if (mPostsListPost.hasFeaturedImageId() || mPostsListPost.hasFeaturedImageUrl()) {
            mPostViewModel.featuredImageUrl.set(mPostsListPost.getFeaturedImageUrl());
        } else {
            mPostViewModel.featuredImageUrl.set(null);
        }

        // set featured image visibility
        mPostViewModel.featuredImageVisibility.set((mPostsListPost.hasFeaturedImageId() || mPostsListPost
                .hasFeaturedImageUrl()) ? View.VISIBLE : View.GONE);

        // set date visibility
        mPostViewModel.dateVisibility.set(mPostsListPost.isLocalDraft() ? View.GONE : View.VISIBLE);

        // set formatted date
        mPostViewModel.formattedDate.set(mPostsListPost.getFormattedDate());

        // set Trash button type
        // local drafts say "delete" instead of "trash"
        mPostViewModel.trashButtonType
                .set(mPostsListPost.isLocalDraft() ? PostListButton.BUTTON_DELETE : PostListButton.BUTTON_TRASH);

        // set view button type
        // posts with local changes have preview rather than view button
        mPostViewModel.viewButtonType.set((mPostsListPost.isLocalDraft() || mPostsListPost.hasLocalChanges()) ?
                PostListButton.BUTTON_PREVIEW : PostListButton.BUTTON_VIEW);

        // if we have enough room to show all buttons, hide the back/more buttons and show stats/trash

        // set More button visibility
        mPostViewModel.moreButtonVisibility.set(hasEnoughRoom() ? View.GONE : View.VISIBLE);

        // set Trash button visibility
        mPostViewModel.trashButtonVisibility.set(hasEnoughRoom() ? View.VISIBLE : View.GONE);

        // set stats button visibility
        mPostViewModel.statsButtonVisibility
                .set((canShowStatsForPost() && hasEnoughRoom()) ? View.VISIBLE : View.GONE);
    }

    private boolean canShowStatsForPost() {
        return mIsStatsSupported
                && mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED
                && !mPostsListPost.isLocalDraft();
    }

    private boolean hasEnoughRoom() {
        boolean canShowStatsButton = canShowStatsForPost();
        int numVisibleButtons = (canShowStatsButton ? 4 : 3);

        return mAlwaysShowAllButtons || numVisibleButtons <= 3;
    }
}
