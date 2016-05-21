package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.DateTimeUtils;

import android.content.Context;
import android.databinding.BaseObservable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.text.format.DateUtils;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PageView}.
 */
public class PageViewModel extends BaseObservable {
    private final Context mContext;

    private int mPosition;
    private PostsListPost mPostsListPost;
    private PostsListPost mPostsListPostPrevious;

    public PageViewModel(Context context, int position, PostsListPost postsListPost, PostsListPost
            postsListPostPrevious) {
        mContext = context;
        mPosition = position;
        mPostsListPost = postsListPost;
        mPostsListPostPrevious = postsListPostPrevious;
    }

    public String getTitle() {
        if (mPostsListPost.hasTitle()) {
            return mPostsListPost.getTitle();
        } else {
            return "(" + mContext.getResources().getText(R.string.untitled) + ")";
        }
    }

    public String getDate() {
        return getPageDateHeaderText(mContext, mPostsListPost);
    }

    public int getDateHeaderVisibility() {
        // don't show date header if same as previous
        boolean showDate;
        if (mPostsListPostPrevious != null) {
            String prevDateStr = getPageDateHeaderText(mContext, mPostsListPostPrevious);
            showDate = !prevDateStr.equals(getPageDateHeaderText(mContext, mPostsListPost));
        } else {
            showDate = true;
        }
        return showDate ? View.VISIBLE : View.GONE;
    }

    /*
     * returns the caption to show in the date header for the passed page - pages with the same
     * caption will be grouped together
     *  - if page is local draft, returns "Local draft"
     *  - if page is scheduled, returns formatted date w/o time
     *  - if created today or yesterday, returns "Today" or "Yesterday"
     *  - if created this month, returns the number of days ago
     *  - if created this year, returns the month name
     *  - if created before this year, returns the month name with year
     */
    private static String getPageDateHeaderText(Context context, PostsListPost page) {
        if (page.isLocalDraft()) {
            return context.getString(R.string.local_draft);
        } else if (page.getStatusEnum() == PostStatus.SCHEDULED) {
            return DateUtils.formatDateTime(context, page.getDateCreatedGmt(), DateUtils.FORMAT_ABBREV_ALL);
        } else {
            Date dtCreated = new Date(page.getDateCreatedGmt());
            Date dtNow = DateTimeUtils.nowUTC();
            int daysBetween = DateTimeUtils.daysBetween(dtCreated, dtNow);
            if (daysBetween == 0) {
                return context.getString(R.string.today);
            } else if (daysBetween == 1) {
                return context.getString(R.string.yesterday);
            } else if (DateTimeUtils.isSameMonthAndYear(dtCreated, dtNow)) {
                return String.format(context.getString(R.string.days_ago), daysBetween);
            } else if (DateTimeUtils.isSameYear(dtCreated, dtNow)) {
                return new SimpleDateFormat("MMMM").format(dtCreated);
            } else {
                return new SimpleDateFormat("MMMM yyyy").format(dtCreated);
            }
        }
    }

    public int getStatusVisibility() {
        return (mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges() ? View.GONE : View.VISIBLE;
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

    public int getMoreButtonVisibility() {
        // no "..." more button when uploading
        return mPostsListPost.isUploading() ? View.GONE : View.VISIBLE;
    }

    public int getDividerTopVisibility() {
        // only show the top divider for the first item
        return mPosition == 0 ? View.VISIBLE : View.GONE;
    }
}
