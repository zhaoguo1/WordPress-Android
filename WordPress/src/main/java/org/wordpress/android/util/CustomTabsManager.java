package org.wordpress.android.util;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
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

    private CustomTabsSession mSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;

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
        if (mClient != null) return;
        if (!isCustomTabsSupported(context)) return;

        String packageNameToBind = CustomTabsHelper.getPackageNameToUse(context);
        if (packageNameToBind == null) return;

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mClient = client;
                mClient.warmup(0);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
            }
        };

        boolean isConnected = CustomTabsClient.bindCustomTabsService(context, packageNameToBind, mConnection);
        if (!isConnected) {
            mConnection = null;
        }
    }

    private CustomTabsSession getSession() {
        if (mClient == null) {
            mSession = null;
        } else if (mSession == null) {
            mSession = mClient.newSession(new CustomTabsCallback() {
                @Override
                public void onNavigationEvent(int navigationEvent, Bundle extras) {
                    AppLog.d(AppLog.T.UTILS, "chrome onNavigationEvent > Code = " + navigationEvent);
                }
            });
        }
        return mSession;
    }

    public void unbindCustomTabsService(Context context) {
        if (mConnection == null) return;

        context.unbindService(mConnection);

        mClient = null;
        mSession = null;
        mConnection = null;
    }

    public void mayLaunchUrl(String url) {
        CustomTabsSession session = getSession();
        if (session != null) {
            session.mayLaunchUrl(Uri.parse(url), null, null);
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
            // note that it's okay for session to be null here
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getSession());
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
