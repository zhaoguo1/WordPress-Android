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
import org.wordpress.android.ui.posts.BasePostPresenter;
import org.wordpress.android.ui.posts.EndlistIndicatorViewModel;
import org.wordpress.android.ui.posts.PagePresenter;
import org.wordpress.android.ui.posts.PostPresenter;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.widgets.RecyclerItemDecoration;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private PostsActionHandler mPostsActionHandler;

    private final boolean mIsPage;

    private ObservableList<BasePostPresenter<?>> mPostPresenters = new ObservableArrayList<>();

    private static final int VIEW_TYPE_POST_OR_PAGE = 0;
    private static final int VIEW_TYPE_ENDLIST_INDICATOR = 1;

    public PostsListAdapter(boolean isPage, ObservableList<BasePostPresenter<?>> postPresenters, PostsActionHandler
            postsActionHandler) {
        mIsPage = isPage;
        mPostPresenters = postPresenters;
        mPostsActionHandler = postsActionHandler;

        postPresenters.addOnListChangedCallback(new ObservableList
                .OnListChangedCallback<ObservableList<BasePostPresenter<?>>>() {
                    @Override
                    public void onChanged(ObservableList<BasePostPresenter<?>> baseObservables) {
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onItemRangeChanged(ObservableList<BasePostPresenter<?>> basePostViewModels, int
                            positionStart, int itemCount) {
                        notifyItemRangeChanged(positionStart, itemCount);
                    }

                    @Override
                    public void onItemRangeInserted(ObservableList<BasePostPresenter<?>> basePostViewModels, int
                            positionStart, int itemCount) {
                        notifyItemRangeInserted(positionStart, itemCount);
                    }

                    @Override
                    public void onItemRangeMoved(ObservableList<BasePostPresenter<?>> basePostViewModels, int
                            fromPosition, int toPosition, int itemCount) {
                        for (int i = 0; i < itemCount; i++) {
                            notifyItemMoved(fromPosition + i, toPosition + i);
                        }
                    }

                    @Override
                    public void onItemRangeRemoved(ObservableList<BasePostPresenter<?>> basePostViewModels, int
                            positionStart, int itemCount) {
                        if (mPostPresenters.size() > 0) {
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
        if (position == mPostPresenters.size()) {
            return VIEW_TYPE_ENDLIST_INDICATOR;
        }
        return VIEW_TYPE_POST_OR_PAGE;
    }

    @Override
    public int getItemCount() {
        if (mPostPresenters.size() == 0) {
            return 0;
        } else {
            return mPostPresenters.size() + 1; // +1 for the endlist indicator
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

            final PostPresenter postPresenter = (PostPresenter) mPostPresenters.get(position);

            postPresenter.start();

            binding.setActionHandler(postPresenter);
            binding.setPostViewModel(postPresenter.getViewModel());
            binding.executePendingBindings();
        } else if (holder instanceof PageViewHolder) {
            final PageItemBinding pageItemBinding = ((PageViewHolder) holder).getBinding();

            final PagePresenter pagePresenter = (PagePresenter) mPostPresenters.get(position);

            pagePresenter.start();

            pageItemBinding.setActionHandler(pagePresenter);
            pageItemBinding.setPageViewModel(pagePresenter.getViewModel());
            pageItemBinding.executePendingBindings();
        }

        // load more posts when we near the end
        if (mPostsActionHandler != null && position >= mPostPresenters.size() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mPostsActionHandler.onLoadMore();
        }
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

    @BindingAdapter({"isPage", "spacingHorizontal", "spacingVertical", "posts", "postsActionHandler"})
    public static void bindAdapter(RecyclerView recyclerView, boolean isPage, float spacingHorizontal, float
            spacingVertical, ObservableList<BasePostPresenter<?>> postPresenters, PostsActionHandler
            postsActionHandler) {
        if (recyclerView.getAdapter() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addItemDecoration(new RecyclerItemDecoration(
                    (int) spacingHorizontal, isPage ? 0 : (int) spacingVertical));
            recyclerView.setAdapter(new PostsListAdapter(isPage, postPresenters, postsActionHandler));
        }
    }
}
