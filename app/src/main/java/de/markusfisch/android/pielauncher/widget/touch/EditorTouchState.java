package de.markusfisch.android.pielauncher.widget.touch;

import android.view.MotionEvent;
import android.view.View;

public class EditorTouchState implements TouchState {
    private final GeneralTouchListener generalListener;

    public EditorTouchState(GeneralTouchListener generalListener) {
        this.generalListener = generalListener;
    }

    @Override
    public void onPointerDown(View v, MotionEvent event, TouchController controller) {
    }

    @Override
    public void onPointerUp(View v, MotionEvent event, TouchController controller) {
    }

    @Override
    public boolean onDown(View v, MotionEvent event, TouchController controller) {
        generalListener.editIconAt(controller.getTouch());
        generalListener.updateEditablePie(controller.getTouch());
        controller.getGeneralListener().invalidate();
        return true;
    }

    @Override
    public void onMove(View v, MotionEvent event, TouchController controller) {
        generalListener.updateEditablePie(controller.getTouch());
        generalListener.invalidate();
    }

    @Override
    public void onUp(View v, MotionEvent event, TouchController controller) {
    }

    @Override
    public void onCancel(View v, MotionEvent event, TouchController controller) {
    }
}
