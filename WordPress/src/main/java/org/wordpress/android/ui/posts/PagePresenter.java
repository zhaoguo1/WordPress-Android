package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostsListContracts.PageActionHandler;
import org.wordpress.android.ui.posts.PostsListContracts.PostView;
import org.wordpress.android.ui.posts.PostsListContracts.PostsActionHandler;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.widgets.PostListButton;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PagePresenter extends BasePostPresenter<PageViewModel> implements PageActionHandler {
    private final PostView mPageView;
    private PostsListPost mPostsListPostPrevious;

    private final PostsListContracts.PostsActionHandler mPostsActionHandler;

    public PagePresenter(PostView pageView, PostsListPost postsListPost, PostsListPost postsListPostPrevious,
            PostsActionHandler postsActionHandler) {
        super(new PageViewModel(), postsListPost);

        mPageView = pageView;
        mPostsListPostPrevious = postsListPostPrevious;
        mPostsActionHandler = postsActionHandler;
    }

    public void setPostsListPosts(PostsListPost postsListPost, PostsListPost postsListPostPrevious) {
        mPostsListPost = postsListPost;
        mPostsListPostPrevious = postsListPostPrevious;

        displayPage();
    }

    @Override
    public void onPageSelected() {
        onPageButtonClick(PostListButton.BUTTON_PREVIEW);
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    @Override
    public void onMoreButtonClick(View view) {
        mViewModel.showPagePopupMenu(mPostsListPost);
    }

    @Override
    public void onPageButtonClick(int buttonType) {
        Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(mPostsListPost.getPostId());
        if (fullPost == null) {
            mPageView.showToast(R.string.post_not_found);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                mPageView.editBlogPostOrPageForResult(mPostsListPost.getPostId(), true);
                break;
            case PostListButton.BUTTON_PUBLISH:
                mPageView.publishPost(fullPost);
                break;
            case PostListButton.BUTTON_VIEW:
                mPageView.browsePostOrPage(WordPress.getCurrentBlog(), fullPost);
                break;
            case PostListButton.BUTTON_PREVIEW:
                mPageView.viewPostPreviewForResult(fullPost, true);
                break;
            case PostListButton.BUTTON_STATS:
                mPageView.viewStatsSinglePostDetails(fullPost, true);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                if (!mPostsListPost.isUploading()) {
                    mPostsActionHandler.onTrashPost(mPostsListPost);
                }
                break;
        }
    }

    @Override
    public void init() {
        displayPage();
    }

    @Override
    public void willBeFirstStart() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    private void displayPage() {
        displayCommon(mViewModel, mPageView.getContext());

        // set date
        mViewModel.date.set(getPageDateHeaderText(mPostsListPost));

        // set date header visibility
        // don't show date header if same as previous
        boolean showDate;
        if (mPostsListPostPrevious != null) {
            String prevDateStr = getPageDateHeaderText(mPostsListPostPrevious);
            showDate = !prevDateStr.equals(getPageDateHeaderText(mPostsListPost));
        } else {
            showDate = true;
        }
        mViewModel.dateHeaderVisibility.set(showDate ? View.VISIBLE : View.GONE);

        // set More button visibility
        // no "..." more button when uploading
        mViewModel.moreButtonVisibility.set(mPostsListPost.isUploading() ? View.GONE : View.VISIBLE);

        // set divider top visibility
        // only show the top divider for the first item
        mViewModel.dividerTopVisibility.set(mPostsListPostPrevious == null ? View.VISIBLE : View.GONE);
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
    private String getPageDateHeaderText(PostsListPost page) {
        final Context context = mPageView.getContext();

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
}
