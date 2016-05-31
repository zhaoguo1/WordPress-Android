package org.wordpress.android.networking.media;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.Listener;
import com.wordpress.rest.RestRequest.ErrorListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaItemModel;
import org.wordpress.android.util.helpers.MediaFile;

import static android.content.Context.MODE_PRIVATE;

/**
 */
public class MediaRestInterface {
    public enum MEDIA_REST_ERROR {
        SYNC_FAILED
    }

    public interface MediaListener {
        Context getContext();

        /**
         * @return
         *  the remote site ID
         */
        String getSiteId();

        /**
         * Called after successfully retrieving all site media.
         */
        void onMediaSynced(MediaFile[] mediaItems);

        /**
         * Called after successfully retrieving a single media item.
         */
        void onMediaItemGet(MediaFile item);

        void onError(MEDIA_REST_ERROR error);
    }

    // Media item object JSON keys
    private static final String ID_KEY = "ID";
    private static final String URL_KEY = "URL";
    private static final String GUID_KEY = "guid";
    private static final String DATE_KEY = "date";
    private static final String POST_ID_KEY = "post_ID";
    private static final String AUTHOR_ID_KEY = "author_ID";
    private static final String FILE_KEY = "file";
    private static final String MIME_TYPE_KEY = "mime_type";
    private static final String EXTENSION_KEY = "extension";
    private static final String TITLE_KEY = "title";
    private static final String CAPTION_KEY = "caption";
    private static final String DESCRIPTION_KEY = "description";
    private static final String ALT_KEY = "alt";
    private static final String THUMBNAILS_KEY = "thumbnails";
    private static final String HEIGHT_KEY = "height";
    private static final String WIDTH_KEY = "width";
    private static final String EXIF_KEY = "exif";
    private static final String META_KEY = "meta";

    // Media item thumbnails object JSON keys
    private static final String THUMBNAIL_KEY = "thumbnail";
    private static final String MEDIUM_THUMNAIL_KEY = "medium";
    private static final String LARGE_THUMBNAIL_KEY = "large";
    private static final String POST_THUMBNAIL_KEY = "post-thumbnail";

    // Media item exif object JSON keys
    private static final String EXIF_APERTURE_KEY = "aperture";
    private static final String EXIF_CREDIT_KEY = "credit";
    private static final String EXIF_CAMERA_KEY = "camera";
    private static final String EXIF_CREATE_TIME_KEY = "created_timestamp";
    private static final String EXIF_COPYRIGHT_KEY = "copyright";
    private static final String EXIF_FOCAL_LENGTH_KEY = "focal_length";
    private static final String EXIF_ISO_KEY = "iso";
    private static final String EXIF_SHUTTER_SPEED_KEY = "shutter_speed";
    private static final String EXIF_ORIENTATION_KEY = "orientation";

    // Media item meta object JSON keys
    private static final String META_LINKS_KEY = "links";

    // Media item meta links object JSON keys
    private static final String META_SELF_LINK_KEY = "self";
    private static final String META_HELP_LINK_KEY = "help";
    private static final String META_SITE_LINK_KEY = "site";

    private static final String MEDIA_SYNC_FOUND_KEY = "found";
    private static final String MEDIA_SYNC_MEDIA_KEY = "media";

    private static final String MEDIA_REST_PREFS = "wp_media_rest_prefs";
    /** Timestamp of last sync of full media library data */
    private static final String LAST_SYNC_ATTEMPT_KEY = "media_last_sync_attempt";
    private static final String LAST_SYNC_KEY = "media_last_sync";

    /** 24 hour sync timer */
    private static final long DEF_SYNC_BLOCK_MS = 24 * 60 * 60 * 1000;

    private final MediaListener mListener;

    public MediaRestInterface(@NonNull MediaListener listener) {
        mListener = listener;
    }

    /**
     * Syncs local media data with remote WordPress media library. A sync block is in place to
     * prevent the operation from occurring more than once in a 24 hour time period.
     *
     * @param force
     *  if true the sync block is ignored
     * @return
     *  true if a network request was sent, otherwise false
     */
    public boolean syncMediaLibrary(boolean force) {
        if (mListener.getContext() == null) return false;

        // check .com site ID sync block
        String siteId = WordPress.currentBlog.getDotComBlogId();
        if (TextUtils.isEmpty(siteId) || (!force && isSyncBlocked())) return false;

        WordPress.getRestClientUtilsV1_1().getAllMedia(siteId, mSyncListener, mSyncErrorListener);
        return true;
    }

    /**
     * @return
     *  true if time since last sync was less than {@link MediaRestInterface#DEF_SYNC_BLOCK_MS}
     */
    public boolean isSyncBlocked() {
        return (System.currentTimeMillis() - getLastSyncTimestamp()) < DEF_SYNC_BLOCK_MS;
    }

    /**
     * @return the previous sync timestamp (in ms) or -1 if not set
     */
    public long getLastSyncAttemptTimestamp() {
        SharedPreferences prefs = getSharedPreferences();
        return prefs.getLong(LAST_SYNC_ATTEMPT_KEY, -1);
    }

    /**
     * @return the previous sync timestamp (in ms) or -1 if not set
     */
    public long getLastSyncTimestamp() {
        SharedPreferences prefs = getSharedPreferences();
        return prefs.getLong(LAST_SYNC_KEY, -1);
    }

    public void setLastSyncAttemptTimestamp(long timestamp) {
        SharedPreferences prefs = getSharedPreferences();
        prefs.edit().putLong(LAST_SYNC_ATTEMPT_KEY, timestamp).apply();
    }

