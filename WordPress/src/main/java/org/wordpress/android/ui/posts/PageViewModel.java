package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.PostsListContracts.PageActionHandler;
import org.wordpress.android.ui.posts.adapters.PageMenuAdapter;
import org.wordpress.android.util.ObservableString;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
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
    public final ObservableField<ShowPagePopupMenuConfig> showPagePopupMenuConfig = new ObservableField<>();

    private ShowPagePopupMenuConfig mOldShowPagePopupMenuConfig;

    public static class ShowPagePopupMenuConfig {
        private final PostsListPost mPostsListPost;

        public ShowPagePopupMenuConfig(PostsListPost postsListPost) {
            mPostsListPost = postsListPost;
        }
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    @BindingAdapter({"showPagePopupMenuConfig", "pageViewModel", "pageActionHandler"})
    public static void onShowPagePopupMenu(ImageView imageView, final ShowPagePopupMenuConfig
            showPagePopupMenuConfig, final PageViewModel pageViewModel, final PageActionHandler pageActionHandler) {
        if (pageViewModel.mOldShowPagePopupMenuConfig == showPagePopupMenuConfig) {
            return;
        }

        pageViewModel.mOldShowPagePopupMenuConfig = showPagePopupMenuConfig;

        Context context = imageView.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(imageView);

        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setModal(true);
        listPopup.setAdapter(new PageMenuAdapter(context, showPagePopupMenuConfig.mPostsListPost));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopup.dismiss();
                if (pageActionHandler != null) {
                    pageActionHandler.onPageButtonClick((int) id);
                }
            }
        });
        listPopup.show();
    }

    public void showPagePopupMenu(PostsListPost postsListPost) {
        // trigger the popup menu by changing the observable;
        showPagePopupMenuConfig.set(new ShowPagePopupMenuConfig(postsListPost));
    }
}
