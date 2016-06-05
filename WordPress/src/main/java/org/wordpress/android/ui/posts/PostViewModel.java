package org.wordpress.android.ui.posts;

import org.wordpress.android.BR;
import org.wordpress.android.databinding.PostCardviewBinding;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.databinding.Bindable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PostView}.
 */
public class PostViewModel extends BasePostViewModel {
    private static final long ROW_ANIM_DURATION = 150;

    private PostCardviewBinding mPostCardviewBinding;
    private final boolean mIsStatsSupported;
    private boolean mAlwaysShowAllButtons;

    public PostViewModel(PostsListPost postsListPost, boolean isStatsSupported) {
        super(postsListPost);

        mIsStatsSupported = isStatsSupported;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);

        // on larger displays we can always show all buttons
        mAlwaysShowAllButtons = (displayWidth >= 1080);
    }

    public void setPostCardviewBinding(PostCardviewBinding postCardviewBinding) {
        mPostCardviewBinding = postCardviewBinding;
    }

    public String getExcerpt() {
        if (mPostsListPost.hasExcerpt()) {
            return PostUtils.collapseShortcodes(mPostsListPost.getExcerpt());
        } else {
            return StringUtils.notNullStr(mPostsListPost.getExcerpt());
        }
    }

    public int getExcerptVisibility() {
        return mPostsListPost.hasExcerpt() ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public String getFeaturedImageUrl() {
        if (mPostsListPost.hasFeaturedImageId() || mPostsListPost.hasFeaturedImageUrl()) {
            return mPostsListPost.getFeaturedImageUrl();
        } else {
            return null;
        }
    }

    public void setFeaturedImageUrl(String imageUrl) {
        mPostsListPost.setFeaturedImageUrl(imageUrl);
        notifyPropertyChanged(BR.featuredImageUrl);
    }

    public WPNetworkImageView.ImageType getFeaturedImageType() {
        return WPNetworkImageView.ImageType.PHOTO;
    }

    public int getFeaturedImageVisibility() {
        return (mPostsListPost.hasFeaturedImageId() || mPostsListPost.hasFeaturedImageUrl()) ? View.VISIBLE : View.GONE;
    }

    public int getDateVisibility() {
        return mPostsListPost.isLocalDraft() ? View.GONE : View.VISIBLE;
    }

    public String getFormattedDate() {
        return mPostsListPost.getFormattedDate();
    }

    public int getTrashButtonType() {
        // local drafts say "delete" instead of "trash"
        return mPostsListPost.isLocalDraft() ? PostListButton.BUTTON_DELETE : PostListButton.BUTTON_TRASH;
    }

    public int getViewButtonType() {
        // posts with local changes have preview rather than view button
        return (mPostsListPost.isLocalDraft() || mPostsListPost.hasLocalChanges()) ? PostListButton.BUTTON_PREVIEW :
                PostListButton.BUTTON_VIEW;
    }

    public int getViewButtonVisibility() {
        // edit / view are always visible
        return View.VISIBLE;
    }

    public int getEditButtonVisibility() {
        // edit / view are always visible
        return View.VISIBLE;
    }

    // if we have enough room to show all buttons, hide the back/more buttons and show stats/trash

    public int getMoreButtonVisibility() {
        return hasEnoughRoom() ? View.GONE : View.VISIBLE;
    }

    public int getBackButtonVisibility() {
        return View.GONE;
    }

    public int getTrashButtonVisibility() {
        return hasEnoughRoom() ? View.VISIBLE : View.GONE;
    }

    public int getStatsButtonVisibility() {
        return (canShowStatsForPost() && hasEnoughRoom()) ? View.VISIBLE : View.GONE;
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

    /*
     * buttons may appear in two rows depending on display size and number of visible
     * buttons - these rows are toggled through the "more" and "back" buttons - this
     * routine is used to animate the new row in and the old row out
     */
    public void animateButtonRows(final boolean showRow1) {
        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f);
        ObjectAnimator animOut = ObjectAnimator.ofPropertyValuesHolder(mPostCardviewBinding.layoutButtons, scaleX,
                scaleY);
        animOut.setDuration(ROW_ANIM_DURATION);
        animOut.setInterpolator(new AccelerateInterpolator());

        animOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // row 1
                mPostCardviewBinding.btnEdit.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                mPostCardviewBinding.btnView.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                mPostCardviewBinding.btnMore.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                // row 2
                mPostCardviewBinding.btnStats.setVisibility(!showRow1 && canShowStatsForPost() ? View.VISIBLE : View
                        .GONE);
                mPostCardviewBinding.btnTrash.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);
                mPostCardviewBinding.btnBack.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);

                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f);
                ObjectAnimator animIn = ObjectAnimator.ofPropertyValuesHolder(mPostCardviewBinding.layoutButtons,
                        scaleX, scaleY);
                animIn.setDuration(ROW_ANIM_DURATION);
                animIn.setInterpolator(new DecelerateInterpolator());
                animIn.start();
            }
        });

        animOut.start();
    }

}
