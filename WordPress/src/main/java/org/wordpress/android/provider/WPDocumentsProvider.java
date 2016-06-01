package org.wordpress.android.provider;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.networking.media.MediaRestInterface;
import org.wordpress.android.networking.media.MediaRestInterface.MediaListener;
import org.wordpress.android.util.helpers.MediaFile;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.WordPressDB.*;
import static org.wordpress.android.provider.WPDocumentsRoot.*;
import static org.wordpress.android.provider.ProviderConstants.*;

import static android.webkit.MimeTypeMap.*;
import static android.provider.DocumentsContract.Document;

/**
 * {@link DocumentsProvider} that allows WordPress media library items to be chosen from the system
 * file picker (launched via {@link android.content.Intent#ACTION_GET_CONTENT}).
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WPDocumentsProvider extends DocumentsProvider implements MediaListener {
    private static final String THUMBNAIL_DIR = "/WordPress/thumbnails";
    private static final String JPG_EXTENSION = ".jpg";

    // loading indicator data for API 19-22
    private static final int LOADING_ID = 0;
    private static final int LOADING_PLACEHOLDER = 0;
    private static final String LOADING_TITLE = "Loading";
    private static final String LOADING_DESC = "Gather blog media";

    private static final String[] MEDIA_COLUMN_NAMES = { COLUMN_NAME_MEDIA_ID,
                                                         COLUMN_NAME_FILE_URL,
                                                         COLUMN_NAME_TITLE,
                                                         COLUMN_NAME_DESCRIPTION };

    private WPDocumentsRoot mRoot;
    private MediaRestInterface mRestInterface;

    @Override
    public boolean onCreate() {
        if (getContext() == null) return false;
        mRoot = new WPDocumentsRoot(getContext());
        return true;
    }

    @Override
    public Cursor queryRoots(String[] columns)
            throws FileNotFoundException {
        if (columns == null) columns = ALL_ROOT_COLUMNS;

        MatrixCursor roots = new MatrixCursor(columns);

        // only add root if user is signed into WordPress.com, otherwise no media is available
        if (WordPress.wpDB != null && AccountHelper.isSignedInWordPressDotCom()) {
            String summary = getContext().getString(R.string.wpdocprovider_root_summary_format);
            mRoot.setRootSummary(String.format(summary, WordPress.currentBlog.getBlogName()));
            mRestInterface = new MediaRestInterface(this);
            if (!mRestInterface.syncMediaLibrary(false)) {
                // TODO: sync not initiated for some reason
            }
            addRow(roots.newRow(), ALL_ROOT_COLUMNS, mRoot.getRoot());
        }

        return roots;
    }

    @Override
    public Cursor queryDocument(String docId, String[] columns)
            throws FileNotFoundException {
        if (TextUtils.isEmpty(docId)) throw new FileNotFoundException();
        if (columns == null) columns = ALL_DOC_COLUMNS;

        MatrixCursor result = new MatrixCursor(columns);
        addRow(result.newRow(), ALL_DOC_COLUMNS, getDocument(docId));
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentId, String[] columns, String sortOrder)
            throws FileNotFoundException {
        if (columns == null) columns = ALL_DOC_COLUMNS;

        MatrixCursor result = new MatrixCursor(columns);

        if (mRoot.isRootDocId(parentId)) {
            addRow(result.newRow(), ALL_DOC_COLUMNS, mRoot.getAllDoc());
            addRow(result.newRow(), ALL_DOC_COLUMNS, mRoot.getImageDoc());
            addRow(result.newRow(), ALL_DOC_COLUMNS, mRoot.getVideoDoc());
        } else if (mRoot.isAllDocId(parentId)) {
            addLibraryItems(SUPPORTED_MIME_TYPES, result);
        } else if (mRoot.isImageDocId(parentId)) {
            addLibraryItems(MIME_TYPE_IMAGES, result);
        } else if (mRoot.isVideoDocId(parentId)) {
            addLibraryItems(MIME_TYPE_VIDEOS, result);
        }

        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        // TODO
        return null;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point size, CancellationSignal signal)
            throws FileNotFoundException {
        if (getContext() == null) return null;

        String thumbnail = WordPress.wpDB.getMediaThumbnailUrl(WordPress.getCurrentLocalTableBlogId(), Long.valueOf(docId));
        File dir = new File(getContext().getFilesDir() + THUMBNAIL_DIR);
        if (!dir.exists() && !dir.mkdirs()) return null;

        File file = new File(dir, docId + JPG_EXTENSION);
        try {
            if (!file.exists() && !file.createNewFile()) return null;
            // replace width argument with suggested size
            URL url = new URL(thumbnail);
            InputStream thumbnailStream = url.openStream();
            FileOutputStream fileStream = new FileOutputStream(file);
            int length;
            byte[] streamData = new byte[1024];
            while ((length = thumbnailStream.read(streamData)) != -1) {
                fileStream.write(streamData, 0, length);
                if (signal.isCanceled()) return null;
            }
            fileStream.close();
            thumbnailStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        final ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    private Object[] getDocument(@NonNull String docId) {
        if (mRoot.isKnownDocId(docId)) return mRoot.getDoc(docId);
        String blogId = String.valueOf(WordPress.getCurrentLocalTableBlogId());
        Cursor docData = WordPress.wpDB.getMediaFile(blogId, docId);
        if (!docData.moveToFirst()) {
            docData.close();
            return null;
        }
        MediaFile doc = WordPress.wpDB.getMediaFile(docData);
        docData.close();
        int icon = doc.isVideo() ? R.drawable.ic_action_video : R.drawable.ic_action_camera;
        return new Object[] {
                docId, doc.getMimeType(), doc.getTitle(), doc.getDescription(), null, icon, 0, 0L
            };
    }

    private void addLibraryItems(@NonNull String types, @NonNull MatrixCursor cursor) {
        Cursor media = getBlogMediaFiles(types);

        if (media.moveToFirst()) {
            do {
                String mimeType = extractMimeType(media);
                Object[] data = null;
                if (!isExpectedMimeType(types, mimeType)) continue;
                if (isImageMimeType(mimeType)) {
                    data = extractMediaData(media, COLUMN_NAME_FILE_URL);
                } else if (isVideoMimeType(mimeType)) {
                    data = extractMediaData(media, COLUMN_NAME_MEDIA_ID);
                }
                if (data != null) addRow(cursor.newRow(), ALL_DOC_COLUMNS, data);
            } while (media.moveToNext());
        }
        media.close();
    }

    private @NonNull String extractMimeType(@NonNull Cursor cursor) {
        if (cursor.isBeforeFirst() || cursor.isAfterLast()) return "";
        int fileUrlColumn = cursor.getColumnIndex(COLUMN_NAME_FILE_URL);
        String fileUrl = cursor.getString(fileUrlColumn);
        return getSingleton().getMimeTypeFromExtension(getFileExtensionFromUrl(fileUrl));
    }

    private Object[] extractMediaData(@NonNull Cursor cursor, @NonNull String urlColumn) {
        Map<String, Object> mediaData = extractData(cursor, MEDIA_COLUMN_NAMES);
        if (mediaData == null) return null;
        String mimeType = getSingleton().getMimeTypeFromExtension(
                getFileExtensionFromUrl(mediaData.get(urlColumn).toString()));
        return new Object[] { mediaData.get(COLUMN_NAME_MEDIA_ID), mimeType,
                mediaData.get(COLUMN_NAME_TITLE), mediaData.get(COLUMN_NAME_DESCRIPTION), null,
                R.drawable.media_image_placeholder, Document.FLAG_SUPPORTS_THUMBNAIL, null };
    }

    private Cursor getBlogMediaFiles(@NonNull String types) {
        String blogId = String.valueOf(WordPress.getCurrentLocalTableBlogId());
        if (types.equals(MIME_TYPE_IMAGES)) {
            return WordPress.wpDB.getMediaImagesForBlog(blogId);
        }
        return WordPress.wpDB.getMediaFilesForBlog(blogId);
    }

    private void showLoadingIndicator(@NonNull MatrixCursor cursor, @NonNull String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+ can set cursor extras to indicate loading
            Bundle loading = new Bundle();
            loading.putBoolean(DocumentsContract.EXTRA_LOADING, true);
            cursor.setExtras(loading);
        } else {
            // API 19-22 must add a placeholder file to indicate loading
            addRow(cursor.newRow(), ALL_DOC_COLUMNS, loadingIndicatorData(mimeType));
        }
    }

    private Object[] loadingIndicatorData(@NonNull String mimeType) {
        return new Object[] { LOADING_ID, mimeType, LOADING_TITLE, LOADING_DESC, null,
                LOADING_PLACEHOLDER, 0, null };
    }

    private final RestRequest.ErrorListener mRestErrorListener = new RestRequest.ErrorListener() {
        @Override public void onErrorResponse(VolleyError error) {
        }
    };

    // TODO: move this to SqlUtils
    private void addRow(MatrixCursor.RowBuilder row, String[] columns, Object[] data) {
        if (row == null || columns == null || data == null || columns.length != data.length) return;
        for (int i = 0; i < columns.length; ++i) {
            row.add(columns[i], data[i]);
        }
    }

    // TODO: move this to SqlUtils
    private Map<String, Object> extractData(Cursor cursor, @NonNull String[] columns) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) return null;

        Map<String, Object> data = new HashMap<>();
        for (String column : columns) {
            int index = cursor.getColumnIndex(column);
            if (index < 0) continue;
            switch (cursor.getType(index)) {
                case Cursor.FIELD_TYPE_STRING:
                    data.put(column, cursor.getString(index));
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    data.put(column, cursor.getInt(index));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    data.put(column, cursor.getFloat(index));
                    break;
                default:
                    break;
            }
        }
        return data;
    }

    @Override
    public String getSiteId() {
        return WordPress.getCurrentRemoteBlogId();
    }

    @Override
    public void onMediaSynced(MediaFile[] mediaItems) {
        if (mediaItems == null || mediaItems.length == 0) {
            // TODO no media
        } else {
            // TODO show media in adapter
        }
    }

    @Override
    public void onMediaItemGet(MediaFile item) {
        if (item == null) {
            // TODO no media
        } else {
            // TODO refresh adapter
        }
    }

    @Override
    public void onError(MediaRestInterface.MEDIA_REST_ERROR error) {
        // TODO
    }
}
