package org.wordpress.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

import java.util.HashMap;
import java.util.Map;

public class WPContentProvider extends ContentProvider {
    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_WP_ID = WordPressDB.COLUMN_NAME_MEDIA_ID;
    public static final String COLUMN_TYPE = WordPressDB.COLUMN_NAME_MIME_TYPE;
    public static final String COLUMN_TITLE = WordPressDB.COLUMN_NAME_TITLE;
    public static final String COLUMN_SUMMARY = WordPressDB.COLUMN_NAME_DESCRIPTION;
    public static final String COLUMN_ICON = WordPressDB.COLUMN_NAME_THUMBNAIL_URL;
    public static final String COLUMN_FLAGS = "wp_flags";
    public static final String COLUMN_SIZE = "wp_size";
    public static final String COLUMN_LAST_MODIFIED = "wp_modified";

    private static final String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_WP_ID,
            COLUMN_TYPE,
            COLUMN_TITLE,
            COLUMN_SUMMARY,
            COLUMN_ICON,
            COLUMN_FLAGS,
            COLUMN_SIZE,
            COLUMN_LAST_MODIFIED,
    };

    private int mBlogId;
    private Map<String,String[]> mMedia = new HashMap<>();

    @Override
    public boolean onCreate() {
        if (WordPress.wpDB == null) return false;

        mBlogId = WordPress.getCurrentBlog().getLocalTableBlogId();
        Cursor imageCursor = WordPress.wpDB.getMediaImagesForBlog(String.valueOf(mBlogId));
        if (imageCursor.moveToFirst()) {
        }

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String path = uri.getPath();
        if (mMedia.containsKey(path)) return null;

        if (projection == null) projection = ALL_COLUMNS;
        MatrixCursor result = new MatrixCursor(projection);

        return result;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
