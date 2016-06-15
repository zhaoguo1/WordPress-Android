package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.databinding.PostListFragmentBinding;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsView;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.ToastUtils;

public class PostsListFragment extends Fragment implements
        PostsView,
        PostView {

    private static final String ARG_LOCAL_BLOG_ID = "ARG_LOCAL_BLOG_ID";
    private static final String ARG_IS_PAGE = "ARG_IS_PAGE";

    public static final int POSTS_REQUEST_COUNT = 20;

    private PostsPresenter mPostsPresenter;
    private boolean isFirstStart;

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

        isFirstStart = (savedInstanceState == null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PostListFragmentBinding viewBinding = DataBindingUtil.inflate(inflater, R.layout.post_list_fragment,
                container, false);

        Bundle args = getArguments();
        boolean isPage = args.getBoolean(ARG_IS_PAGE);
        int localBlogId = args.getInt(ARG_LOCAL_BLOG_ID);

        final Blog blog = WordPress.getBlog(localBlogId);
        boolean isStatsSupported = blog.isDotcomFlag() || blog.isJetpackPowered();

        mPostsPresenter = new PostsPresenter(localBlogId, this, this, isPage, isStatsSupported);

        viewBinding.setViewModel(mPostsPresenter.getPostsViewModel());
        viewBinding.setPostsActionHandler(mPostsPresenter);
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
    public void showToast(@StringRes int stringResId) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), stringResId);
        }
    }

    @Override
    public void newPost(boolean isPage) {
        if (!isAdded()) return;

        if (WordPress.getCurrentBlog() != null) {
            ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getCurrentBlog(), isPage);
        } else {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isFirstStart) {
            isFirstStart = false;

            mPostsPresenter.willBeFirstStart();
        }

        mPostsPresenter.start();
    }

    @Override
    public void onStop() {
        mPostsPresenter.stop();
        super.onStop();
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
