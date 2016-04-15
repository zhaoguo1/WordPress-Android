package org.wordpress.android.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.R;

import static android.provider.DocumentsContract.*;

import static org.wordpress.android.provider.ProviderConstants.MIME_TYPE_ANY_IMAGE;
import static org.wordpress.android.provider.ProviderConstants.MIME_TYPE_ANY_VIDEO;

/**
 * The WordPress Documents Root acts as a local directory root that can be queried by the OS. This
 * allows WordPress.com users to browse their media library within the system file picker. The root
 * will be available system-wide, users will be able to access their content from any app that
 * utilizes the system picker for image and/or video selection.
 *
 * This class contains information used by {@link WPDocumentsProvider} to create and query media
 * library content.
 *
 * <b>Supported Content</b>
 * <ul>
 *     <li>Image, 'image/*'</li>
 *     <li>Video, 'video/*'</li>
 * </ul>
 *
 * <b>Roots provided:</b>
 * <ul>
 *     <li>WordPress.com media library</li>
 *     <li><strikethrough>Capture</strikethrough></li>
 * </ul>
 *
 * <b>Documents provided:</b>
 * <ul>
 *     <li>Root: queried to load root children, in this root there are three children</li>
 *     <li>All: directory with all images and videos in the current user's media library</li>
 *     <li>Image: directory with all images in the current user's media library</li>
 *     <li>Video: directory with all videos in the current user's media library</li>
 *     <li><strikethrough>Capture: directory with files for capturing content</strikethrough></li>
 * </ul>
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WPDocumentsRoot {
    public static final String[] ALL_ROOT_COLUMNS = {
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_MIME_TYPES
    };
    public static final String[] ALL_DOC_COLUMNS = {
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_SUMMARY,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_ICON,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    /** @see DocumentsContract.Root#COLUMN_MIME_TYPES */
    public static final String SUPPORTED_MIME_TYPES = MIME_TYPE_ANY_IMAGE + "\n" + MIME_TYPE_ANY_VIDEO;

    private static final String WP_ROOT_ID = "wpRoot";
    private static final String WP_ROOT_DOC_ID = "wpRootDocument";
    private static final String WP_ALL_DOC_ID = "wpAllMediaDocument";
    private static final String WP_IMAGE_DOC_ID = "wpImageDocument";
    private static final String WP_VIDEO_DOC_ID = "wpVideoDocument";

    private static final int WP_ROOT_ICON = R.mipmap.app_icon;
    private static final int WP_ALL_ICON = R.drawable.media_image_placeholder;
    private static final int WP_IMAGE_ICON = R.drawable.media_image_placeholder;
    private static final int WP_VIDEO_ICON = R.drawable.media_image_placeholder;

    private Object[] mRoot;
    private Object[] mRootDoc;
    private Object[] mAllDoc;
    private Object[] mImageDoc;
    private Object[] mVideoAllDoc;

    public WPDocumentsRoot(@NonNull Context context) {
        Resources res = context.getResources();
        String rootTitle = res.getString(R.string.wpdocprovider_root_title);
        String rootSummary = res.getString(R.string.wpdocprovider_root_summary);
        String rootDocSummary = res.getString(R.string.wpdocprovider_root_doc_summary);
        String allDocTitle = res.getString(R.string.wpdocprovider_all_doc_title);
        String imageDocTitle = res.getString(R.string.wpdocprovider_image_doc_title);
        String videoDocTitle = res.getString(R.string.wpdocprovider_video_doc_title);
        String allDocSummary = res.getString(R.string.wpdocprovider_all_doc_summary);
        String imageDocSummary = res.getString(R.string.wpdocprovider_image_doc_summary);
        String videoDocSummary = res.getString(R.string.wpdocprovider_video_doc_summary);

        mRoot = new Object[] {
                WP_ROOT_ID, 0, WP_ROOT_ICON, rootTitle, rootSummary,
                WP_ROOT_DOC_ID, 0L, SUPPORTED_MIME_TYPES };
        mRootDoc = new Object[] {
                WP_ROOT_DOC_ID, Document.MIME_TYPE_DIR, rootTitle,
                rootDocSummary, null, WP_ROOT_ICON, 0, 0L };
        mAllDoc = new Object[] {
                WP_ALL_DOC_ID, Document.MIME_TYPE_DIR, allDocTitle,
                allDocSummary, null, WP_ALL_ICON, 0, 0L };
        mImageDoc = new Object[] {
                WP_IMAGE_DOC_ID, Document.MIME_TYPE_DIR, imageDocTitle,
                imageDocSummary, null, WP_IMAGE_ICON, 0, 0L };
        mVideoAllDoc = new Object[] {
                WP_VIDEO_DOC_ID, Document.MIME_TYPE_DIR, videoDocTitle,
                videoDocSummary, null, WP_VIDEO_ICON, 0, 0L };
    }

    public boolean isRootDocId(@NonNull String docId) {
        return docId.equals(WP_ROOT_DOC_ID);
    }

    public boolean isAllDocId(@NonNull String docId) {
        return docId.equals(WP_ALL_DOC_ID);
    }

    public boolean isImageDocId(@NonNull String docId) {
        return docId.equals(WP_IMAGE_DOC_ID);
    }

    public boolean isVideoDocId(@NonNull String docId) {
        return docId.equals(WP_VIDEO_DOC_ID);
    }

    public boolean isKnownDocId(String docId) {
        return !TextUtils.isEmpty(docId) && (docId.equals(WP_ROOT_DOC_ID) ||
                docId.equals(WP_ALL_DOC_ID) ||
                docId.equals(WP_IMAGE_DOC_ID) ||
                docId.equals(WP_VIDEO_DOC_ID));
    }

    public Object[] getDoc(@NonNull String docId) {
        switch (docId) {
            case WP_ROOT_DOC_ID: return getRootDoc();
            case WP_ALL_DOC_ID: return getAllDoc();
            case WP_IMAGE_DOC_ID: return getImageDoc();
            case WP_VIDEO_DOC_ID: return getVideoDoc();
        }
        return null;
    }

    public Object[] getRoot() {
        return mRoot;
    }

    public Object[] getRootDoc() {
        return mRootDoc;
    }

    public Object[] getAllDoc() {
        return mAllDoc;
    }

    public Object[] getImageDoc() {
        return mImageDoc;
    }

    public Object[] getVideoDoc() {
        return mVideoAllDoc;
    }
}
