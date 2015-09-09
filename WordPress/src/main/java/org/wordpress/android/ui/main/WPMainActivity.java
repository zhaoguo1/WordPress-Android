package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaAddFragment;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.SettingsFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.CoreEvents.MainViewPagerScrolled;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.CoreEvents.UserSignedOutWordPressCom;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;

import de.greenrobot.event.EventBus;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class WPMainActivity extends Activity
    implements MediaAddFragment.MediaAddFragmentCallback, Bucket.Listener<Note> {

    private WPViewPager mViewPager;
    private WPMainTabLayout mTabLayout;
    private WPMainTabAdapter mTabAdapter;
    private TextView mConnectionBar;

    private boolean mWasPaused;

    private final Handler mHandler = new Handler();

    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";
    private static final String KEY_WAS_PAUSED = "was_paused";

    private static final long QUARTER_SECOND_MS = 250L;
    private static final long TWO_SECONDS_MS    = 2000L;

    private final ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageSelected(final int position) {
            AppPrefs.setMainTabIndex(position);
            trackLastVisibleTab(position);

            if (position == WPMainTabAdapter.TAB_NOTIFS) {
                new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            // tell the masterbar fragment at this position that it just become active
            notifyMasterbarFragment(MasterbarFragmentEvent.FRAGMENT_ACTIVATED, position, 0);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // noop
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // fire event if the "My Site" page is being scrolled so the fragment can
            // animate its fab to match
            if (position == WPMainTabAdapter.TAB_MY_SITE) {
                EventBus.getDefault().post(new MainViewPagerScrolled(positionOffset));
            }
        }
    };

    private final TabLayout.OnTabSelectedListener mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            //  nop
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // scroll the active fragment to the top, if available
            BaseMasterbarFragment fragment = getMasterbarFragmentAtPosition(tab.getPosition());
            if (fragment != null) {
                fragment.onScrollToTop();
            }
        }
    };

    /*
     * notifies the newly-active fragment that it has been activated or resumed - takes care of
     * the situation where fragment hasn't been created yet by waiting using incremental backoff
     */
    private enum MasterbarFragmentEvent {
        FRAGMENT_ACTIVATED,
        FRAGMENT_RESUMED
    }
    private void notifyMasterbarFragment(final MasterbarFragmentEvent event,
                                         final int position,
                                         final int counter) {
        BaseMasterbarFragment fragment = getMasterbarFragmentAtPosition(position);
        if (fragment != null) {
            switch (event) {
                case FRAGMENT_ACTIVATED:
                    fragment.onMasterbarTabActivated();
                    break;
                case FRAGMENT_RESUMED:
                    fragment.onMasterbarTabResumed();
                    break;
            }
        } else if (counter < 3) {
            AppLog.i(T.MAIN, "main activity > waiting for fragment at position " + position + ", count=" + counter);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && position == getCurrentPosition()) {
                        notifyMasterbarFragment(event, position, counter + 1);
                    }
                }
            }, QUARTER_SECOND_MS);
        } else {
            AppLog.w(T.MAIN, "main activity > failed waiting for fragment at position " + position);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStatusBarColor();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mViewPager = (WPViewPager) findViewById(R.id.viewpager_main);
        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        mConnectionBar = (TextView) findViewById(R.id.connection_bar);
        mConnectionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // slide out the bar on click, then re-check connection after a brief delay
                AniUtils.animateBottomBar(mConnectionBar, false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            checkConnection();
                        }
                    }
                }, TWO_SECONDS_MS);
            }
        });

        mTabLayout = (WPMainTabLayout) findViewById(R.id.tab_layout);
        mTabLayout.createTabs();

        mTabLayout.setOnTabSelectedListener(mOnTabSelectedListener);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        if (savedInstanceState != null) {
            mWasPaused = savedInstanceState.getBoolean(KEY_WAS_PAUSED);
        } else if (AccountHelper.isSignedIn()) {
            // open note detail if activity called from a push, otherwise return to the tab
            // that was showing last time
            boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                    false));
            if (openedFromPush) {
                getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                launchWithNoteId();
            } else {
                int position = AppPrefs.getMainTabIndex();
                if (mTabAdapter.isValidPosition(position) && position != getCurrentPosition()) {
                    mViewPager.setCurrentItem(position);
                }
            }
        } else {
            ActivityLauncher.showSignInForResult(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAS_PAUSED, mWasPaused);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_tint));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(T.MAIN, "main activity > new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
    }

    /*
     * called when app is launched from a push notification, switches to the notification tab
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) return;

        mViewPager.setCurrentItem(WPMainTabAdapter.TAB_NOTIFS);

        // Check for push authorization request
        if (getIntent().hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            Bundle extras = getIntent().getExtras();
            String token = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, "");
            String title = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TITLE, "");
            String message = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, "");
            long expires = extras.getLong(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, 0);

            long now = System.currentTimeMillis() / 1000;
            if (expires > 0 && now > expires) {
                // Show a toast if the user took too long to open the notification
                ToastUtils.showToast(this, R.string.push_auth_expired, ToastUtils.Duration.LONG);
                AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
            } else {
                NotificationsUtils.showPushAuthAlert(this, token, title, message);
            }
        }

        boolean shouldShowKeyboard = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
        if (GCMIntentService.getNotificationsCount() == 1) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (!TextUtils.isEmpty(noteId)) {
                NotificationsListFragment.openNote(this, noteId, shouldShowKeyboard, false);
            }
        }

        GCMIntentService.clearNotifications();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mWasPaused = true;

        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start listening to Simperium Note bucket
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }
        mTabLayout.checkNoteBadge();

        checkConnection();

        if (mWasPaused) {
            mWasPaused = false;

            // We need to track the current item on the screen when this activity is resumed.
            // Ex: Notifications -> notifications detail -> back to notifications
            int position = getCurrentPosition();
            trackLastVisibleTab(position);

            // tell the active masterbar fragment that the main activity was resumed
            notifyMasterbarFragment(MasterbarFragmentEvent.FRAGMENT_RESUMED, position, 0);
        }
    }

    private void trackLastVisibleTab(int position) {
        switch (position) {
            case WPMainTabAdapter.TAB_MY_SITE:
                ActivityId.trackLastActivity(ActivityId.MY_SITE);
                AnalyticsTracker.track(AnalyticsTracker.Stat.MY_SITE_ACCESSED);
                break;
            case WPMainTabAdapter.TAB_READER:
                ActivityId.trackLastActivity(ActivityId.READER);
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);
                break;
            case WPMainTabAdapter.TAB_ME:
                ActivityId.trackLastActivity(ActivityId.ME);
                AnalyticsTracker.track(AnalyticsTracker.Stat.ME_ACCESSED);
                break;
            case WPMainTabAdapter.TAB_NOTIFS:
                ActivityId.trackLastActivity(ActivityId.NOTIFICATIONS);
                AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
                break;
            default:
                break;
        }
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    private void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated
        ReaderPostListFragment.resetLastUpdateDt();

        // remember the current tab position, then recreate the adapter so new fragments are created
        int position = getCurrentPosition();
        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        // restore previous position
        if (mTabAdapter.isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    private void moderateCommentOnActivityResult(Intent data) {
        try {
            if (SimperiumUtils.getNotesBucket() != null) {
                Note note = SimperiumUtils.getNotesBucket().get(StringUtils.notNullStr(data.getStringExtra
                        (NotificationsListFragment.NOTE_MODERATE_ID_EXTRA)));
                CommentStatus status = CommentStatus.fromString(data.getStringExtra(
                        NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA));
                NotificationsUtils.moderateCommentForNote(note, status, findViewById(R.id.root_view_main));
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    WordPress.registerForCloudMessaging(this);
                    resetFragments();
                } else if (!AccountHelper.isSignedIn()) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (resultCode == RESULT_OK && data != null) {
                    moderateCommentOnActivityResult(data);
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.BLOG_SETTINGS:
                if (resultCode == BlogPreferencesActivity.RESULT_BLOG_REMOVED) {
                    // user removed the current (self-hosted) blog from blog settings
                    if (!AccountHelper.isSignedIn()) {
                        ActivityLauncher.showSignInForResult(this);
                    } else {
                        MySiteFragment mySiteFragment = getMySiteFragment();
                        if (mySiteFragment != null) {
                            mySiteFragment.setBlog(WordPress.getCurrentBlog());
                        }
                    }
                }
                break;
            case RequestCodes.ACCOUNT_SETTINGS:
                if (resultCode == SettingsFragment.LANGUAGE_CHANGED) {
                    resetFragments();
                }
                break;
            case RequestCodes.CREATE_BLOG:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
        }
    }

    private int getCurrentPosition() {
        return mViewPager.getCurrentItem();
    }

    private BaseMasterbarFragment getMasterbarFragmentAtPosition(int position) {
        Fragment fragment = (mTabAdapter != null ? mTabAdapter.getFragment(position) : null);
        return (fragment instanceof BaseMasterbarFragment) ? (BaseMasterbarFragment) fragment : null;
    }

    /*
     * returns the my site fragment from the sites tab
     */
    private MySiteFragment getMySiteFragment() {
        Fragment fragment = (mTabAdapter != null ? mTabAdapter.getFragment(WPMainTabAdapter.TAB_MY_SITE) : null);
        if (fragment instanceof MySiteFragment) {
            return (MySiteFragment) fragment;
        }
        return null;
    }

    // Updates `last_seen` notifications flag in Simperium and removes tab indicator
    private class UpdateLastSeenTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return SimperiumUtils.updateLastSeenTime();
        }

        @Override
        protected void onPostExecute(Boolean lastSeenTimeUpdated) {
            if (isFinishing()) return;

            if (lastSeenTimeUpdated) {
                mTabLayout.showNoteBadge(false);
            }
        }
    }

    // Events

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutWordPressCom event) {
        resetFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutCompletely event) {
        ActivityLauncher.showSignInForResult(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidCredentialsDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.RestApiUnauthorized event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.TwoFactorAuthenticationDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidSslCertificateDetected event) {
        SelfSignedSSLCertsManager.askForSslTrust(this, null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.LoginLimitDetected event) {
        ToastUtils.showToast(this, R.string.limit_reached, ToastUtils.Duration.LONG);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        mTabLayout.checkNoteBadge();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        updateConnectionBar(event.isConnected());
    }

    private void checkConnection() {
        updateConnectionBar(NetworkUtils.isNetworkAvailable(this));
    }

    private void updateConnectionBar(boolean isConnected) {
        if (isConnected && mConnectionBar.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, false);
        } else if (!isConnected && mConnectionBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mConnectionBar, true);
        }
    }

    /*
     * Simperium Note bucket listeners
     */
    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String s) {
        if (changeType == Bucket.ChangeType.INSERT || changeType == Bucket.ChangeType.MODIFY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) return;

                    if (isViewingNotificationsTab()) {
                        new UpdateLastSeenTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        mTabLayout.checkNoteBadge();
                    }
                }
            });
        }
    }

    private boolean isViewingNotificationsTab() {
        return getCurrentPosition() == WPMainTabAdapter.TAB_NOTIFS;
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onMediaAdded(String mediaId) {
    }
}
