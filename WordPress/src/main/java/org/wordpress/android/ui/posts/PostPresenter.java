package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostsListContracts.PostActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.PostListButton;

import android.view.View;

public class PostPresenter extends BasePostPresenter<PostViewModel> implements PostActionHandler {
    private final PostView mPostView;
    private final boolean mIsStatsSupported;

    private final PostsActionHandler mPostsActionHandler;

    public PostPresenter(PostView postView, PostsListPost postsListPost, boolean isStatsSupported, PostsActionHandler
            postsActionHandler) {
        super(new PostViewModel(), postsListPost);

        mPostView = postView;
        mIsStatsSupported = isStatsSupported;

        mPostsActionHandler = postsActionHandler;

        displayPost();
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
        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                mPostView.editBlogPostOrPageForResult(mPostsListPost.getPostId(), false);
                return;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                if (!mPostsListPost.isUploading()) {
                    mPostsActionHandler.onTrashPost(mPostsListPost);
                }
                return;
            case PostListButton.BUTTON_MORE:
                mViewModel.animateButtonRows(mViewModel.showRow.get() + 1, canShowStatsForPost(), canPublishPost());
                return;
            case PostListButton.BUTTON_BACK:
                mViewModel.animateButtonRows(0, canShowStatsForPost(), canPublishPost());
                return;
        }

        Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(mPostsListPost.getPostId());
        if (fullPost == null) {
            mPostView.showToast(R.string.post_not_found);
            return;
        }

        switch (buttonType) {
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
        }
    }

    @Override
    public void willBeFirstStart() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    private void displayPost() {
        displayCommon(mViewModel, mPostView.getContext());

        // set excerpt
        if (mPostsListPost.hasExcerpt()) {
            mViewModel.excerpt.set(PostUtils.collapseShortcodes(mPostsListPost.getExcerpt()));
        } else {
            mViewModel.excerpt.set(StringUtils.notNullStr(mPostsListPost.getExcerpt()));
        }

        // set excerpt visibility
        mViewModel.excerptVisibility.set(mPostsListPost.hasExcerpt() ? View.VISIBLE : View.GONE);

        // set featured image url
        if (mPostsListPost.hasFeaturedImageId() || mPostsListPost.hasFeaturedImageUrl()) {
            mViewModel.featuredImageUrl.set(mPostsListPost.getFeaturedImageUrl());
        } else {
            mViewModel.featuredImageUrl.set(null);
        }

        // set featured image visibility
        mViewModel.featuredImageVisibility.set((mPostsListPost.hasFeaturedImageId() || mPostsListPost
                .hasFeaturedImageUrl()) ? View.VISIBLE : View.GONE);

        // set date visibility
        mViewModel.dateVisibility.set(mPostsListPost.isLocalDraft() ? View.GONE : View.VISIBLE);

        // set formatted date
        mViewModel.formattedDate.set(mPostsListPost.getFormattedDate());

        // set Trash button type
        // local drafts say "delete" instead of "trash"
        mViewModel.trashButtonType
                .set(mPostsListPost.isLocalDraft() ? PostListButton.BUTTON_DELETE : PostListButton.BUTTON_TRASH);

        // set view button type
        // posts with local changes have preview rather than view button
        mViewModel.viewButtonType.set((mPostsListPost.isLocalDraft() || mPostsListPost.hasLocalChanges())
                || mPostsListPost.getStatusEnum() == PostStatus.DRAFT
                        ? PostListButton.BUTTON_PREVIEW : PostListButton.BUTTON_VIEW);

        final boolean hasEnoughRoom = hasEnoughRoom();

        // set Publish button visibility
        mViewModel.publishButtonVisibility.set(canPublishPost() && hasEnoughRoom ? View.VISIBLE : View.GONE);

        // if we have enough room to show all buttons, hide the back/more buttons and show stats/trash/publish

        // set Edit button visibility
        mViewModel.editButtonVisibility.set(View.VISIBLE);

        // set View button visibility
        mViewModel.viewButtonVisibility.set(View.VISIBLE);

        // set stats button visibility
        mViewModel.statsButtonVisibility
                .set((canShowStatsForPost() && hasEnoughRoom) ? View.VISIBLE : View.GONE);

        // set Trash button visibility
        mViewModel.trashButtonVisibility.set(hasEnoughRoom ? View.VISIBLE : View.GONE);

        // set More button visibility
        mViewModel.moreButtonVisibility.set(hasEnoughRoom ? View.GONE : View.VISIBLE);

        // set Back button visibility
        mViewModel.backButtonVisibility.set(View.GONE);
    }

    private boolean canShowStatsForPost() {
        return mIsStatsSupported
                && mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED
                && !mPostsListPost.isLocalDraft()
                && !mPostsListPost.hasLocalChanges();
    }

    private boolean hasEnoughRoom() {
        boolean canShowPublishButton = canPublishPost();
        boolean canShowStatsButton = canShowStatsForPost();

        int numVisibleButtons = 3;
        if (canShowPublishButton) numVisibleButtons++;
        if (canShowStatsButton) numVisibleButtons++;

        int displayWidth = mPostView.getDisplayWidth();
        // on larger displays we can always show all buttons
        boolean alwaysShowAllButtons = (displayWidth >= 1080);

        return alwaysShowAllButtons || numVisibleButtons <= 3;
    }

    private boolean canPublishPost() {
        return mPostsListPost != null && !mPostsListPost.isUploading() &&
                (mPostsListPost.hasLocalChanges() || mPostsListPost.isLocalDraft() ||
                        mPostsListPost.getStatusEnum() == PostStatus.DRAFT);
    }
}
