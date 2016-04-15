package org.wordpress.android.provider;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.media.MediaGridFragment;
import org.xmlrpc.android.ApiHelper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class WPDocumentsProvider extends DocumentsProvider {
    private static final String PROVIDER_PREFERENCES = "wp_documents_provider";
    private static final String LAST_SYNC_KEY = "last_pdated";
    private static final String THUMBNAIL_DIR = "/WordPress/thumbnails";
    private static final String JPG_EXTENSION = ".jpg";
    private static final long MIN_SYNC_WAIT = 600L;

    private final Map<String, String> mDocumentIdToThumbnail = new HashMap<>();

    private WPDocumentsRoot mRoot;
    private long mLastSync;
    private ApiHelper.SyncMediaLibraryTask.Callback mSyncMediaCallback;

    @Override
    public boolean onCreate() {
        if (getContext() == null || !AccountHelper.isSignedInWordPressDotCom()) return false;
        mSyncMediaCallback = null;
        mLastSync = getLastSyncTimestamp(getContext());
        mRoot = new WPDocumentsRoot(getContext());
        refreshAllMedia(false);
        return true;
    }

    @Override
    public Cursor queryRoots(String[] columns)
            throws FileNotFoundException {
        if (getContext() == null) return null;
        if (columns == null) columns = ALL_ROOT_COLUMNS;
        if (mRoot == null) mRoot = new WPDocumentsRoot(getContext());
        MatrixCursor roots = new MatrixCursor(columns);
        addRow(roots.newRow(), ALL_ROOT_COLUMNS, mRoot.getRoot());
        return roots;
    }

    @Override
    public Cursor queryDocument(String docId, String[] columns)
            throws FileNotFoundException {
        if (TextUtils.isEmpty(docId)) throw new FileNotFoundException();
        if (columns == null) columns = ALL_DOC_COLUMNS;

        MatrixCursor result = new MatrixCursor(columns);
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());

        if (mRoot.isKnownDocId(docId)) {
            if (mRoot.isRootDocId(docId)) {
                // Add root directories
                addRow(result.newRow(), ALL_DOC_COLUMNS, mRoot.getDoc(docId));
            } else if (mRoot.isAllDocId(docId)) {
                if (WordPress.wpDB.getMediaCountAll(blogId) == 0) {
                    refreshAllMedia(true);
                } else {
                    addAllWordPressMedia(result);
                }
            } else if (mRoot.isImageDocId(docId)) {
                if (WordPress.wpDB.getMediaCountImages(blogId) == 0) {
                    refreshAllMedia(true);
                } else {
                    addWordPressImages(result);
                }
            } else if (mRoot.isVideoDocId(docId)) {
                if (WordPress.wpDB.getMediaCountAll(blogId) == 0) {
                    refreshAllMedia(true);
                } else {
                    addWordPressVideos(result);
                }
            }
        } else {
            // TODO: actual content queried!
        }

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
            addAllWordPressMedia(result);
        } else if (mRoot.isImageDocId(parentId)) {
            addWordPressImages(result);
        } else if (mRoot.isVideoDocId(parentId)) {
            addWordPressVideos(result);
        }

        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        return null;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point size, CancellationSignal signal)
            throws FileNotFoundException {
        if (getContext() == null) return null;
        String thumbnail = mDocumentIdToThumbnail.get(docId);
        File dir = new File(getContext().getFilesDir() + THUMBNAIL_DIR);
        if (!dir.exists() && !dir.mkdirs()) return null;

        File file = new File(dir, docId + JPG_EXTENSION);
        try {
            if (!file.exists() && !file.createNewFile()) return null;
            URL url = new URL(thumbnail.replace("https", "http").replace("?w=150", "?w=" + size.x));
            InputStream thumbnailStream = url.openStream();
            FileOutputStream fileStream = new FileOutputStream(file);
            int length;
            byte[] data = new byte[1024];
            while ((length = thumbnailStream.read(data)) != -1) {
                fileStream.write(data, 0, length);
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

    private long getLastSyncTimestamp(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PROVIDER_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getLong(LAST_SYNC_KEY, -1);
    }

    private void setLastSyncTimestamp(@NonNull Context context, long timestamp) {
        SharedPreferences prefs =
                context.getSharedPreferences(PROVIDER_PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_SYNC_KEY, timestamp).apply();
    }

    private void refreshAllMedia(boolean force) {
        if (mSyncMediaCallback != null || getContext() == null) return;

        long curTime = System.currentTimeMillis();
        if (!force && (curTime - mLastSync) < MIN_SYNC_WAIT) return;

        setLastSyncTimestamp(getContext(), curTime);
        mSyncMediaCallback = new ApiHelper.SyncMediaLibraryTask.Callback() {
            @Override
            public void onSuccess(int count) {
                String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                if (WordPress.wpDB.getMediaCountAll(blogId) == 0 && count == 0) {
                    // There is no media at all
                }
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                if (errorType != ApiHelper.ErrorType.NO_ERROR) {
                }
            }
        };
        List<Object> apiArgs = new ArrayList<>();
        apiArgs.add(WordPress.getCurrentBlog());
        ApiHelper.SyncMediaLibraryTask getMediaTask = new ApiHelper.SyncMediaLibraryTask(0, MediaGridFragment.Filter.ALL, mSyncMediaCallback);
        getMediaTask.execute(apiArgs);
    }

    private void addImageRow(@NonNull Cursor cursor, @NonNull MatrixCursor rowParent) {
        int mediaIdColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID);
        int fileUrlColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL);
        int filePathColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH);
        int thumbnailColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_THUMBNAIL_URL);
        int titleColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_TITLE);
        int summaryColumn = cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DESCRIPTION);
        int mediaId = cursor.getInt(mediaIdColumn);
        String fileUrl = cursor.getString(fileUrlColumn);
        String filePath = cursor.getString(filePathColumn);
        String thumbnailUrl = cursor.getString(thumbnailColumn);

        // Store thumbnail URL for future lookup
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            mDocumentIdToThumbnail.put(String.valueOf(mediaId), thumbnailUrl);
        }

        if (verifyLocalImage(mediaId, fileUrl, filePath)) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(fileUrl));
            Object[] imageData = { mediaId, mimeType, cursor.getString(titleColumn),
                    cursor.getString(summaryColumn), null, R.drawable.media_image_placeholder,
                    Document.FLAG_SUPPORTS_THUMBNAIL, null };
            addRow(rowParent.newRow(), ALL_DOC_COLUMNS, imageData);
        }
    }

    private boolean verifyLocalImage(int id, String url, String path) {
        return id >= 0 && !(TextUtils.isEmpty(url) && TextUtils.isEmpty(path));
    }

    private void addRow(MatrixCursor.RowBuilder row, String[] columns, Object[] data) {
        if (row == null || columns == null || data == null || columns.length != data.length) return;
        for (int i = 0; i < columns.length; ++i) {
            row.add(columns[i], data[i]);
        }
    }

    private void addAllWordPressMedia(MatrixCursor cursor) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Cursor images = WordPress.wpDB.getMediaFilesForBlog(blogId);
        if (!images.moveToFirst()) {
            // TODO: query WordPress.com media library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Bundle loading = new Bundle();
                loading.putBoolean(DocumentsContract.EXTRA_LOADING, true);
                cursor.setExtras(loading);
            }
            return;
        } else {
            // TODO: query for changes to WordPress.com media library
        }

        do {
            addImageRow(images, cursor);
        } while (images.moveToNext());
    }

    private void addWordPressImages(MatrixCursor cursor) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Cursor images = WordPress.wpDB.getMediaImagesForBlog(blogId);
        if (!images.moveToFirst()) {
            // TODO: query WordPress.com media library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Bundle loading = new Bundle();
                loading.putBoolean(DocumentsContract.EXTRA_LOADING, true);
                cursor.setExtras(loading);
            }
            return;
        } else {
            // TODO: query for changes to WordPress.com media library
        }

        do {
            addImageRow(images, cursor);
        } while (images.moveToNext());
    }

    private void addWordPressVideos(MatrixCursor cursor) {
    }
}
