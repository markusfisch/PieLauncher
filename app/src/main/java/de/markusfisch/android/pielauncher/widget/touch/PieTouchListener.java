package de.markusfisch.android.pielauncher.widget.touch;

import android.graphics.Point;

public interface PieTouchListener {
    void openMenu(long eventTime);

    void startSpin();

    void spin(double twist);

    void stopSpin();

    double getPieTwist();

    Point getPieCenter();

    boolean inDeadZone(Point touch);

    boolean expandPanelInDeadZone();

    void fadeOutMode();

    void evaluateSelectedIcon(Point touch);
}
