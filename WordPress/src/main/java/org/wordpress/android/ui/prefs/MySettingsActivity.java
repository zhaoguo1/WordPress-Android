package org.wordpress.android.ui.prefs;

import org.wordpress.android.R;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.AppCompatPreferenceActivity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MySettingsActivity extends AppCompatPreferenceActivity {

    List<Header> mHeaders;

    private void buildHeaders() {
        final boolean signedInWPcom = AccountHelper.isSignedInWordPressDotCom();

        mHeaders = new ArrayList<>();

        if (signedInWPcom) {
            Header header = new Header();
            header.titleRes = R.string.account_settings;
            header.iconRes = R.drawable.me_icon_account_settings;
            header.fragment = "org.wordpress.android.ui.prefs.AccountSettingsFragment";
            mHeaders.add(header);
        }

        Header header = new Header();
        header.titleRes = R.string.me_btn_app_settings;
        header.iconRes = R.drawable.me_icon_app_settings;
        header.fragment = "org.wordpress.android.ui.prefs.AppSettingsFragment";
        mHeaders.add(header);

        if (signedInWPcom) {
            header = new Header();
            header.titleRes = R.string.notification_settings;
            header.iconRes = R.drawable.me_icon_notifications;
            header.fragment = "org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment";
            mHeaders.add(header);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        if (mHeaders == null) {
            buildHeaders();
        }

        target.addAll(mHeaders);
    }

    @Override
    protected boolean isValidFragment (String fragmentName) {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        buildHeaders();

        getIntent().putExtra(PreferenceActivity.EXTRA_NO_HEADERS, !onIsMultiPane() || (mHeaders.size() == 1));

        super.onCreate(savedInstanceState);

        if (onIsHidingHeaders()) {
            String singleFragmentName = getIntent().getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
            if (singleFragmentName != null && singleFragmentName.length() > 0) {
                for (Header header : mHeaders) {
                    if (header.fragment.equals(singleFragmentName)) {
                        setTitle(header.titleRes);
                        break;
                    }
                }
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (NetworkUtils.isNetworkAvailable(this)) {
            AccountHelper.getDefaultAccount().fetchAccountSettings();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
