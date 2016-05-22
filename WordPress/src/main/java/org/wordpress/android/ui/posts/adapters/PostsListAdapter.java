package org.wordpress.android.ui.posts.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;

import org.wordpress.android.R;
import org.wordpress.android.databinding.EndlistIndicatorBinding;
import org.wordpress.android.databinding.PageItemBinding;
import org.wordpress.android.databinding.PostCardviewBinding;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.posts.EndlistIndicatorViewModel;
import org.wordpress.android.ui.posts.PagePresenter;
import org.wordpress.android.ui.posts.PageViewModel;
import org.wordpress.android.ui.posts.PostPresenter;
import org.wordpress.android.ui.posts.PostViewModel;
import org.wordpress.android.ui.posts.PostsListContracts.PagesActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PageActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PageAdapterView;
import org.wordpress.android.ui.posts.PostsListContracts.PageView;
import org.wordpress.android.ui.posts.PostsListContracts.PostActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostAdapterView;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.util.DisplayUtils;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private PostView mPostView;
    private PageView mPageView;
    private OnLoadMoreListener mOnLoadMoreListener;
    private PagesActionHandler mPagesActionHandler;
    private PostsActionHandler mPostsActionHandler;

    private final int mLocalTableBlogId;
    private final int mPhotonWidth;
    private final int mPhotonHeight;

    private final boolean mIsPage;
    private final boolean mIsPrivateBlog;
    private final boolean mIsStatsSupported;
    private final boolean mAlwaysShowAllButtons;

    private final LayoutInflater mLayoutInflater;

    private PostsListPostList mPosts = new PostsListPostList();

    private static final long ROW_ANIM_DURATION = 150;

    private static final int VIEW_TYPE_POST_OR_PAGE = 0;
    private static final int VIEW_TYPE_ENDLIST_INDICATOR = 1;

    public PostsListAdapter(Context context, @NonNull Blog blog, boolean isPage, PostView postView, PageView
            pageView, PostsActionHandler postsActionHandler, PagesActionHandler pagesActionHandler) {
        mPostView = postView;
        mPageView = pageView;
        mPagesActionHandler = pagesActionHandler;
        mPostsActionHandler = postsActionHandler;
        mIsPage = isPage;
        mLayoutInflater = LayoutInflater.from(context);

        mLocalTableBlogId = blog.getLocalTableBlogId();
        mIsPrivateBlog = blog.isPrivate();
        mIsStatsSupported = blog.isDotcomFlag() || blog.isJetpackPowered();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int contentSpacing = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (contentSpacing * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        // on larger displays we can always show all buttons
        mAlwaysShowAllButtons = (displayWidth >= 1080);
    }

    public void setPostsList(PostsListPostList posts) {
        mPosts = posts;
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    private PostsListPost getItem(int position) {
        if (isValidPostPosition(position)) {
            return mPosts.get(position);
        }
        return null;
    }

    private boolean isValidPostPosition(int position) {
        return (position >= 0 && position < mPosts.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mPosts.size()) {
            return VIEW_TYPE_ENDLIST_INDICATOR;
        }
        return VIEW_TYPE_POST_OR_PAGE;
    }

    @Override
    public int getItemCount() {
        if (mPosts.size() == 0) {
            return 0;
        } else {
            return mPosts.size() + 1; // +1 for the endlist indicator
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

        final PostsListPost post = mPosts.get(position);

        if (holder instanceof PostViewHolder) {
            final PostCardviewBinding binding = ((PostViewHolder) holder).getBinding();

            final PostViewModel postViewModel = new PostViewModel(context, mIsStatsSupported, post);

            PostActionHandler postActionHandler = new PostPresenter(
                    mPostView,
                    new PostAdapterView() {
                        @Override
                        public void animateButtonRows(boolean showRow1) {
                            PostsListAdapter.this.animateButtonRows(binding, showRow1, postViewModel
                                    .canShowStatsForPost(post));
                        }
                    },
                    post,
                    mPostsActionHandler);

            binding.setActionHandler(postActionHandler);
            binding.setPostViewModel(postViewModel);
            binding.executePendingBindings();
        } else if (holder instanceof PageViewHolder) {
            final PageItemBinding pageItemBinding = ((PageViewHolder) holder).getBinding();

            final PageViewModel pageViewModel = new PageViewModel(context, position, post, position == 0 ? null :
                    mPosts.get(position - 1));

            final PagePresenter pagePresenter = new PagePresenter(mPageView, post, mPagesActionHandler);
            pagePresenter.setPageAdapterView(new PageAdapterView() {
                    @Override
                    public void showPagePopupMenu(View view) {
                        PostsListAdapter.this.showPagePopupMenu(view, post, pagePresenter);
                    }
            });
            pageItemBinding.setActionHandler(pagePresenter);
            pageItemBinding.setPageViewModel(pageViewModel);
            pageItemBinding.executePendingBindings();
        }

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= mPosts.size() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    private void showPagePopupMenu(View view, final PostsListPost page, final PageActionHandler pageActionHandler) {
        Context context = view.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(view);

        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setModal(true);
        listPopup.setAdapter(new PageMenuAdapter(context, page));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopup.dismiss();
                if (pageActionHandler != null) {
                    pageActionHandler.onPageButtonClick((int) id);
                }
            }
        });
        listPopup.show();
    }

    /*
     * buttons may appear in two rows depending on display size and number of visible
     * buttons - these rows are toggled through the "more" and "back" buttons - this
     * routine is used to animate the new row in and the old row out
     */
    private void animateButtonRows(final PostCardviewBinding binding,
                                   final boolean showRow1,
                                   final boolean canShowStats) {
        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f);
        ObjectAnimator animOut = ObjectAnimator.ofPropertyValuesHolder(binding.layoutButtons, scaleX, scaleY);
        animOut.setDuration(ROW_ANIM_DURATION);
        animOut.setInterpolator(new AccelerateInterpolator());

        animOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // row 1
                binding.btnEdit.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                binding.btnView.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                binding.btnMore.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                // row 2
                binding.btnStats.setVisibility(!showRow1 && canShowStats ? View.VISIBLE : View.GONE);
                binding.btnTrash.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);
                binding.btnBack.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);

                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f);
                ObjectAnimator animIn = ObjectAnimator.ofPropertyValuesHolder(binding.layoutButtons, scaleX, scaleY);
                animIn.setDuration(ROW_ANIM_DURATION);
                animIn.setInterpolator(new DecelerateInterpolator());
                animIn.start();
            }
        });

        animOut.start();
    }

    /*
     * hides the post - used when the post is trashed by the user but the network request
     * to delete the post hasn't completed yet
     */
    public void hidePost(PostsListPost post) {
        int position = mPosts.indexOfPost(post);
        if (position > -1) {
            mPosts.remove(position);
            if (mPosts.size() > 0) {
                notifyItemRemoved(position);
            } else {
                // we must call notifyDataSetChanged when the only post has been deleted - if we
                // call notifyItemRemoved the recycler will throw an IndexOutOfBoundsException
                // because removing the last post also removes the end list indicator
                notifyDataSetChanged();
            }
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

    /*
     * called after the media (featured image) for a post has been downloaded - locate the post
     * and set its featured image url to the passed url
     */
    public void mediaUpdated(long mediaId, String mediaUrl) {
        int position = mPosts.indexOfFeaturedMediaId(mediaId);
        if (isValidPostPosition(position)) {
            mPosts.get(position).setFeaturedImageUrl(mediaUrl);
            notifyItemChanged(position);
        }
    }
}
