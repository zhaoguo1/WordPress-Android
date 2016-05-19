package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.content.Context;
import android.databinding.BaseObservable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.view.View;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PostView}.
 */
public class PostViewModel extends BaseObservable {
    private final Context mContext;

    private PostsListPost mPostsListPost;

    private final boolean mIsStatsSupported;
    private final boolean mAlwaysShowAllButtons;

    public PostViewModel(Context context, boolean isStatsSupported, PostsListPost postsListPost) {
        mContext = context;
        mIsStatsSupported = isStatsSupported;
        mPostsListPost = postsListPost;

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);

        // on larger displays we can always show all buttons
        mAlwaysShowAllButtons = (displayWidth >= 1080);
    }

    public String getTitle() {
        if (mPostsListPost.hasTitle()) {
            return mPostsListPost.getTitle();
        } else {
            return "(" + mContext.getResources().getText(R.string.untitled) + ")";
        }
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

    public String getFeaturedImageUrl() {
        if (mPostsListPost.hasFeaturedImageId() || mPostsListPost.hasFeaturedImageUrl()) {
            return mPostsListPost.getFeaturedImageUrl();
        } else {
            return null;
        }
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

    public int getStatusTextVisibility() {
        return ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) ? View.GONE : View.VISIBLE;
    }

    public String getStatusText() {
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            return "";
        }

        int statusTextResId = 0;

        if (mPostsListPost.isUploading()) {
            statusTextResId = R.string.post_uploading;
        } else if (mPostsListPost.isLocalDraft()) {
            statusTextResId = R.string.local_draft;
        } else if (mPostsListPost.hasLocalChanges()) {
            statusTextResId = R.string.local_changes;
        } else {
            switch (mPostsListPost.getStatusEnum()) {
                case DRAFT:
                    statusTextResId = R.string.draft;
                    break;
                case PRIVATE:
                    statusTextResId = R.string.post_private;
                    break;
                case PENDING:
                    statusTextResId = R.string.pending_review;
                    break;
                case SCHEDULED:
                    statusTextResId = R.string.scheduled;
                    break;
                case TRASHED:
                    statusTextResId = R.string.trashed;
                    break;
            }
        }

        return (statusTextResId != 0 ? mContext.getResources().getString(statusTextResId) : "");
    }

    public @ColorInt int getStatusTextColor() {
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            return 0;
        }

        int statusColorResId = R.color.grey_darken_10;

        if (mPostsListPost.isUploading()) {
            statusColorResId = R.color.alert_yellow;
        } else if (mPostsListPost.isLocalDraft()) {
            statusColorResId = R.color.alert_yellow;
        } else if (mPostsListPost.hasLocalChanges()) {
            statusColorResId = R.color.alert_yellow;
        } else {
            switch (mPostsListPost.getStatusEnum()) {
                case DRAFT:
                    statusColorResId = R.color.alert_yellow;
                    break;
                case PRIVATE:
                    break;
                case PENDING:
                    statusColorResId = R.color.alert_yellow;
                    break;
                case SCHEDULED:
                    statusColorResId = R.color.alert_yellow;
                    break;
                case TRASHED:
                    statusColorResId = R.color.alert_red;
                    break;
            }
        }

        return mContext.getResources().getColor(statusColorResId);
    }

    public Drawable getStatusTextLeftDrawable() {
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            return null;
        }

        int statusIconResId = 0;

        if (mPostsListPost.isUploading()) {
        } else if (mPostsListPost.isLocalDraft()) {
            statusIconResId = R.drawable.noticon_scheduled;
        } else if (mPostsListPost.hasLocalChanges()) {
            statusIconResId = R.drawable.noticon_scheduled;
        } else {
            switch (mPostsListPost.getStatusEnum()) {
                case DRAFT:
                    statusIconResId = R.drawable.noticon_scheduled;
                    break;
                case PRIVATE:
                    break;
                case PENDING:
                    statusIconResId = R.drawable.noticon_scheduled;
                    break;
                case SCHEDULED:
                    statusIconResId = R.drawable.noticon_scheduled;
                    break;
                case TRASHED:
                    statusIconResId = R.drawable.noticon_trashed;
                    break;
            }
        }

        return (statusIconResId != 0 ? mContext.getResources().getDrawable(statusIconResId) : null);
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
        return (canShowStatsForPost(mPostsListPost) && hasEnoughRoom()) ? View.VISIBLE : View.GONE;
    }

    public boolean canShowStatsForPost(PostsListPost post) {
        return mIsStatsSupported
                && post.getStatusEnum() == PostStatus.PUBLISHED
                && !post.isLocalDraft();
    }

    private boolean hasEnoughRoom() {
        boolean canShowStatsButton = canShowStatsForPost(mPostsListPost);
        int numVisibleButtons = (canShowStatsButton ? 4 : 3);

        return mAlwaysShowAllButtons || numVisibleButtons <= 3;
    }
}
