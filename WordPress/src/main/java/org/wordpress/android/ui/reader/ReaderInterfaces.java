package org.wordpress.android.ui.reader;

import android.view.View;

import org.wordpress.android.models.ReaderPost;

public class ReaderInterfaces {

    public interface OnPostSelectedListener {
        public void onPostSelected(ReaderPost post);
    }

    public interface OnTagSelectedListener {
        public void onTagSelected(String tagName);
    }

    /*
     * called from post detail fragment when user clicks a link
     */
    public interface OnReaderUrlClickedListener {
        public void onReaderUrlClicked(String url);
    }

    /*
     * called from post detail fragment so toolbar can animate in/out when scrolling
     */
    public interface AutoHideToolbarListener {
        public void onShowHideToolbar(boolean show);
    }

    /*
     * called when user taps the dropdown arrow next to a post to show the popup menu
     */
    public interface OnPostPopupListener {
        public void onShowPostPopup(View view, ReaderPost post);
    }

    /*
     * used by adapters to notify when data has been loaded
     */
    public interface DataLoadedListener {
        public void onDataLoaded(boolean isEmpty);
    }

}
