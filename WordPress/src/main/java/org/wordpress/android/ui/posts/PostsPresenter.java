package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostMediaService;
import org.wordpress.android.ui.posts.services.PostUpdateService;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.xmlrpc.android.ApiHelper;

import android.os.AsyncTask;
import android.text.TextUtils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class PostsPresenter implements BasePresenter, PostsListContracts.PostsActionHandler {

    private final PostsListContracts.PostsView mPostsView;
    private final int mLocalTableBlogId;
    private final boolean mIsPrivateBlog;
    private final boolean mIsPage;

    private final int mPhotonWidth;
    private final int mPhotonHeight;

    private final PostsListPostList mPosts = new PostsListPostList();
    private final PostsListPostList mTrashedPosts = new PostsListPostList();

    private boolean mIsLoadingPosts;
    private boolean mIsFetchingPosts;

    private boolean mCanLoadMorePosts = true;

    public PostsPresenter(int blogLocalId, PostsListContracts.PostsView postsView, boolean isPage) {
        mPostsView = postsView;
        mIsPage = isPage;

        mLocalTableBlogId = blogLocalId;

        final Blog blog = WordPress.getBlog(blogLocalId);
        mIsPrivateBlog = blog.isPrivate();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mPostsView.getContext());
        int contentSpacing = mPostsView.getContext().getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (contentSpacing * 2);
        mPhotonHeight = mPostsView.getContext().getResources().getDimensionPixelSize(R.dimen
                .reader_featured_image_height);

        mPostsView.setPresenter(this);
        mPostsView.setPostsActionHandler(this);
    }

    @Override
    public void onFabClick() {
        mPostsView.newPost();
    }

    @Override
    public void init() {
        requestPosts(false);
    }

    @Override
    public void start() {
        EventBus.getDefault().register(this);

        if (WordPress.getCurrentBlog() != null) {
            loadPosts();
        }
    }

    @Override
    public void stop() {
        EventBus.getDefault().unregister(this);
    }

    /*
     * PostMediaService has downloaded the media info for a post's featured image, tell
     * the adapter so it can show the featured image now that we have its URL
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaInfoUpdated event) {
        if (WordPress.getCurrentBlog() != null) {
            mPostsView.mediaUpdated(event.getMediaId(), event.getMediaUrl());
        }
    }

    /*
     * upload start, reload so correct status on uploading post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (WordPress.getCurrentLocalTableBlogId() == event.mLocalBlogId) {
            loadPosts();
        }
    }

    /*
     * upload ended, reload regardless of success/fail so correct status of uploaded post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadEnded event) {
        if (WordPress.getCurrentLocalTableBlogId() == event.mLocalBlogId) {
            loadPosts();
        }
    }

    /*
     * PostUpdateService finished a request to retrieve new posts
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.RequestPosts event) {
        mIsFetchingPosts = false;
        if (event.getBlogId() == WordPress.getCurrentLocalTableBlogId()) {
            mPostsView.setRefreshing(false);
            mPostsView.hideLoadMoreProgress();
            if (!event.getFailed()) {
                mCanLoadMorePosts = event.canLoadMore();
                loadPosts();
            } else {
                ApiHelper.ErrorType errorType = event.getErrorType();
                if (errorType != null && errorType != ApiHelper.ErrorType.TASK_CANCELLED && errorType != ApiHelper
                        .ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            mPostsView.updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                            break;
                        default:
                            mPostsView.updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                            break;
                    }
                }
            }
        }
    }

    private void loadPosts() {
        if (mIsLoadingPosts) {
            AppLog.d(AppLog.T.POSTS, "post adapter > already loading posts");
        } else {
            new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onLoadMore() {
        if (mCanLoadMorePosts && !mIsFetchingPosts) {
            requestPosts(true);
        }
    }

    @Override
    public void requestPosts(boolean loadMore) {
        if (mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(mPostsView.getContext())) {
            mPostsView.updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        mIsFetchingPosts = true;
        if (loadMore) {
            mPostsView.showLoadMoreProgress();
        } else {
            mPostsView.setRefreshing(true);
        }
        PostUpdateService.startServiceForBlog(mPostsView.getContext(), WordPress.getCurrentLocalTableBlogId(),
                mIsPage, loadMore);
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        private PostsListPostList tmpPosts;
        private final ArrayList<Long> mediaIdsToUpdate = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsLoadingPosts = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsLoadingPosts = false;
        }

        @Override
        protected Boolean doInBackground(Void... nada) {
            tmpPosts = WordPress.wpDB.getPostsListPosts(mLocalTableBlogId, mIsPage);

            // make sure we don't return any hidden posts
            for (PostsListPost hiddenPost : mTrashedPosts) {
                tmpPosts.remove(hiddenPost);
            }

            // go no further if existing post list is the same
            if (mPosts.isSameList(tmpPosts)) {
                return false;
            }

            // generate the featured image url for each post
            String imageUrl;
            for (PostsListPost post : tmpPosts) {
                if (post.isLocalDraft()) {
                    imageUrl = null;
                } else if (post.getFeaturedImageId() != 0) {
                    imageUrl = WordPress.wpDB.getMediaThumbnailUrl(mLocalTableBlogId, post.getFeaturedImageId());
                    // if the imageUrl isn't found it means the featured image info hasn't been added to
                    // the local media library yet, so add to the list of media IDs to request info for
                    if (TextUtils.isEmpty(imageUrl)) {
                        mediaIdsToUpdate.add(post.getFeaturedImageId());
                    }
                } else if (post.hasDescription()) {
                    ReaderImageScanner scanner = new ReaderImageScanner(post.getDescription(), mIsPrivateBlog);
                    imageUrl = scanner.getLargestImage();
                } else {
                    imageUrl = null;
                }

                if (!TextUtils.isEmpty(imageUrl)) {
                    post.setFeaturedImageUrl(
                            ReaderUtils.getResizedImageUrl(
                                    imageUrl,
                                    mPhotonWidth,
                                    mPhotonHeight,
                                    mIsPrivateBlog));
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts.clear();
                mPosts.addAll(tmpPosts);
                mPostsView.setPosts(mPosts, mIsFetchingPosts);

                if (mediaIdsToUpdate.size() > 0) {
                    PostMediaService.startService(WordPress.getContext(), mLocalTableBlogId, mediaIdsToUpdate);
                }
            }

            mIsLoadingPosts = false;
        }
    }
}
