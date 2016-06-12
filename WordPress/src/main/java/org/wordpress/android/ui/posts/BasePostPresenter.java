package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.BasePresenter;

import android.content.Context;
import android.view.View;

public abstract class BasePostPresenter implements BasePresenter {

    protected PostsListPost mPostsListPost;

    public PostsListPost getPostsListPost() {
        return mPostsListPost;
    }

    protected void displayCommon(BasePostViewModel basePostViewModel, Context context) {
        // set title
        if (mPostsListPost.hasTitle()) {
            basePostViewModel.title.set(mPostsListPost.getTitle());
        } else {
            basePostViewModel.title.set("(" + context.getResources().getString(R.string.untitled) + ")");
        }

        // set status text
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            basePostViewModel.statusText.set("");
        } else {
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

            basePostViewModel.statusText.set(statusTextResId != 0 ?
                    context.getResources().getString(statusTextResId) : "");
        }

        // set status text color
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            basePostViewModel.statusTextColor.set(0);
        } else {
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

            basePostViewModel.statusTextColor.set(context.getResources().getColor(statusColorResId));
        }

        // set status text visibility
        basePostViewModel.statusTextVisibility.set(
                ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                        !mPostsListPost.hasLocalChanges()) ? View.GONE : View.VISIBLE);

        // set status text left drawable
        if ((mPostsListPost.getStatusEnum() == PostStatus.PUBLISHED) && !mPostsListPost.isLocalDraft() &&
                !mPostsListPost.hasLocalChanges()) {
            basePostViewModel.statusTextLeftDrawable.set(null);
        } else {
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

            basePostViewModel.statusTextLeftDrawable.set(statusIconResId != 0 ?
                    context.getResources().getDrawable(statusIconResId) : null);
        }

    }
}
