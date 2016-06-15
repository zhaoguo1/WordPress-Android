package org.wordpress.android.ui;

public interface BasePresenter {
    void init();

    void willBeFirstStart();

    void start();

    void stop();
}
