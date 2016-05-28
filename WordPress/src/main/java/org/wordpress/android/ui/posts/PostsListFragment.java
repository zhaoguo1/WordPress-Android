package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.databinding.PostListFragmentBinding;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.PostsListContracts.PageView;
import org.wordpress.android.ui.posts.PostsListContracts.PagesActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostsView;
import org.wordpress.android.ui.posts.PostsListContracts.Undoable;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.widgets.RecyclerItemDecoration;

public class PostsListFragment extends Fragment implements
        PostsListAdapter.OnLoadMoreListener,
        PostsView,
        PageView,
        PostView {

    private static final String ARG_LOCAL_BLOG_ID = "ARG_LOCAL_BLOG_ID";
    private static final String ARG_IS_PAGE = "ARG_IS_PAGE";

    PostsPresenter mPostsPresenter;
    PostsActionHandler mPostsActionHandler;
    PagesActionHandler mPagesActionHandler;

    public static final int POSTS_REQUEST_COUNT = 20;

    private PostsListAdapter mPostsListAdapter;

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ProgressBar mProgressLoadMore;
    private TextView mEmptyViewTitle;
    private ImageView mEmptyViewImage;

    private int mLocalBlogId;
    private boolean mIsPage;

    public static PostsListFragment newInstance(int localBlogId, boolean isPage) {
        Bundle args = new Bundle();
        args.putInt(ARG_LOCAL_BLOG_ID, localBlogId);
        args.putBoolean(ARG_IS_PAGE, isPage);
        PostsListFragment fragment = new PostsListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isAdded()) {
            Bundle args = getArguments();
            if (args != null) {
                mIsPage = args.getBoolean(ARG_IS_PAGE);
                mLocalBlogId = args.getInt(ARG_LOCAL_BLOG_ID);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PostListFragmentBinding viewBinding = DataBindingUtil.inflate(inflater, R.layout.post_list_fragment,
                container, false);

        mRecyclerView = viewBinding.recyclerView;
        mProgressLoadMore = viewBinding.progress;

        mEmptyView = viewBinding.emptyView;
        mEmptyViewTitle = viewBinding.titleEmpty;
        mEmptyViewImage = viewBinding.imageEmpty;

        Context context = getActivity();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        int spacingVertical = mIsPage ? 0 : context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        PostsListViewModel postsListViewModel = new PostsListViewModel(context, viewBinding);
        mPostsPresenter = new PostsPresenter(mLocalBlogId, postsListViewModel, this, mIsPage);
        mPostsActionHandler = mPostsPresenter;
        mPagesActionHandler = mPostsPresenter;

        postsListViewModel.setSwipeToRefreshHelper(new SwipeToRefreshHelper(
                inflater.getContext(),
                viewBinding.ptrLayout,
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        mPostsActionHandler.onRefreshRequested();
                    }
                }));

        viewBinding.setViewModel(postsListViewModel);
        viewBinding.setActionHandler(mPostsActionHandler);
        viewBinding.executePendingBindings();

        return viewBinding.getRoot();
    }

    @Override
    public void editBlogPostOrPageForResult(long postOrPageId, boolean isPage) {
        if (isAdded()) {
            ActivityLauncher.editBlogPostOrPageForResult(getActivity(), postOrPageId, isPage);
        }
    }

    @Override
    public void publishPost(Post post) {
        if (isAdded()) {
            final Activity activity = getActivity();
            PostUploadService.addPostToUpload(post);
            activity.startService(new Intent(activity, PostUploadService.class));
        }
    }

    @Override
    public void browsePostOrPage(Blog blog, Post post) {
        if (isAdded()) {
            ActivityLauncher.browsePostOrPage(getActivity(), blog, post);
        }
    }

    @Override
    public void viewPostPreviewForResult(Post post, boolean isPage) {
        if (isAdded()) {
            ActivityLauncher.viewPostPreviewForResult(getActivity(), post, isPage);
        }
    }

    @Override
    public void viewStatsSinglePostDetails(Post post, boolean isPost) {
        if (isAdded()) {
            ActivityLauncher.viewStatsSinglePostDetails(getActivity(), post, isPost);
        }
    }

    @Override
    public void hidePost(PostsListPost postsListPost) {
        getPostListAdapter().hidePost(postsListPost);
    }

    @Override
    public void showToast(@StringRes int stringResId) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), stringResId);
        }
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            mPostsListAdapter = new PostsListAdapter(getActivity(), WordPress.getCurrentBlog(), mIsPage, this, this,
                    mPostsActionHandler, mPagesActionHandler);
            mPostsListAdapter.setOnLoadMoreListener(this);
        }

        return mPostsListAdapter;
    }

    private boolean isPostAdapterEmpty() {
        return (mPostsListAdapter != null && mPostsListAdapter.getItemCount() == 0);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // since setRetainInstance(true) is used, we only need to request latest
        // posts the first time this is called (ie: not after device rotation)
        if (bundle == null && NetworkUtils.checkConnection(getActivity())) {
            mPostsPresenter.init();
        }
    }

    @Override
    public void newPost() {
        if (!isAdded()) return;

        if (WordPress.getCurrentBlog() != null) {
            ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getCurrentBlog(), mIsPage);
        } else {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found);
        }
    }

    public void onResume() {
        super.onResume();

        if (WordPress.getCurrentBlog() != null && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostListAdapter());
        }
    }

    @Override
    public void showLoadMoreProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoadMoreProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.GONE);
        }
    }

    public void mediaUpdated(long mediaId, String mediaUrl) {
        if (isAdded()) {
            getPostListAdapter().mediaUpdated(mediaId, mediaUrl);
        }
    }

    @Override
    public void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (!isAdded() || mEmptyView == null) {
            return;
        }

        int stringId;
        switch (emptyViewMessageType) {
            case LOADING:
                stringId = mIsPage ? R.string.pages_fetching : R.string.posts_fetching;
                break;
            case NO_CONTENT:
                stringId = mIsPage ? R.string.pages_empty_list : R.string.posts_empty_list;
                break;
            case NETWORK_ERROR:
                stringId = R.string.no_network_message;
                break;
            case PERMISSION_ERROR:
                stringId = mIsPage ? R.string.error_refresh_unauthorized_pages :
                        R.string.error_refresh_unauthorized_posts;
                break;
            case GENERIC_ERROR:
                stringId = mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts;
                break;
            default:
                return;
        }

        mEmptyViewTitle.setText(getText(stringId));
        mEmptyViewImage.setVisibility(emptyViewMessageType == EmptyViewMessageType.NO_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(isPostAdapterEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPosts(PostsListPostList posts, boolean isFetchingPosts) {
        if (!isAdded()) {
            return;
        }

        getPostListAdapter().setPostsList(posts);
    }

    @Override
    public void onStart() {
        super.onStart();
        mPostsPresenter.start();
    }

    @Override
    public void onStop() {
        mPostsPresenter.stop();
        super.onStop();
    }

    /*
     * called by the adapter to load more posts when the user scrolls towards the last post
     */
    @Override
    public void onLoadMore() {
        mPostsActionHandler.onLoadMore();
    }

    @Override
    public void withUndo(final Undoable undoable) {
        if (!isAdded()) {
            return;
        }

        Snackbar.make(getView().findViewById(R.id.coordinator), undoable.getText(), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undoable.onUndo();
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        // wait for the undo snackbar to disappear before actually deleting the post
                        super.onDismissed(snackbar, event);

                        undoable.onDismiss();
                    }
                })
                .show();
    }
}
