package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.DateTimeUtils;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PageView}.
 */
public class PageViewModel extends BasePostViewModel {
    private int mPosition;
    private PostsListPost mPostsListPost;
    private PostsListPost mPostsListPostPrevious;

    public PageViewModel(int position, PostsListPost postsListPost, PostsListPost postsListPostPrevious) {
        super(postsListPost);

        mPosition = position;
        mPostsListPost = postsListPost;
        mPostsListPostPrevious = postsListPostPrevious;
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

    public int getMoreButtonVisibility() {
        // no "..." more button when uploading
        return mPostsListPost.isUploading() ? View.GONE : View.VISIBLE;
    }

    public int getDividerTopVisibility() {
        // only show the top divider for the first item
        return mPosition == 0 ? View.VISIBLE : View.GONE;
    }
}
