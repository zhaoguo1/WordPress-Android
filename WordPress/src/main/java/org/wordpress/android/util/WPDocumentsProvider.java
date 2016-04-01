package org.wordpress.android.util;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.AccountHelper;

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
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static android.provider.DocumentsContract.Root;
import static android.provider.DocumentsContract.Document;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class WPDocumentsProvider extends DocumentsProvider {
    // Root information
    public static final String ROOT_ID = "wpDocumentsProviderRoot";
    public static final int ROOT_FLAGS = 0;
    public static final int ROOT_ICON = R.mipmap.app_icon;
    public static final String ROOT_TITLE = "WordPress";
    public static final String ROOT_SUMMARY = "Media Library";
    public static final String ROOT_DOC_ID = "root:";
    public static final long ROOT_AVAILABLE_BYTES = 0L;
    public static final String ROOT_MIME_TYPES = "image/*";

    // Root Document information
    public static final String ROOT_DOC_MIME_TYPE = Document.MIME_TYPE_DIR;
    public static final String ROOT_DOC_TITLE = "Whole Library";
    public static final String ROOT_DOC_SUMMARY = "All your images and videos";
    public static final int ROOT_DOC_FLAGS = 0;
    public static final int ROOT_DOC_ICON = R.mipmap.app_icon;
    public static final long ROOT_DOC_SIZE = 0L;

    private static final String[] DEFAULT_ROOT_COLUMNS = {
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_MIME_TYPES
    };
    private static final String[] DEFAULT_DOC_COLUMNS = {
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_SUMMARY,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_ICON,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    private final Map<Integer, String> mDocumentIdToThumbnail = new HashMap<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] columns) throws FileNotFoundException {
        if (columns == null) columns = DEFAULT_ROOT_COLUMNS;
        MatrixCursor result = new MatrixCursor(columns);
        if (!AccountHelper.isSignedInWordPressDotCom()) return result;
        addRootRow(result);
        return result;
    }

    @Override
    public Cursor queryDocument(String docId, String[] columns) throws FileNotFoundException {
        if (columns == null) columns = DEFAULT_DOC_COLUMNS;
        MatrixCursor result = new MatrixCursor(columns);
        if (ROOT_DOC_ID.equals(docId)) {
            addRootDocumentRow(result);
        } else {
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentId, String[] columns, String sortOrder) throws FileNotFoundException {
        if (columns == null) columns = DEFAULT_DOC_COLUMNS;
        MatrixCursor result = new MatrixCursor(columns);

        Cursor cursor = WordPress.wpDB.getMediaImagesForBlog(String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId()));
        if (!cursor.moveToFirst()) {
            // TODO: query WordPress.com media library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Bundle loading = new Bundle();
                loading.putBoolean(DocumentsContract.EXTRA_LOADING, true);
                result.setExtras(loading);
            }
            return result;
        } else {
            // TODO: query for changes to WordPress.com media library
        }

        do {
            addImageRow(cursor, result);
        } while (cursor.moveToNext());

        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal) throws FileNotFoundException {
        return null;
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

        if (verifyLocalImage(mediaId, fileUrl, filePath)) {
            if (!TextUtils.isEmpty(thumbnailUrl)) {
                mDocumentIdToThumbnail.put(mediaId, thumbnailUrl);
            }

            MatrixCursor.RowBuilder row = rowParent.newRow();
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(fileUrl));

            row.add(Document.COLUMN_DOCUMENT_ID, mediaId);
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add(Document.COLUMN_DISPLAY_NAME, cursor.getString(titleColumn));
            row.add(Document.COLUMN_SUMMARY, cursor.getString(summaryColumn));
            row.add(Document.COLUMN_LAST_MODIFIED, null);
            row.add(Document.COLUMN_ICON, R.drawable.media_image_placeholder);
            row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL);
            row.add(Document.COLUMN_SIZE, null);
        }
    }

    private boolean verifyLocalImage(int id, String url, String path) {
        return id >= 0 && !(TextUtils.isEmpty(url) && TextUtils.isEmpty(path));
    }

    private void addRootRow(MatrixCursor cursor) {
        MatrixCursor.RowBuilder builder = cursor.newRow();
        builder.add(Root.COLUMN_ROOT_ID, ROOT_ID);
        builder.add(Root.COLUMN_FLAGS, ROOT_FLAGS);
        builder.add(Root.COLUMN_ICON, ROOT_ICON);
        builder.add(Root.COLUMN_TITLE, ROOT_TITLE);
        builder.add(Root.COLUMN_SUMMARY, ROOT_SUMMARY);
        builder.add(Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID);
        builder.add(Root.COLUMN_AVAILABLE_BYTES, ROOT_AVAILABLE_BYTES);
        builder.add(Root.COLUMN_MIME_TYPES, ROOT_MIME_TYPES);
    }

    private void addRootDocumentRow(MatrixCursor cursor) {
        MatrixCursor.RowBuilder builder = cursor.newRow();
        builder.add(Document.COLUMN_DOCUMENT_ID, ROOT_DOC_ID);
        builder.add(Document.COLUMN_MIME_TYPE, ROOT_DOC_MIME_TYPE);
        builder.add(Document.COLUMN_DISPLAY_NAME, ROOT_DOC_TITLE);
        builder.add(Document.COLUMN_SUMMARY, ROOT_DOC_SUMMARY);
        builder.add(Document.COLUMN_LAST_MODIFIED, null);
        builder.add(Document.COLUMN_ICON, ROOT_DOC_ICON);
        builder.add(Document.COLUMN_FLAGS, ROOT_DOC_FLAGS);
        builder.add(Document.COLUMN_SIZE, ROOT_DOC_SIZE);
    }
}
