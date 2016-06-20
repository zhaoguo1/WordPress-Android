package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.VolleyUtils;

import java.net.HttpURLConnection;

public class ReaderBlogActions {

    public static class BlockedBlogResult {
        public long blogId;
        public ReaderPostList deletedPosts;
        public boolean wasFollowing;
    }

    private static String jsonToString(JSONObject json) {
        return (json != null ? json.toString() : "");
    }

    public static boolean followSiteByUrl(final String siteUrl,
                                          final boolean isAskingToFollow,
                                          final ActionListener actionListener) {
        if (TextUtils.isEmpty(siteUrl)) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        ReaderBlog blogInfo = ReaderBlogTable.getBlogInfoFromUrl(siteUrl);
        if (blogInfo != null) {
            return internalFollowSite(blogInfo, isAskingToFollow, actionListener);
        }

        updateFeedInfo(0, siteUrl, new UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                if (blogInfo != null) {
                    internalFollowSite(
                            blogInfo,
                            isAskingToFollow,
                            actionListener);
                } else {
                    ReaderActions.callActionListener(actionListener, false);
                }
            }
        });

        return true;
    }

    private static boolean internalFollowSite(
            final ReaderBlog blogInfo,
            final boolean isAskingToFollow,
            final ActionListener actionListener)
    {
        if (blogInfo == null) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        setIsFollowedBlog(blogInfo, isAskingToFollow);

        if (isAskingToFollow) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_FOLLOWED);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_UNFOLLOWED);
        }

        final String actionName = (isAskingToFollow ? "follow" : "unfollow");
        final String path = "read/following/mine/"
                + (isAskingToFollow ? "new" : "delete")
                + "?url=" + UrlUtils.urlEncode(blogInfo.getUrl());

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, isAskingToFollow);
                if (success) {
                    AppLog.d(T.READER, actionName + " succeeded");
                } else {
                    AppLog.w(T.READER, actionName + " failed - " + jsonToString(jsonObject) + " - " + path);
                    setIsFollowedBlog(blogInfo, !isAskingToFollow);
                }
                ReaderActions.callActionListener(actionListener, success);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "feed " + actionName + " failed with error");
                AppLog.e(T.READER, volleyError);
                setIsFollowedBlog(blogInfo, !isAskingToFollow);
                ReaderActions.callActionListener(actionListener, false);
            }
        };

        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
        return true;
    }

    /*
     * called when a follow/unfollow fails to restore local data to previous state
     */
    private static void setIsFollowedBlogId(long blogId, boolean isFollowed) {
        ReaderBlogTable.setIsFollowedBlogId(blogId, isFollowed);
        ReaderPostTable.setFollowStatusForPostsInBlog(blogId, isFollowed);
    }

    private static void setIsFollowedFeedId(long feedId, boolean isFollowed) {
        ReaderBlogTable.setIsFollowedFeedId(feedId, isFollowed);
        ReaderPostTable.setFollowStatusForPostsInFeed(feedId, isFollowed);
    }

    private static void setIsFollowedBlog(ReaderBlog blogInfo, boolean isFollowed) {
        if (blogInfo == null) return;

        ReaderDatabase.getWritableDb().beginTransaction();
        try {
            if (blogInfo.blogId != 0) {
                setIsFollowedBlogId(blogInfo.blogId, isFollowed);
            }
            if (blogInfo.feedId != 0) {
                setIsFollowedFeedId(blogInfo.feedId, isFollowed);
            }

            ReaderDatabase.getWritableDb().setTransactionSuccessful();
        } finally {
            ReaderDatabase.getWritableDb().endTransaction();
        }
    }

    /*
     * returns whether a follow/unfollow was successful based on the response to:
     *      read/follows/new
     *      read/follows/delete
     *      site/$site/follows/new
     *      site/$site/follows/mine/delete
     */
    private static boolean isFollowActionSuccessful(JSONObject json, boolean isAskingToFollow) {
        if (json == null) {
            return false;
        }

        boolean isSubscribed;
        if (json.has("subscribed")) {
            // read/follows/
            isSubscribed = json.optBoolean("subscribed", false);
        } else {
            // site/$site/follows/
            isSubscribed = json.has("is_following") && json.optBoolean("is_following", false);
        }
        return (isSubscribed == isAskingToFollow);
    }

    /*
     * request info about a specific blog
     */
    public static void updateBlogInfo(long blogId,
                                      final String blogUrl,
                                      final UpdateBlogInfoListener infoListener) {
        // must pass either a valid id or url
        final boolean hasBlogId = (blogId != 0);
        final boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);
        if (!hasBlogId && !hasBlogUrl) {
            AppLog.w(T.READER, "cannot get blog info without either id or url");
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, infoListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // authentication error may indicate that API access has been disabled for this blog
                int statusCode = VolleyUtils.statusCodeFromVolleyError(volleyError);
                boolean isAuthErr = (statusCode == HttpURLConnection.HTTP_FORBIDDEN);
                // if we failed to get the blog info using the id and this isn't an authentication
                // error, try again using just the domain
                if (!isAuthErr && hasBlogId && hasBlogUrl) {
                    AppLog.w(T.READER, "failed to get blog info by id, retrying with url");
                    updateBlogInfo(0, blogUrl, infoListener);
                } else {
                    AppLog.e(T.READER, volleyError);
                    if (infoListener != null) {
                        infoListener.onResult(null);
                    }
                }
            }
        };

        String path;
        if (hasBlogId) {
            path = "read/sites/" + blogId;
        } else {
            path = "read/sites/" + UrlUtils.urlEncode(UrlUtils.getHost(blogUrl));
        }
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    public static void updateFeedInfo(long feedId, String feedUrl, final UpdateBlogInfoListener infoListener) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateBlogInfoResponse(jsonObject, infoListener);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (infoListener != null) {
                    infoListener.onResult(null);
                }
            }
        };

        String path;
        if (feedId != 0) {
            path = "read/feed/" + feedId;
        } else {
            path = "read/feed/" + UrlUtils.urlEncode(feedUrl);
        }
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    private static void handleUpdateBlogInfoResponse(JSONObject jsonObject, UpdateBlogInfoListener infoListener) {
        if (jsonObject == null) {
            if (infoListener != null) {
                infoListener.onResult(null);
            }
            return;
        }

        ReaderBlog blogInfo = ReaderBlog.fromJson(jsonObject);
        ReaderBlogTable.addOrUpdateBlog(blogInfo);

        if (infoListener != null) {
            infoListener.onResult(blogInfo);
        }
    }

    /*
     * block a blog - result includes the list of posts that were deleted by the block so they
     * can be restored if the user undoes the block
     */
    public static BlockedBlogResult blockBlogFromReader(final long blogId, final ActionListener actionListener) {
        final BlockedBlogResult blockResult = new BlockedBlogResult();
        blockResult.blogId = blogId;
        blockResult.deletedPosts = ReaderPostTable.getPostsInBlog(blogId, 0, false);
        blockResult.wasFollowing = ReaderBlogTable.isFollowedBlog(blogId);

        ReaderPostTable.deletePostsInBlog(blogId);
        setIsFollowedBlogId(blogId, false);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderActions.callActionListener(actionListener, true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
                setIsFollowedBlogId(blogId, blockResult.wasFollowing);
                ReaderActions.callActionListener(actionListener, false);
            }
        };

        AppLog.i(T.READER, "blocking blog " + blogId);
        String path = "me/block/sites/" + Long.toString(blogId) + "/new";
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return blockResult;
    }

    public static void undoBlockBlogFromReader(final BlockedBlogResult blockResult) {
        if (blockResult == null) {
            return;
        }
        if (blockResult.deletedPosts != null) {
            ReaderPostTable.addOrUpdatePosts(null, blockResult.deletedPosts);
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = (jsonObject != null && jsonObject.optBoolean("success"));
                // re-follow the blog if it was being followed prior to the block
                if (success && blockResult.wasFollowing) {
                    refollowBlogById(blockResult.blogId);
                } else if (!success) {
                    AppLog.w(T.READER, "failed to unblock blog " + blockResult.blogId);
                }

            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };

        AppLog.i(T.READER, "unblocking blog " + blockResult.blogId);
        String path = "me/block/sites/" + Long.toString(blockResult.blogId) + "/delete";
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    private static void refollowBlogById(final long blogId) {
        if (blogId == 0) return;

        setIsFollowedBlogId(blogId, true);

        final String path = "sites/" + blogId + "/follows/new";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                boolean success = isFollowActionSuccessful(jsonObject, true);
                if (success) {
                    AppLog.d(T.READER, "refollow blog succeeded");
                } else {
                    AppLog.w(T.READER, "refollow blog failed - " + jsonToString(jsonObject) + " - " + path);
                    setIsFollowedBlogId(blogId, false);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.w(T.READER, "refollow blog failed with error");
                AppLog.e(T.READER, volleyError);
                setIsFollowedBlogId(blogId, false);
            }
        };

        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }
}
