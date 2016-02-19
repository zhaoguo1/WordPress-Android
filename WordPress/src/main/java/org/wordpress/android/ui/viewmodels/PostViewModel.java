package org.wordpress.android.ui.viewmodels;

import org.wordpress.android.R;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.view.View;

/**
 * ViewModel for the PostsListModel to post_cardview.xml
 */
public class PostViewModel {
    private Context mContext;
    private PostsListPost mPostsListPost;

    public PostViewModel(Context context, PostsListPost postsListPost) {
        mContext = context;
        mPostsListPost = postsListPost;
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

}
