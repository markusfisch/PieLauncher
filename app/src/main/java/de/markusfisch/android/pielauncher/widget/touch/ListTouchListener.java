package de.markusfisch.android.pielauncher.widget.touch;

import android.content.Context;
import android.graphics.Point;

public interface ListTouchListener {
    int getScrollY();

    int getMaxScrollY();

    void scroll(int scrollY, boolean anim);

    void keepScrolling(int velocityY);

    void initLongPress(Context context, Point touch);

    void cancelLongPress();

    boolean isFadingIn(long eventTime);

    boolean isDraggingDownList();

    float getDragProgress();

    void onDragDown(float dragProgress);

    void cancelDragDownList();

    void stopFling();

    void initAppShortcutsScroll(Point touch);

    void scrollAppShortcuts(int delta);

    boolean isAppShortcutsTouched();

    void setAppShortcutsTouched(boolean touched);
}
