package org.wordpress.android.ui.posts;

import org.wordpress.android.util.DataBindingUtils.AnimationTrigger;
import org.wordpress.android.util.ObservableString;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

/**
 * Exposes the data to be used in the {@link PostsListContracts.PostView}.
 */
public class PostViewModel extends BasePostViewModel {
    private static final long ROW_ANIM_DURATION = 150;

    public final ObservableString excerpt = new ObservableString();
    public final ObservableInt excerptVisibility = new ObservableInt(View.GONE);
    public final ObservableString featuredImageUrl = new ObservableString(null);
    public final ObservableInt featuredImageVisibility = new ObservableInt(View.GONE);
    public final ObservableField<WPNetworkImageView.ImageType> featuredImageType = new ObservableField<>
            (WPNetworkImageView.ImageType.PHOTO);
    public final ObservableString formattedDate = new ObservableString(null);
    public final ObservableInt dateVisibility = new ObservableInt(View.GONE);
    public final ObservableInt trashButtonType = new ObservableInt();
    public final ObservableInt viewButtonType = new ObservableInt();

    // edit / view are always visible
    public final ObservableInt viewButtonVisibility = new ObservableInt(View.VISIBLE);
    public final ObservableInt editButtonVisibility = new ObservableInt(View.VISIBLE);

    public final ObservableInt publishButtonVisibility = new ObservableInt(View.VISIBLE);
    public final ObservableInt moreButtonVisibility = new ObservableInt(View.VISIBLE);
    public final ObservableInt backButtonVisibility = new ObservableInt(View.GONE);
    public final ObservableInt trashButtonVisibility = new ObservableInt(View.GONE);
    public final ObservableInt statsButtonVisibility = new ObservableInt(View.GONE);

    public final ObservableInt showRow = new ObservableInt();
    public final ObservableBoolean canShowStats = new ObservableBoolean();
    public final ObservableBoolean canPublish = new ObservableBoolean();

    public AnimationTrigger animTriggered = new AnimationTrigger();

    @BindingAdapter({"showRow", "canShowStats", "canPublish", "postViewModel", "animTriggered"})
    public static void onAnimateButtonRow(final LinearLayout layoutButtons, final int showRow, final boolean
            canShowStats, final boolean canPublish, final PostViewModel postViewModel, AnimationTrigger animTriggered) {
        if(!animTriggered.isTriggered()) {
            return;
        }

        animTriggered.clearTrigger();

        /*
         * buttons may appear in two rows depending on display size and number of visible
         * buttons - these rows are toggled through the "more" and "back" buttons - this
         * routine is used to animate the new row in and the old row out
         */

        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f);
        ObjectAnimator animOut = ObjectAnimator.ofPropertyValuesHolder(layoutButtons, scaleX, scaleY);
        animOut.setDuration(ROW_ANIM_DURATION);
        animOut.setInterpolator(new AccelerateInterpolator());

        animOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // row 0
                postViewModel.editButtonVisibility.set(showRow == 0 ? View.VISIBLE : View.GONE);
                postViewModel.viewButtonVisibility.set(showRow == 0 ? View.VISIBLE : View.GONE);

                final boolean willShowMoreButton = showRow == 0 || showRow == 1 && canShowStats && canPublish;
                postViewModel.moreButtonVisibility.set(willShowMoreButton ? View.VISIBLE : View.GONE);

                // row 1
                postViewModel.statsButtonVisibility.set(showRow == 1 && canShowStats ? View.VISIBLE : View.GONE);
                postViewModel.trashButtonVisibility.set(showRow == 1 ? View.VISIBLE : View.GONE);
                postViewModel.backButtonVisibility.set(!willShowMoreButton ? View.VISIBLE : View.GONE);

                // row 2
                postViewModel.publishButtonVisibility.set(
                        (showRow == 1 && !canShowStats || showRow == 2) && canPublish ? View.VISIBLE : View.GONE);

                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f);
                ObjectAnimator animIn = ObjectAnimator.ofPropertyValuesHolder(layoutButtons, scaleX, scaleY);
                animIn.setDuration(ROW_ANIM_DURATION);
                animIn.setInterpolator(new DecelerateInterpolator());
                animIn.start();
            }
        });

        animOut.start();
    }

    public void animateButtonRows(final int showRow, boolean canShowStats, boolean canPublish) {
        this.showRow.set(showRow);
        this.canShowStats.set(canShowStats);
        this.canPublish.set(canPublish);

        // trigger the animation
        animTriggered.trigger();
    }
}
