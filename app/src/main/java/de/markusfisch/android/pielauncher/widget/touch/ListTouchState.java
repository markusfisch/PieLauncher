package de.markusfisch.android.pielauncher.widget.touch;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

public class ListTouchState implements TouchState {
    private final ListTouchListener listener;

    private VelocityTracker velocityTracker;
    private int scrollOffset;

    public ListTouchState(ListTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPointerDown(View v, MotionEvent event, TouchController controller) {
        listener.setAppShortcutsTouched(false);
        listener.cancelLongPress();
        controller.addTouch(event);
        initScroll(event, controller);
    }

    @Override
    public void onPointerUp(View v, MotionEvent event, TouchController controller) {
        controller.updateReferences(event);
        initScroll(event, controller);
    }

    @Override
    public boolean onDown(View v, MotionEvent event, TouchController controller) {
        if (listener.isFadingIn(event.getEventTime())) {
            return false;
        }
        initScroll(event, controller);
        listener.initAppShortcutsScroll(controller.getTouch());
        listener.initLongPress(v.getContext(), controller.getTouch());
        controller.getGeneralListener().invalidate();
        return true;
    }

    @Override
    public void onMove(View v, MotionEvent event, TouchController controller) {
        if (controller.isScroll(event)) {
            listener.cancelLongPress();
        }
        if (!scrollAppShortcuts(event, controller)) {
            scroll(event, controller);
        }
    }

    @Override
    public void onUp(View v, MotionEvent event, TouchController controller) {
        listener.cancelLongPress();
        listener.cancelDragDownList();
        keepScrolling(event);
        listener.setAppShortcutsTouched(false);
    }

    @Override
    public void onCancel(View v, MotionEvent event, TouchController controller) {
        listener.cancelLongPress();
        listener.cancelDragDownList();
        recycleVelocityTracker();
        listener.setAppShortcutsTouched(false);
    }

    private void initScroll(MotionEvent event, TouchController controller) {
        listener.stopFling();
        initVelocityTracker(event);
        scrollOffset = listener.getScrollY();
    }

    private boolean scrollAppShortcuts(MotionEvent event, TouchController controller) {
        if (!listener.isAppShortcutsTouched()) {
            return false;
        }
        int index = controller.getPrimaryIndex(event);
        TouchController.TouchReference tr = controller.getTouchReference(event, index);
        if (tr == null || controller.isTap(tr, event.getEventTime(), controller.getTapOrScrollTimeout())) {
            return true;
        }
        if (velocityTracker != null) {
            velocityTracker.addMovement(event);
        }
        int y = Math.round(event.getY(index));
        listener.scrollAppShortcuts(tr.scrollRef - y);
        return true;
    }

    private void scroll(MotionEvent event, TouchController controller) {
        if (listener.isDraggingDownList()) {
            listener.onDragDown(listener.getDragProgress());
            return;
        }
        int index = controller.getPrimaryIndex(event);
        TouchController.TouchReference tr = controller.getTouchReference(event, index);
        if (tr == null || controller.isTap(tr, event.getEventTime(), controller.getTapOrScrollTimeout())) {
            return;
        }
        if (velocityTracker != null) {
            velocityTracker.addMovement(event);
        }
        int y = Math.round(event.getY(index));
        int scrollY = scrollOffset + (tr.scrollRef - y);
        int maxScrollY = listener.getMaxScrollY();
        int lastScrollY = clamp(scrollY, 0, maxScrollY);
        listener.scroll(lastScrollY, true);
        if (lastScrollY == 0 || lastScrollY == maxScrollY) {
            tr.scrollRef = y;
            scrollOffset = listener.getScrollY();
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void keepScrolling(MotionEvent event) {
        if (event.getPointerCount() < 2 && velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000);
            listener.keepScrolling(Math.round(velocityTracker.getYVelocity()));
            recycleVelocityTracker();
        }
    }

    private void initVelocityTracker(MotionEvent event) {
        recycleVelocityTracker();
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }
}
