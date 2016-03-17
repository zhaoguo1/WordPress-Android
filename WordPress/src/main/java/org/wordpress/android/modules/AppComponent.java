package org.wordpress.android.modules;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.GCMRegistrationIntentService;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.WPDelayedHurlStack;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.module.ReleaseBaseModule;
import org.wordpress.android.stores.module.ReleaseNetworkModule;
import org.wordpress.android.stores.module.ReleaseStoreModule;
import org.wordpress.android.ui.DeepLinkingIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.main.MeFragment;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureActivity;
import org.wordpress.android.ui.stats.StatsWidgetProvider;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface AppComponent {
    void inject(WordPress application);
    void inject(SignInFragment object);
    void inject(WPMainActivity object);
    void inject(ShareIntentReceiverActivity object);
    void inject(BlogPreferencesActivity object);
    void inject(SiteSettingsFragment object);
    void inject(AccountSettingsFragment object);
    void inject(StatsWidgetConfigureActivity object);
    void inject(StatsWidgetProvider object);
    void inject(StatsActivity object);
    void inject(GCMMessageService object);
    void inject(GCMRegistrationIntentService object);
    void inject(DeepLinkingIntentReceiverActivity object);
    void inject(CommentDetailFragment object);
    void inject(MeFragment object);
    void inject(SitePickerActivity object);
    void inject(MySiteFragment object);

    // WPDelayedHurlStack will burn in hell as soon as we have all the stores ready
    void inject(WPDelayedHurlStack object);
}