    public void setLastSyncTimestamp(long timestamp) {
        SharedPreferences prefs = getSharedPreferences();
        prefs.edit().putLong(LAST_SYNC_KEY, timestamp).apply();
    }

    public MediaItemModel[] deserializeSyncResponse2(JSONObject response) {
        if (response == null || response.optInt(MEDIA_SYNC_FOUND_KEY) < 1) return null;

        try {
            JSONArray mediaList = response.getJSONArray(MEDIA_SYNC_MEDIA_KEY);
            MediaItemModel[] mediaItems = new MediaItemModel[mediaList.length()];
            for (int i = 0; i < mediaItems.length; ++i) {
                mediaItems[i] = deserializeMediaItem(mediaList.getJSONObject(i));
            }
            return mediaItems;
        } catch (JSONException e) {
        }

        return null;
    }

    public MediaFile[] deserializeSyncResponse(JSONObject response) {
        if (response == null || response.optInt(MEDIA_SYNC_FOUND_KEY) < 1) return null;

        try {
            JSONArray mediaList = response.getJSONArray(MEDIA_SYNC_MEDIA_KEY);
            MediaFile[] mediaItems = new MediaFile[mediaList.length()];
            for (int i = 0; i < mediaItems.length; ++i) {
                mediaItems[i] = deserializeMediaFile(mediaList.getJSONObject(i));
            }
            return mediaItems;
        } catch (JSONException e) {
        }

        return null;
    }

    public MediaFile deserializeMediaFile(@NonNull JSONObject json) {
        MediaFile file = new MediaFile();
        try {
            file.setId(json.getInt(ID_KEY));
            file.setPostID(json.optLong(POST_ID_KEY));
            file.setFilePath(null);
            file.setFileName(json.optString(TITLE_KEY));
            file.setTitle(json.optString(TITLE_KEY));
            file.setDescription(json.optString(DESCRIPTION_KEY));
            file.setCaption(json.optString(CAPTION_KEY));
            file.setWidth(json.optInt(WIDTH_KEY));
            file.setHeight(json.optInt(HEIGHT_KEY));
            file.setMimeType(json.optString(MIME_TYPE_KEY));
            file.setFileURL(json.optString(URL_KEY));
            file.setDateCreatedGMT(json.optLong(DATE_KEY));
        } catch (JSONException exception) {
            return null;
        }
        return file;
    }

    public MediaItemModel deserializeMediaItem(@NonNull JSONObject json) {
        MediaItemModel item = new MediaItemModel();
        try {
            item.id = json.getLong(ID_KEY);
            item.url = json.getString(URL_KEY);
            item.guid = json.getString(GUID_KEY);
            item.date = json.optString(DATE_KEY);
            item.postId = json.optLong(POST_ID_KEY);
            item.authorId = json.optLong(AUTHOR_ID_KEY);
            item.file = json.optString(FILE_KEY);
            item.mimeType = json.optString(MIME_TYPE_KEY);
            item.extension = json.optString(EXTENSION_KEY);
            item.title = json.optString(TITLE_KEY);
            item.caption = json.optString(CAPTION_KEY);
            item.description = json.optString(DESCRIPTION_KEY);
            item.alt = json.optString(ALT_KEY);
            item.height = json.optInt(HEIGHT_KEY);
            item.width = json.optInt(WIDTH_KEY);

            if (json.has(THUMBNAILS_KEY)) {
                JSONObject thumbnails = json.getJSONObject(THUMBNAILS_KEY);
                item.thumbnailUrl = thumbnails.optString(THUMBNAIL_KEY);
                item.mediumThumbnailUrl = thumbnails.optString(MEDIUM_THUMNAIL_KEY);
                item.largeThumbnailUrl = thumbnails.optString(LARGE_THUMBNAIL_KEY);
                item.postThumbnailUrl = thumbnails.optString(POST_THUMBNAIL_KEY);
            }

            if (json.has(EXIF_KEY)) {
                JSONObject exif = json.getJSONObject(EXIF_KEY);
                item.aperature = exif.optInt(EXIF_APERTURE_KEY);
                item.credit = exif.optString(EXIF_CREDIT_KEY);
                item.camera = exif.optString(EXIF_CAMERA_KEY);
                item.creationDate = exif.optLong(EXIF_CREATE_TIME_KEY);
                item.copyright = exif.optString(EXIF_COPYRIGHT_KEY);
                item.focalLength = exif.optInt(EXIF_FOCAL_LENGTH_KEY);
                item.iso = exif.optInt(EXIF_ISO_KEY);
                item.shutterSpeed = exif.optInt(EXIF_SHUTTER_SPEED_KEY);
                item.orientation = exif.optInt(EXIF_ORIENTATION_KEY);
            }

            if (json.has(META_KEY)) {
                JSONObject meta = json.getJSONObject(META_KEY);
                item.selfLink = meta.optString(META_SELF_LINK_KEY);
                item.helpLink = meta.optString(META_HELP_LINK_KEY);
                item.siteLink = meta.optString(META_SITE_LINK_KEY);
            }
        } catch (JSONException e) {
            return null;
        }
        return item;
    }

    private SharedPreferences getSharedPreferences() {
        return mListener.getContext().getSharedPreferences(MEDIA_REST_PREFS, MODE_PRIVATE);
    }

    private final Listener mSyncListener = new Listener() {
        @Override public void onResponse(JSONObject response) {
            mListener.onMediaSynced(deserializeSyncResponse(response));
        }
    };

    private final ErrorListener mSyncErrorListener = new ErrorListener() {
        @Override public void onErrorResponse(VolleyError error) {
            mListener.onError(MEDIA_REST_ERROR.SYNC_FAILED);
        }
    };
}
