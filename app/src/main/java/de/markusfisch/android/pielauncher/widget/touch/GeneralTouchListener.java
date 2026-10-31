package de.markusfisch.android.pielauncher.widget.touch;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

public interface GeneralTouchListener {
    void performAction(View v, Point touch, TapFlags tapFlags);

    void postDelayed(Runnable r, long delayMillis);

    void removeCallbacks(Runnable r);

    void releaseIcon();

    void setTouch(Point touch);

    void invalidate();

    Context getContext();

    void editIconAt(Point touch);

    void updateEditablePie(Point touch);
}
