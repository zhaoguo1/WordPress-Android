package org.wordpress.android.ui.posts.adapters;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.databinding.EndlistIndicatorBinding;
import org.wordpress.android.databinding.PageItemBinding;
import org.wordpress.android.databinding.PostCardviewBinding;
import org.wordpress.android.ui.posts.BasePostViewModel;
import org.wordpress.android.ui.posts.EndlistIndicatorViewModel;
import org.wordpress.android.ui.posts.PageViewModel;
import org.wordpress.android.ui.posts.PostViewModel;
import org.wordpress.android.ui.posts.PostsListContracts.PagesActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.ui.posts.PostsListFragment;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private OnLoadMoreListener mOnLoadMoreListener;
    private PagesActionHandler mPagesActionHandler;
    private PostsActionHandler mPostsActionHandler;

    private final boolean mIsPage;

    private ObservableList<BasePostViewModel> mPostViewModels = new ObservableArrayList<>();

    private static final int VIEW_TYPE_POST_OR_PAGE = 0;
    private static final int VIEW_TYPE_ENDLIST_INDICATOR = 1;

    public PostsListAdapter(boolean isPage, ObservableList<BasePostViewModel> postViewModels,
            PostsActionHandler postsActionHandler, PagesActionHandler pagesActionHandler,
            OnLoadMoreListener onLoadMoreListener) {
        mIsPage = isPage;
        mPostViewModels = postViewModels;
        mPagesActionHandler = pagesActionHandler;
        mPostsActionHandler = postsActionHandler;
        mOnLoadMoreListener = onLoadMoreListener;

        postViewModels.addOnListChangedCallback(new ObservableList
                .OnListChangedCallback<ObservableList<BasePostViewModel>>() {
                    @Override
                    public void onChanged(ObservableList<BasePostViewModel> baseObservables) {
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onItemRangeChanged(ObservableList<BasePostViewModel> basePostViewModels, int
                            positionStart, int itemCount) {
                        notifyItemRangeChanged(positionStart, itemCount);
                    }

                    @Override
                    public void onItemRangeInserted(ObservableList<BasePostViewModel> basePostViewModels, int
                            positionStart, int itemCount) {
                        notifyItemRangeInserted(positionStart, itemCount);
                    }

                    @Override
                    public void onItemRangeMoved(ObservableList<BasePostViewModel> basePostViewModels, int fromPosition,
                            int toPosition, int itemCount) {
                        for (int i = 0; i < itemCount; i++) {
                            notifyItemMoved(fromPosition + i, toPosition + i);
                        }
                    }

                    @Override
                    public void onItemRangeRemoved(ObservableList<BasePostViewModel> basePostViewModels, int
                            positionStart, int itemCount) {
                        if (mPostViewModels.size() > 0) {
                            notifyItemRangeRemoved(positionStart, itemCount);
                        } else {
                            // we must call notifyDataSetChanged when the only post has been deleted - if we
                            // call notifyItemRemoved the recycler will throw an IndexOutOfBoundsException
                            // because removing the last post also removes the end list indicator
                            notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mPostViewModels.size()) {
            return VIEW_TYPE_ENDLIST_INDICATOR;
        }
        return VIEW_TYPE_POST_OR_PAGE;
    }

    @Override
    public int getItemCount() {
        if (mPostViewModels.size() == 0) {
            return 0;
        } else {
            return mPostViewModels.size() + 1; // +1 for the endlist indicator
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ENDLIST_INDICATOR) {
            EndlistIndicatorBinding endlistIndicatorBinding = DataBindingUtil.inflate(LayoutInflater.from(parent
                    .getContext()), R.layout.endlist_indicator, parent, false);
            return new EndListViewHolder(endlistIndicatorBinding);
        } else if (mIsPage) {
            PageItemBinding pageBinding = DataBindingUtil.inflate(LayoutInflater.from(parent
                    .getContext()), R.layout.page_item, parent, false);
            return new PageViewHolder(pageBinding);
        } else {
            PostCardviewBinding postCardviewBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                    R.layout.post_cardview, parent, false);
            return new PostViewHolder(postCardviewBinding);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        // nothing to do if this is the static endlist indicator
        if (getItemViewType(position) == VIEW_TYPE_ENDLIST_INDICATOR) {
            final EndlistIndicatorViewModel endlistIndicatorViewModel = new EndlistIndicatorViewModel(context, mIsPage);

            final EndlistIndicatorBinding binding = ((EndListViewHolder) holder).getBinding();
            binding.setEndlistIndicatorViewModel(endlistIndicatorViewModel);
            binding.executePendingBindings();
            return;
        }

        if (holder instanceof PostViewHolder) {
            final PostCardviewBinding binding = ((PostViewHolder) holder).getBinding();

            final PostViewModel postViewModel = (PostViewModel) mPostViewModels.get(position);

            postViewModel.getBasePostPresenter().init();
            postViewModel.getBasePostPresenter().start();

            binding.setActionHandler(postViewModel.getActionHandler());
            binding.setPostViewModel(postViewModel);
            binding.executePendingBindings();
        } else if (holder instanceof PageViewHolder) {
            final PageItemBinding pageItemBinding = ((PageViewHolder) holder).getBinding();

            final PageViewModel pageViewModel = (PageViewModel) mPostViewModels.get(position);//new PageViewModel(context, position, post, position == 0 ? null : mPosts.get(position - 1));

            pageViewModel.getBasePostPresenter().init();
            pageViewModel.getBasePostPresenter().start();

            pageItemBinding.setActionHandler(pageViewModel.getActionHandler());
            pageItemBinding.setPageViewModel(pageViewModel);
            pageItemBinding.executePendingBindings();
        }

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= mPostViewModels.size() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private PostCardviewBinding binding;

        public PostViewHolder(PostCardviewBinding postCardviewBinding) {
            super(postCardviewBinding.cardView);

            binding = postCardviewBinding;
        }

        public PostCardviewBinding getBinding() {
            return binding;
        }
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        private PageItemBinding binding;

        public PageViewHolder(PageItemBinding pageItemBinding) {
            super(pageItemBinding.getRoot());

            binding = pageItemBinding;
        }

        public PageItemBinding getBinding() {
            return binding;
        }
    }

    class EndListViewHolder extends RecyclerView.ViewHolder {
        private EndlistIndicatorBinding binding;

        public EndListViewHolder(EndlistIndicatorBinding endlistIndicatorBinding) {
            super(endlistIndicatorBinding.endlistIndicator);

            binding = endlistIndicatorBinding;
        }

        EndlistIndicatorBinding getBinding() {
            return binding;
        }
    }

    @BindingAdapter({"isPage", "posts", "postView", "postsActionHandler", "pagesActionHandler", "onLoadMoreListener"})
    public static void bindAdapter(RecyclerView recyclerView, boolean isPage, ObservableList<BasePostViewModel>
            postViewModels, PostView postView, PostsActionHandler postsActionHandler,
            PagesActionHandler pagesActionHandler, PostsListAdapter.OnLoadMoreListener onLoadMoreListener) {
        if (recyclerView.getAdapter() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(new PostsListAdapter(isPage, postViewModels, postsActionHandler,
                    pagesActionHandler, onLoadMoreListener));
        }
    }
}
