package org.wordpress.android.ui.main;

import android.app.Fragment;

import org.wordpress.android.util.AppLog;

/*
 * base class for all "masterbar" fragments - ie: fragments hosted in the main activity ViewPager
 */
public class BaseMasterbarFragment extends Fragment {

    /*
     * returns true if this fragment is hosted in the main activity, false if it's hosted
     * by some other activity - note that currently only ReaderPostListFragment is ever
     * hosted in an activity other than the main one
     */
    public boolean isHostedInMasterbar() {
        return (getActivity() instanceof WPMainActivity);
    }

    /*
     * called by the main activity when the user switches to the tab containing this fragment
     */
    public void onMasterbarTabActivated() {
        AppLog.d(AppLog.T.MAIN, "onMasterbarTabActivated " + this.getClass().getSimpleName());
    }

    /*
     * called by main activity when this is the active masterbar fragment and the main
     * activity is resumed after being paused - descendants should override this and
     * perform any operations necessary after returning from another activity
     */
    public void onMasterbarTabResumed() {
        AppLog.d(AppLog.T.MAIN, "onMasterbarTabResumed " + this.getClass().getSimpleName());
    }

    /*
     * called when user taps the active tab, descendants should override this and scroll
     * their contents to the top
     */
    public void onScrollToTop() {
        AppLog.d(AppLog.T.MAIN, "onScrollToTop " + this.getClass().getSimpleName());
    }
}