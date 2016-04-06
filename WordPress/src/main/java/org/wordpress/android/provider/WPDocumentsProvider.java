package org.wordpress.android.provider;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.DeviceUtils;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.wordpress.android.provider.ProviderConstants.*;

import static android.provider.DocumentsContract.Root;
import static android.provider.DocumentsContract.Document;

/**
 * {@link DocumentsProvider} that allows WordPress media library items to be chosen
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WPDocumentsProvider extends DocumentsProvider {
    /** @see DocumentsContract.Root#COLUMN_MIME_TYPES */
    public static final String SUPPORTED_MIME_TYPES = MIME_TYPE_IMAGE + "\n" + MIME_TYPE_VIDEO;

    public static final String IMAGE_CAPTURE_ID = "wpImages";
    public static final String VIDEO_CAPTURE_ID = "wpVideo";

    private static final String CAPTURE_ROOT_ID = "wpCaptureRoot";
    private static final String WP_ROOT_ID = "wpRoot";

    private static final String[] ALL_ROOT_COLUMNS = {
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_MIME_TYPES
    };
    private static final String[] ALL_DOC_COLUMNS = {
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_SUMMARY,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_ICON,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    //
    // Root and Root Document information for Capture and WordPress library roots
    //
    private static final Object[] CAPTURE_ROOT = { CAPTURE_ROOT_ID, 0,
            R.drawable.media_image_placeholder, "Camera", "Capture media", "caproot:", 0L, SUPPORTED_MIME_TYPES };
    private static final Object[] WP_ROOT = { WP_ROOT_ID, 0, R.mipmap.app_icon,
            "WordPress", "Media Library", "wproot:", 0L, SUPPORTED_MIME_TYPES};
    private static final Object[] CAPTURE_ROOT_DOC = { CAPTURE_ROOT[5], Document.MIME_TYPE_DIR, "CapRoot",
            "Capture new media", null, R.drawable.media_image_placeholder, 0, 0L };
    private static final Object[] WP_ROOT_DOC = { WP_ROOT[5], Document.MIME_TYPE_DIR, "WPRoot",
            "WordPress media", null, R.mipmap.app_icon, 0, 0L };

    //
    // Document information for Capture documents and WordPress library filter documents
    //
    private static final Object[] CAPTURE_IMAGE_DOC = { IMAGE_CAPTURE_ID, MIME_TYPE_IMAGE, "Photo",
            "Take a picture", null, R.drawable.ic_action_camera, 0, 0L };
    private static final Object[] CAPTURE_VIDEO_DOC = { VIDEO_CAPTURE_ID, MIME_TYPE_VIDEO, "Video",
            "Record a video", null, R.drawable.ic_action_video, 0, 0L };
    private static final Object[] WP_ALL_DIR_DOC = { "wpAllMedia", Document.MIME_TYPE_DIR, "All",
            "All WordPress media", null, R.drawable.media_image_placeholder, 0, 0L };
    private static final Object[] WP_IMAGE_DIR_DOC = { "wpImages", Document.MIME_TYPE_DIR, "Images",
            "WordPress images", null, R.drawable.media_image_placeholder, 0, 0L };
    private static final Object[] WP_VIDEO_DIR_DOC = { "wpVideo", Document.MIME_TYPE_DIR, "Video",
            "WordPress videos", null, R.drawable.media_image_placeholder, 0, 0L };

    private final Map<String, String> mDocumentIdToThumbnail = new HashMap<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] columns)
            throws FileNotFoundException {
        if (columns == null) columns = ALL_ROOT_COLUMNS;
        return addRoots(new MatrixCursor(columns));
    }

    @Override
    public Cursor queryDocument(String docId, String[] columns)
            throws FileNotFoundException {
        if (TextUtils.isEmpty(docId)) throw new FileNotFoundException();
        if (columns == null) columns = ALL_DOC_COLUMNS;
        MatrixCursor result = new MatrixCursor(columns);
        if (docId.equals(CAPTURE_ROOT[5])) {
            addRowData(result.newRow(), ALL_DOC_COLUMNS, CAPTURE_ROOT_DOC);
        } else if (docId.equals(WP_ROOT[5])) {
            addRowData(result.newRow(), ALL_DOC_COLUMNS, WP_ROOT_DOC);
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentId, String[] columns, String sortOrder)
            throws FileNotFoundException {
        if (columns == null) columns = ALL_DOC_COLUMNS;

        MatrixCursor result = new MatrixCursor(columns);
        if (parentId.equals(CAPTURE_ROOT_DOC[0])) {
            addRowData(result.newRow(), ALL_DOC_COLUMNS, CAPTURE_IMAGE_DOC);
            addRowData(result.newRow(), ALL_DOC_COLUMNS, CAPTURE_VIDEO_DOC);
        } else if (parentId.equals(WP_ROOT_DOC[0])) {
            addRowData(result.newRow(), ALL_DOC_COLUMNS, WP_ALL_DIR_DOC);
            addRowData(result.newRow(), ALL_DOC_COLUMNS, WP_IMAGE_DIR_DOC);
            addRowData(result.newRow(), ALL_DOC_COLUMNS, WP_VIDEO_DIR_DOC);
        } else if (parentId.equals(WP_ALL_DIR_DOC[0])) {
            addAllWordPressMedia(result);
        } else if (parentId.equals(WP_IMAGE_DIR_DOC[0])) {
            addWordPressImages(result);
        } else if (parentId.equals(WP_VIDEO_DIR_DOC[0])) {
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
        File dir = new File(getContext().getFilesDir() + "/WordPress/thumbnails");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File file = new File(dir, docId + ".jpg");
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

    private MatrixCursor addRoots(@NonNull MatrixCursor cursor) {
        if (getContext() == null) return cursor;
        // add items to capture new images/video if device is capable
        if (DeviceUtils.getInstance().hasCamera(getContext())) {
            addRowData(cursor.newRow(), ALL_ROOT_COLUMNS, CAPTURE_ROOT);
        }
        // add WordPress media if user is signed into a .com account
        if (AccountHelper.isSignedInWordPressDotCom()) {
            addRowData(cursor.newRow(), ALL_ROOT_COLUMNS, WP_ROOT);
        }
        return cursor;
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
            addRowData(rowParent.newRow(), ALL_DOC_COLUMNS, imageData);
        }
    }

    private boolean verifyLocalImage(int id, String url, String path) {
        return id >= 0 && !(TextUtils.isEmpty(url) && TextUtils.isEmpty(path));
    }

    private void addRowData(MatrixCursor.RowBuilder row, String[] columns, Object[] data) {
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
