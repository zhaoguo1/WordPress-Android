package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.PostsListContracts.PageActionHandler;
import org.wordpress.android.ui.posts.adapters.PageMenuAdapter;
import org.wordpress.android.util.ObservableString;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.ObservableInt;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListPopupWindow;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PageView}.
 */
public class PageViewModel extends BasePostViewModel {
    public final ObservableString date = new ObservableString();
    public final ObservableInt dateHeaderVisibility = new ObservableInt();
    public final ObservableInt moreButtonVisibility = new ObservableInt();
    public final ObservableInt dividerTopVisibility = new ObservableInt();

    public final ObservableInt showPagePopupMenuTrigger = new ObservableInt();

    // counters used for triggering the animation of the buttons row
    private Integer mDoneShowPagePopupMenuCounter = 0;

    public PageActionHandler getActionHandler() {
        return (PageActionHandler) getBasePostPresenter();
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    @BindingAdapter({"showPagePopupMenuTrigger", "pageViewModel"})
    public static void onShowPagePopupMenu(ImageView imageView, final int showPagePopupMenuTrigger, final
    PageViewModel pageViewModel) {
        if (pageViewModel.mDoneShowPagePopupMenuCounter == showPagePopupMenuTrigger) {
            return;
        }

        pageViewModel.mDoneShowPagePopupMenuCounter = showPagePopupMenuTrigger;

        Context context = imageView.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(imageView);

        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setModal(true);
        listPopup.setAdapter(new PageMenuAdapter(context, pageViewModel.getBasePostPresenter().getPostsListPost()));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopup.dismiss();
                if (pageViewModel.getActionHandler() != null) {
                    pageViewModel.getActionHandler().onPageButtonClick((int) id);
                }
            }
        });
        listPopup.show();
    }

    public void showPagePopupMenu() {
        showPagePopupMenuTrigger.set(showPagePopupMenuTrigger.get() + 1);
    }
}
