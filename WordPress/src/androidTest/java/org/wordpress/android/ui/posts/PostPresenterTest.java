package org.wordpress.android.ui.posts;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.WPNetworkImageView;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.test.AndroidTestCase;
import android.view.View;

import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PostPresenterTest extends AndroidTestCase {
    private PostsListContracts.PostView mPostView;
    private PostsListContracts.PostsActionHandler mPostsActionHandler;

    private int mDisplayWidth;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        setLocale("en", "EN");

        mDisplayWidth = 2000;

        mPostView = new PostsListContracts.PostView() {
            @Override
            public int getDisplayWidth() {
                return mDisplayWidth;
            }

            @Override
            public void editBlogPostOrPageForResult(long postOrPageId, boolean isPage) {}

            @Override
            public void publishPost(Post post) {}

            @Override
            public void browsePostOrPage(Blog blog, Post post) {}

            @Override
            public void viewPostPreviewForResult(Post post, boolean isPage) {}

            @Override
            public void viewStatsSinglePostDetails(Post post, boolean isPost) {}

            @Override
            public Context getContext() {
                return mContext;
            }

            @Override
            public String getString(@StringRes int stringRes) {
                return mContext.getString(stringRes);
            }

            @Override
            public CharSequence getText(@StringRes int textRes) {
                return mContext.getText(textRes);
            }

            @Override
            public void showToast(int stringResId) {}
        };

        mPostsActionHandler = new PostsListContracts.PostsActionHandler() {
            @Override
            public void onRefreshRequested() {}

            @Override
            public void onLoadMore() {}

            @Override
            public void onFabClick() {}

            @Override
            public void onTrashPost(PostsListPost postsListPost) {}
        };

    }

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // here we update locale for date formatters
        Locale.setDefault(locale);
        // here we update locale for app resources
        Resources res = getContext().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private PostsListPost createPostsListPost() {
        Post post = new Post(new Random().nextInt(), false);
        post.setLocalTablePostId(new Random().nextInt());
        post.setRemotePostId("" + new Random().nextInt());
        post.setFeaturedImageId(new Random().nextInt());
        post.setTitle("post" + new Random().nextInt());
        post.setDescription("descr" + new Random().nextInt());
        post.setPostExcerpt("excerpt" + new Random().nextInt());
        post.setPostStatus(PostStatus.toString(PostStatus.PUBLISHED));
        post.setLocalDraft(false);
        post.setLocalChange(false);
        post.setDate_created_gmt(new Date().getTime() - 10 * 60 * 60 * 1000);

        PostsListPost postsListPost = new PostsListPost(post);
        postsListPost.setFeaturedImageUrl("" + new Random().nextInt());

        return postsListPost;
    }

    public void testPresenter_PublishedPost() throws InterruptedException {
        // simulate wide display
        mDisplayWidth = 2000;

        PostsListPost postsListPost = createPostsListPost();

        PostPresenter postPresenter = new PostPresenter(mPostView, postsListPost, true, mPostsActionHandler);
        PostViewModel postViewModel = postPresenter.getViewModel();

        // check title
        assertEquals(postsListPost.getTitle(), postViewModel.title.get());

        // check excerpt
        assertEquals(postsListPost.getExcerpt(), postViewModel.excerpt.get());
        assertEquals(View.VISIBLE, postViewModel.excerptVisibility.get());

        // displayed post status should just be empty
        assertEquals("", postViewModel.statusText.get());

        assertEquals(postsListPost.getFeaturedImageUrl(), postViewModel.featuredImageUrl.get());
        assertEquals(View.VISIBLE, postViewModel.featuredImageVisibility.get());
        assertEquals(WPNetworkImageView.ImageType.PHOTO, postViewModel.featuredImageType.get());

        assertEquals("10h", postViewModel.formattedDate.get());
        assertEquals(View.VISIBLE, postViewModel.dateVisibility.get());

        assertTrue(postViewModel.showRow1.get());

        assertEquals(View.VISIBLE, postViewModel.trashButtonVisibility.get());
        assertEquals(PostListButton.BUTTON_TRASH, postViewModel.trashButtonType.get());

        assertEquals(View.VISIBLE, postViewModel.viewButtonVisibility.get());
        assertEquals(PostListButton.BUTTON_VIEW, postViewModel.viewButtonType.get());

        assertEquals(View.VISIBLE, postViewModel.editButtonVisibility.get());

        assertEquals(View.GONE, postViewModel.backButtonVisibility.get());

        assertEquals(View.VISIBLE, postViewModel.statsButtonVisibility.get());

        // simulate narrow display
        mDisplayWidth = 600;

        // make the presenter update the UI
        postPresenter.setPostsListPost(postsListPost);

        assertEquals(View.VISIBLE, postViewModel.moreButtonVisibility.get());
        assertEquals(View.GONE, postViewModel.backButtonVisibility.get());
        assertEquals(View.GONE, postViewModel.trashButtonVisibility.get());
        assertEquals(View.GONE, postViewModel.statsButtonVisibility.get());

        // simulated button "More" press
        postPresenter.onPostButtonClick(PostListButton.BUTTON_MORE);

        assertFalse(postViewModel.showRow1.get());
        assertTrue(postViewModel.animTriggered.isTriggered());
    }
}
