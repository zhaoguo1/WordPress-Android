package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.PostsListContracts.PagesActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostsView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsViewModel;
import org.wordpress.android.ui.posts.PostsListContracts.Undoable;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostMediaService;
import org.wordpress.android.ui.posts.services.PostUpdateService;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.xmlrpc.android.ApiHelper;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class PostsPresenter implements BasePresenter, PostsActionHandler, PagesActionHandler {

    private final PostsViewModel mPostsViewModel;
    private final PostsView mPostsView;
    private final int mLocalTableBlogId;
    private final boolean mIsPrivateBlog;
    private final boolean mIsPage;
    private final boolean mIsStatsSupported;

    private final int mPhotonWidth;
    private final int mPhotonHeight;

    private final Map<String, BasePostViewModel> mPosts = new LinkedHashMap<>();
    private final List<PostsListPost> mTrashedPosts = new ArrayList<>();

    private final ObservableList<BasePostViewModel> mPostViewModels = new ObservableArrayList<>();

    private boolean mIsLoadingPosts;
    private boolean mIsFetchingPosts;

    private boolean mCanLoadMorePosts = true;

    public PostsPresenter(int blogLocalId, PostsViewModel postsViewModel, PostsView postsView, boolean isPage,
            boolean isStatsSupported) {
        mPostsViewModel = postsViewModel;
        mPostsView = postsView;
        mIsPage = isPage;
        mIsStatsSupported = isStatsSupported;

        mLocalTableBlogId = blogLocalId;

        final Blog blog = WordPress.getBlog(blogLocalId);
        mIsPrivateBlog = blog.isPrivate();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(mPostsView.getContext());
        int contentSpacing = mPostsView.getContext().getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (contentSpacing * 2);
        mPhotonHeight = mPostsView.getContext().getResources().getDimensionPixelSize(R.dimen
                .reader_featured_image_height);

        postsViewModel.setPosts(mPostViewModels);
    }

    @Override
    public void onFabClick() {
        mPostsView.newPost();
    }

    @Override
    public void init() {
        mPostsViewModel.setEmptyViewTitle(mPostsView.getText(R.string.empty_list_default));

        requestPosts(false);
    }

    @Override
    public void start() {
        EventBus.getDefault().register(this);

        if (WordPress.getCurrentBlog() != null) {
            loadPosts();
        }

        mPostsViewModel.slideFabInIfHidden();
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
        BasePostViewModel basePostViewModel = indexOfFeaturedMediaId(event.getMediaId());

        if (basePostViewModel != null && basePostViewModel instanceof PostViewModel) {
            ((PostViewModel) basePostViewModel).setFeaturedImageUrl(event.getMediaUrl());
        }
    }

    private BasePostViewModel indexOfFeaturedMediaId(long mediaId) {
        if (mediaId == 0) {
            return null;
        }
        for (BasePostViewModel basePostViewModel : mPostViewModels) {
            if (basePostViewModel.getPostsListPost().getFeaturedImageId() == mediaId) {
                return basePostViewModel;
            }
        }
        return null;
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
            mPostsViewModel.setIsRefreshing(false);
            mPostsViewModel.setLoadMoreProgressVisibility(false);
            if (!event.getFailed()) {
                mCanLoadMorePosts = event.canLoadMore();
                loadPosts();
            } else {
                ApiHelper.ErrorType errorType = event.getErrorType();
                if (errorType != null && errorType != ApiHelper.ErrorType.TASK_CANCELLED && errorType != ApiHelper
                        .ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            updateEmptyView(mIsPage, EmptyViewMessageType.PERMISSION_ERROR,
                                    isPostListEmpty());
                            break;
                        default:
                            updateEmptyView(mIsPage, EmptyViewMessageType.GENERIC_ERROR,
                                    isPostListEmpty());
                            break;
                    }
                }
            }
        }
    }

    private boolean isPostListEmpty() {
        return mPosts != null && mPosts.size() == 0;
    }

    private void loadPosts() {
        if (mIsLoadingPosts) {
            AppLog.d(AppLog.T.POSTS, "post adapter > already loading posts");
        } else {
            new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onRefreshRequested() {
        if (!NetworkUtils.checkConnection(mPostsView.getContext())) {
            mPostsViewModel.setIsRefreshing(false);
            updateEmptyView(mIsPage, EmptyViewMessageType.NETWORK_ERROR, isPostListEmpty());
            return;
        }
        requestPosts(false);
    }

    @Override
    public void onLoadMore() {
        if (mCanLoadMorePosts && !mIsFetchingPosts) {
            requestPosts(true);
        }
    }

    private void requestPosts(boolean loadMore) {
        if (mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(mPostsView.getContext())) {
            updateEmptyView(mIsPage, EmptyViewMessageType.NETWORK_ERROR, isPostListEmpty());
            return;
        }

        mIsFetchingPosts = true;
        if (loadMore) {
            mPostsViewModel.setLoadMoreProgressVisibility(true);
        } else {
            mPostsViewModel.setIsRefreshing(true);
        }
        PostUpdateService.startServiceForBlog(mPostsView.getContext(), WordPress.getCurrentLocalTableBlogId(),
                mIsPage, loadMore);
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        private List<PostsListPost> tmpPosts;
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
            if (isSameList(tmpPosts)) {
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
                Map<String, BasePostViewModel> posts = new LinkedHashMap<>();
                ObservableList<BasePostViewModel> tmpPostViewmodels = new ObservableArrayList<>();
                for (PostsListPost post : tmpPosts) {
                    BasePostViewModel existingPostViewModel = mPosts.get(post.getRemotePostId());
                    if (existingPostViewModel == null) {
                        existingPostViewModel = new PostViewModel(post, mIsStatsSupported);
                    }

                    posts.put(post.getRemotePostId(), existingPostViewModel);
                    tmpPostViewmodels.add(existingPostViewModel);
                }

                mPosts.clear();
                mPosts.putAll(posts);

                mPostViewModels.retainAll(tmpPostViewmodels);

                int i = 0;
                for (BasePostViewModel basePostViewModel : tmpPostViewmodels) {
                    if (!mPostViewModels.contains(basePostViewModel)) {
                        mPostViewModels.add(i, basePostViewModel);
                    }

                    i++;
                }

                if (mediaIdsToUpdate.size() > 0) {
                    PostMediaService.startService(WordPress.getContext(), mLocalTableBlogId, mediaIdsToUpdate);
                }
            }

            mIsLoadingPosts = false;

            updateEmptyView();
        }
    }

    private boolean isSameList(List<PostsListPost> list2) {
        if (list2 == null || mPosts.size() != list2.size()) {
            return false;
        }

        int i = 0;
        for (Map.Entry<String, BasePostViewModel> entry : mPosts.entrySet()) {
            PostsListPost newPost = list2.get(i++);
            PostsListPost currentPost = entry.getValue().getPostsListPost();

            if (newPost.getPostId() != currentPost.getPostId())
                return false;
            if (!newPost.getTitle().equals(currentPost.getTitle()))
                return false;
            if (newPost.getDateCreatedGmt() != currentPost.getDateCreatedGmt())
                return false;
            if (!newPost.getOriginalStatus().equals(currentPost.getOriginalStatus()))
                return false;
            if (newPost.isUploading() != currentPost.isUploading())
                return false;
            if (newPost.isLocalDraft() != currentPost.isLocalDraft())
                return false;
            if (newPost.hasLocalChanges() != currentPost.hasLocalChanges())
                return false;
            if (!newPost.getDescription().equals(currentPost.getDescription()))
                return false;
        }

        return true;
    }

    private void hidePost(PostsListPost postsListPost) {
        mPostViewModels.remove(mPosts.get(postsListPost.getRemotePostId()));

        updateEmptyView();
    }

    private void updateEmptyView() {
        if (mPosts.size() == 0 && !mIsFetchingPosts) {
            if (NetworkUtils.isNetworkAvailable(mPostsView.getContext())) {
                updateEmptyView(mIsPage, EmptyViewMessageType.NO_CONTENT, isPostListEmpty());
            } else {
                updateEmptyView(mIsPage, EmptyViewMessageType.NETWORK_ERROR, isPostListEmpty());
            }
        } else if (mPosts.size() > 0) {
            mPostsViewModel.setEmptyViewVisibility(false);
        }
    }

    private void updateEmptyView(boolean isPage, EmptyViewMessageType emptyViewMessageType, boolean isEmpty) {
        int stringId;
        switch (emptyViewMessageType) {
            case LOADING:
                stringId = isPage ? R.string.pages_fetching : R.string.posts_fetching;
                break;
            case NO_CONTENT:
                stringId = isPage ? R.string.pages_empty_list : R.string.posts_empty_list;
                break;
            case NETWORK_ERROR:
                stringId = R.string.no_network_message;
                break;
            case PERMISSION_ERROR:
                stringId = isPage ? R.string.error_refresh_unauthorized_pages :
                        R.string.error_refresh_unauthorized_posts;
                break;
            case GENERIC_ERROR:
                stringId = isPage ? R.string.error_refresh_pages : R.string.error_refresh_posts;
                break;
            default:
                return;
        }

        mPostsViewModel.setEmptyViewTitle(mPostsView.getText(stringId));
        mPostsViewModel.setEmptyViewImageVisibility(emptyViewMessageType == EmptyViewMessageType.NO_CONTENT);
        mPostsViewModel.setEmptyViewVisibility(isEmpty);
    }

    /*
     * send the passed post to the trash with undo
     */
    @Override
    public void onTrashPost(final PostsListPost post) {
        //only check if network is available in case this is not a local draft - local drafts have not yet
        //been posted to the server so they can be trashed w/o further care
        if (!post.isLocalDraft() && !NetworkUtils.checkConnection(mPostsView.getContext())) {
            return;
        }

        final Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(post.getPostId());
        if (fullPost == null) {
            mPostsView.showToast(R.string.post_not_found);
            return;
        }

        // remove post from the list and add it to the list of trashed posts
        hidePost(post);
        mPosts.remove(post);
        mTrashedPosts.add(post);

        mPostsView.withUndo(new Undoable() {
            @Override
            public String getText() {
                if (post.isLocalDraft()) {
                    return mPostsView.getString(R.string.post_deleted);
                } else {
                    return mPostsView.getString(R.string.post_trashed);
                }
            }

            @Override
            public void onUndo() {
                // user undid the trash, so unhide the post and remove it from the list of trashed posts
                mTrashedPosts.remove(post);
                loadPosts();
            }

            @Override
            public void onDismiss() {
                // if the post no longer exists in the list of trashed posts it's because the
                // user undid the trash, so don't perform the deletion
                if (!mTrashedPosts.contains(post)) {
                    return;
                }

                // remove from the list of trashed posts in case onDismissed is called multiple
                // times - this way the above check prevents us making the call to delete it twice
                // https://code.google.com/p/android/issues/detail?id=190529
                mTrashedPosts.remove(post);

                WordPress.wpDB.deletePost(fullPost);

                if (!post.isLocalDraft()) {
                    new ApiHelper.DeleteSinglePostTask().execute(WordPress.getCurrentBlog(),
                            fullPost.getRemotePostId(), false);
                }
            }
        });
    }
}
