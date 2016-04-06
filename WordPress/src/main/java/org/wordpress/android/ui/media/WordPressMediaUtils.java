package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.helpers.Version;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.IOException;

import static org.wordpress.android.WordPressDB.*;

public class WordPressMediaUtils {
    public interface LaunchCameraCallback {
        void onMediaCapturePathReady(String mediaCapturePath);
    }

    public static int getPlaceholder(String url) {
        if (MediaUtils.isValidImage(url)) {
            return R.drawable.media_image_placeholder;
        } else if (MediaUtils.isDocument(url)) {
            return R.drawable.media_document;
        } else if (MediaUtils.isPowerpoint(url)) {
            return R.drawable.media_powerpoint;
        } else if (MediaUtils.isSpreadsheet(url)) {
            return R.drawable.media_spreadsheet;
        } else if (MediaUtils.isVideo(url)) {
            return org.wordpress.android.editor.R.drawable.media_movieclip;
        } else if (MediaUtils.isAudio(url)) {
            return R.drawable.media_audio;
        } else {
            return 0;
        }
    }

    /**
     * Given a media file cursor, returns the thumbnail network URL. Will use photon if available,
     * using the specified width.
     *
     * @param width width to use for photon request (if applicable)
     */
    public static String getNetworkThumbnailUrl(Cursor cursor, int width) {
        String thumbnailURL = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_THUMBNAIL_URL));

        // Allow non-private wp.com and Jetpack blogs to use photon to get a higher res thumbnail
        if ((WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable())) {
            String imageURL = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FILE_URL));
            if (imageURL != null) {
                thumbnailURL = PhotonUtils.getPhotonImageUrl(imageURL, width, 0);
            }
        }

        return thumbnailURL;
    }

    /**
     * Returns a poster (thumbnail) URL given a VideoPress video URL
     */
    public static String getVideoPressVideoPosterFromURL(String videoUrl) {
        String posterUrl = "";

        if (videoUrl != null) {
            int filetypeLocation = videoUrl.lastIndexOf(".");
            if (filetypeLocation > 0) {
                posterUrl = videoUrl.substring(0, filetypeLocation) + "_std.original.jpg";
            }
        }
        return posterUrl;
    }

    /**
     * Loads the given network image URL into the {@link NetworkImageView}, using the default
     * {@link ImageLoader} (via {@link WordPress#imageLoader}.
     */
    public static void loadNetworkImage(String url, NetworkImageView view) {
        loadNetworkImage(url, view, WordPress.imageLoader);
    }

    /**
     * Loads the given network image URL into the {@link NetworkImageView}.
     */
    public static void loadNetworkImage(String url, NetworkImageView view, ImageLoader loader) {
        if (url != null) {
            Uri uri = Uri.parse(url);
            String filepath = uri.getLastPathSegment();

            int placeholderResId = WordPressMediaUtils.getPlaceholder(filepath);
            view.setErrorImageResId(placeholderResId);

            // no default image while downloading
            view.setDefaultImageResId(0);

            if (MediaUtils.isValidImage(filepath)) {
                view.setTag(url);
                view.setImageUrl(url, loader);
            } else {
                view.setImageResource(placeholderResId);
            }
        } else {
            view.setImageResource(0);
        }
    }

    public static void launchPictureLibrary(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(preparePictureLibraryIntent(activity),
                RequestCodes.PICTURE_LIBRARY);
    }

    public static void launchPictureLibrary(Fragment fragment) {
        if (!fragment.isAdded()) return;
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(preparePictureLibraryIntent(fragment.getActivity()),
                RequestCodes.PICTURE_LIBRARY);
    }

    public static void launchVideoLibrary(Fragment fragment) {
        if (!fragment.isAdded()) return;
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(prepareVideoLibraryIntent(fragment.getActivity()),
                RequestCodes.VIDEO_LIBRARY);
    }

    public static void launchCamera(Activity activity, LaunchCameraCallback callback) {
        Intent intent = prepareLaunchCamera(activity, callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    public static void launchCamera(Fragment fragment, LaunchCameraCallback callback) {
        if (!fragment.isAdded()) return;
        Intent intent = prepareLaunchCamera(fragment.getActivity(), callback);
        if (intent != null) {
            AppLockManager.getInstance().setExtendedTimeout();
            fragment.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    public static void launchVideoCamera(Activity activity) {
        AppLockManager.getInstance().setExtendedTimeout();
        activity.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    public static void launchVideoCamera(Fragment fragment) {
        if (!fragment.isAdded()) return;
        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(prepareVideoCameraIntent(), RequestCodes.TAKE_VIDEO);
    }

    private static Intent getLaunchCameraIntent(LaunchCameraCallback callback) {
        File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String mediaFile = "Camera" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
        String mediaCapturePath = dcimDir + File.separator + mediaFile;
        Uri mediaCaptureUri = Uri.fromFile(new File(mediaCapturePath));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mediaCaptureUri);
        
        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException("Path to file could not be created.");
            } catch (IOException e) {
                AppLog.e(T.POSTS, e);
            }
        }

        if (callback != null) callback.onMediaCapturePathReady(mediaCapturePath);
        return intent;
    }

    private static Intent prepareVideoLibraryIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return Intent.createChooser(intent, context.getString(R.string.pick_video));
    }

    private static Intent prepareVideoCameraIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    private static Intent preparePictureLibraryIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return Intent.createChooser(intent, context.getString(R.string.pick_photo));
    }

    private static Intent prepareLaunchCamera(Context context, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            return getLaunchCameraIntent(callback);
        }
    }

    private static void showSDCardRequiredDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(context.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(context.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(context.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    /**
     * This is a workaround for WP3.4.2 that deletes the media from the server when editing
     * media properties within the app.
     * See: https://github.com/wordpress-mobile/WordPress-Android/issues/204
     */
    public static boolean isWordPressVersionWithMediaEditingCapabilities() {
        if (WordPress.currentBlog == null) {
            return false;
        }

        if (WordPress.currentBlog.getWpVersion() == null) {
            return true;
        }

        if (WordPress.currentBlog.isDotcomFlag()) {
            return true;
        }

        Version minVersion;
        Version currentVersion;
        try {
            minVersion = new Version("3.5.2");
            currentVersion = new Version(WordPress.currentBlog.getWpVersion());

            if (currentVersion.compareTo(minVersion) == -1) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            AppLog.e(T.POSTS, e);
        }

        return true;
    }

    public static boolean canDeleteMedia(String blogId, String mediaID) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaID);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        cursor.close();
        return state == null || !state.equals("uploading");
    }
}
