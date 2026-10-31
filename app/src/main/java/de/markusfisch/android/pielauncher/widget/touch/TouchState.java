package de.markusfisch.android.pielauncher.widget.touch;

import android.view.MotionEvent;
import android.view.View;

public interface TouchState {
    void onPointerDown(View v, MotionEvent event, TouchController controller);

    void onPointerUp(View v, MotionEvent event, TouchController controller);

    boolean onDown(View v, MotionEvent event, TouchController controller);

    void onMove(View v, MotionEvent event, TouchController controller);

    void onUp(View v, MotionEvent event, TouchController controller);

    void onCancel(View v, MotionEvent event, TouchController controller);
}
