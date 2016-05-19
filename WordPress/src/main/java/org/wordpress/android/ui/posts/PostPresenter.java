package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.widgets.PostListButton;

import android.view.View;

public class PostPresenter implements BasePresenter, PostsListContracts.PostActionHandler {

    private final PostsListContracts.PostView mPostView;
    private final PostsListContracts.AdapterView mAdapterView;
    private final PostsListPost mPostsListPost;
    private final boolean mIsPage;

    public PostPresenter(PostsListContracts.PostView postView, PostsListContracts.AdapterView adapterView,
            PostsListPost postsListPost, boolean isPage) {
        mPostView = postView;
        mAdapterView = adapterView;
        mPostsListPost = postsListPost;
        mIsPage = isPage;
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
                mPostView.editBlogPostOrPageForResult(mPostsListPost.getPostId(), mIsPage);
                break;
            case PostListButton.BUTTON_PUBLISH:
                mPostView.publishPost(fullPost);
                break;
            case PostListButton.BUTTON_VIEW:
                mPostView.browsePostOrPage(WordPress.getCurrentBlog(), fullPost);
                break;
            case PostListButton.BUTTON_PREVIEW:
                mPostView.viewPostPreviewForResult(fullPost, mIsPage);
                break;
            case PostListButton.BUTTON_STATS:
                mPostView.viewStatsSinglePostDetails(fullPost, mIsPage);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                if (!mPostsListPost.isUploading()) {
//                    trashPost(mPostsListPost);
                }
                break;
            case PostListButton.BUTTON_MORE:
                mAdapterView.animateButtonRows(false);
                break;
            case PostListButton.BUTTON_BACK:
                mAdapterView.animateButtonRows(true);
                break;
        }
    }

//    /*
//     * send the passed post to the trash with undo
//     */
//    private void trashPost(final PostsListPost post) {
//        //only check if network is available in case this is not a local draft - local drafts have not yet
//        //been posted to the server so they can be trashed w/o further care
//        if (!post.isLocalDraft() && !NetworkUtils.checkConnection(mPostView.getContext())) {
//            return;
//        }
//
//        final Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(post.getPostId());
//        if (fullPost == null) {
//            mPostView.showToast(R.string.post_not_found);
//            return;
//        }
//
//        // remove post from the list and add it to the list of trashed posts
//        mPostView.hidePost(post);
//        mPosts.remove(post);
//        mTrashedPosts.add(post);
//
//        // make sure empty view shows if user deleted the only post
//        if (mPosts.size() == 0) {
//            mPostView.updateEmptyView(EmptyViewMessageType.NO_CONTENT);
//        }
//
//        mPostView.withUndo(new PostsListContracts.Undoable() {
//            @Override
//            public String getText() {
//                if (post.isLocalDraft()) {
//                    return mIsPage ? mPostView.getString(R.string.page_deleted) : mPostView.getString(R.string
//                            .post_deleted);
//                } else {
//                    return mIsPage ? mPostView.getString(R.string.page_trashed) : mPostView.getString(R.string
//                            .post_trashed);
//                }
//            }
//
//            @Override
//            public void onUndo() {
//                // user undid the trash, so unhide the post and remove it from the list of trashed posts
//                mTrashedPosts.remove(post);
//                mPostView.hidePost(post);
//                mPostView.hideEmptyView();
//            }
//
//            @Override
//            public void onDismiss() {
//                // if the post no longer exists in the list of trashed posts it's because the
//                // user undid the trash, so don't perform the deletion
//                if (!mTrashedPosts.contains(post)) {
//                    return;
//                }
//
//                // remove from the list of trashed posts in case onDismissed is called multiple
//                // times - this way the above check prevents us making the call to delete it twice
//                // https://code.google.com/p/android/issues/detail?id=190529
//                mTrashedPosts.remove(post);
//
//                WordPress.wpDB.deletePost(fullPost);
//
//                if (!post.isLocalDraft()) {
//                    new ApiHelper.DeleteSinglePostTask().execute(WordPress.getCurrentBlog(),
//                            fullPost.getRemotePostId(), mIsPage);
//                }
//            }
//        });
//    }

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
