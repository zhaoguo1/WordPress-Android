package org.wordpress.android.util;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.passcodelock.AppLockManager;

/**
 * enables simplified handling of Chrome custom tabs in any activity
 *
 *  1. create CustomTabsManager instance in onCreate()
 *  2. call bindCustomTabService() in onResume()
 *  3. call unbindCustomTabService() in onPause()
 *  4. call mayLaunchUrl() to provide hints on urls the user may load
 *  5. call openUrl() to display the page
 *
 * activities which don't need the benefits on mayLaunchUrl() can skip all that
 * and simply call the static browseUrl() method
 */
public class CustomTabsManager {

    public enum OpenUrlType {
        // show pages in-app even if custom tabs aren't supported
        INTERNAL,
        // show pages in-app only if custom tabs are supported (external browser otherwise)
        INTERNAL_IF_CUSTOM_TABS_SUPPORTED,
        // show pages in external browser
        EXTERNAL
    }

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mCustomTabsClient;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;

    public static CustomTabsManager newInstance() {
        return new CustomTabsManager();
    }

    public static void browseUrl(Context context, String url) {
        newInstance().openUrl(context, url);
    }

    public static void browseUrl(Context context, String url, OpenUrlType openUrlType) {
        newInstance().openUrl(context, url, openUrlType);
    }

    private CustomTabsManager() {
        // noop for now
    }

    public void bindCustomTabsService(Context context) {
        if (mCustomTabsClient != null) return;
        if (!isCustomTabsSupported(context)) return;

        String packageNameToBind = CustomTabsHelper.getPackageNameToUse(context);
        if (packageNameToBind == null) return;

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mCustomTabsClient = client;
                mCustomTabsClient.warmup(0);
                mCustomTabsSession = mCustomTabsClient.newSession(null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCustomTabsClient = null;
            }
        };

        boolean isConnected = CustomTabsClient.bindCustomTabsService(context, packageNameToBind, mCustomTabsServiceConnection);
        if (!isConnected) {
            mCustomTabsServiceConnection = null;
        }
    }

    public void unbindCustomTabsService(Context context) {
        if (mCustomTabsServiceConnection == null) return;

        context.unbindService(mCustomTabsServiceConnection);

        mCustomTabsClient = null;
        mCustomTabsSession = null;
        mCustomTabsServiceConnection = null;
    }

    public void mayLaunchUrl(String url) {
        if (mCustomTabsSession != null) {
            mCustomTabsSession.mayLaunchUrl(Uri.parse(url), null, null);
        }
    }

    public void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }

    public void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (context == null || TextUtils.isEmpty(url)) return;

        if (openUrlType == OpenUrlType.EXTERNAL) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
                AppLockManager.getInstance().setExtendedTimeout();
            } catch (ActivityNotFoundException e) {
                String errString = context.getString(org.wordpress.android.R.string.reader_toast_err_url_intent);
                ToastUtils.showToast(context, String.format(errString, url), ToastUtils.Duration.LONG);
            }
        } else if (isCustomTabsSupported(context) && context instanceof Activity) {
            // note that it's okay for mCustomTabsSession to be null here
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(mCustomTabsSession);
            builder.setShowTitle(true);
            builder.setStartAnimations(context, R.anim.activity_slide_in_from_right, R.anim.do_nothing);
            builder.setExitAnimations(context, R.anim.do_nothing, R.anim.activity_slide_out_to_right);
            builder.setCloseButtonIcon(
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_arrow_back_black_24dp));
            CustomTabsIntent customTabsIntent = builder.build();
            CustomTabsHelper.addKeepAliveExtra(context, customTabsIntent.intent);
            // instanceof check in else statement was so we could safely pass an activity here
            customTabsIntent.launchUrl((Activity) context, Uri.parse(url));
        } else {
            WPWebViewActivity.openURL(context, url);
        }
    }

    /*
     * test whether Chrome custom tabs are supported by attempting to bind to the service
     */
    private static Boolean mIsCustomTabsSupported;
    private static boolean isCustomTabsSupported(Context context) {
        if (mIsCustomTabsSupported == null) {
            String packageNameToBind = CustomTabsHelper.getPackageNameToUse(context);
            if (packageNameToBind == null) {
                mIsCustomTabsSupported = false;
                return false;
            }

            CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            mIsCustomTabsSupported = CustomTabsClient.bindCustomTabsService(context, packageNameToBind, connection);
            if (mIsCustomTabsSupported) {
                context.unbindService(connection);
            }
        }

        return mIsCustomTabsSupported;
    }
}
