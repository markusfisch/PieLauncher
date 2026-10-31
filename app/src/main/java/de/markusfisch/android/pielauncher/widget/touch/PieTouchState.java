package de.markusfisch.android.pielauncher.widget.touch;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import de.markusfisch.android.pielauncher.graphics.PieMenu;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class PieTouchState implements TouchState {
    private final PieTouchListener listener;

    private int spinId = -1;
    private double spinAngleDown;
    private double spinInitialTwist;

    private long expandDownTime;
    private final Point expandTouch = new Point();
    private VelocityTracker velocityTracker;

    public PieTouchState(PieTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPointerDown(View v, MotionEvent event, TouchController controller) {
        cancelExpandPanel();
        startSpin(event, controller);
        controller.getGeneralListener().invalidate();
    }

    @Override
    public void onPointerUp(View v, MotionEvent event, TouchController controller) {
        stopSpin(event, controller);
    }

    @Override
    public boolean onDown(View v, MotionEvent event, TouchController controller) {
        if (listener.inDeadZone(controller.getTouch())) {
            initExpandPanel(event, event.getEventTime());
            return true;
        }
        listener.openMenu(event.getEventTime());
        listener.evaluateSelectedIcon(controller.getTouch());
        controller.getGeneralListener().invalidate();
        return true;
    }

    @Override
    public void onMove(View v, MotionEvent event, TouchController controller) {
        expandPanel(v.getContext(), event, controller);
        spin(event, controller);
        listener.evaluateSelectedIcon(controller.getTouch());
        controller.getGeneralListener().invalidate();
    }

    @Override
    public void onUp(View v, MotionEvent event, TouchController controller) {
        cancelSpin();
        cancelExpandPanel();
    }

    @Override
    public void onCancel(View v, MotionEvent event, TouchController controller) {
        cancelSpin();
        cancelExpandPanel();
        listener.fadeOutMode();
    }

    private void startSpin(MotionEvent event, TouchController controller) {
        if (spinId < 0) {
            initSpin(event, event.getActionIndex());
            listener.startSpin();
        }
    }

    private void spin(MotionEvent event, TouchController controller) {
        int count = event.getPointerCount();
        if (count == 1 && event.getPointerId(0) != controller.getPrimaryId()) {
            cancelSpin();
            controller.setPrimaryId(-1);
            listener.fadeOutMode();
            return;
        }
        if (count < 2) {
            cancelSpin();
            return;
        }
        for (int i = 0; i < count; ++i) {
            if (event.getPointerId(i) == spinId) {
                double twist = spinInitialTwist + PieMenu.getAngleDifference(
                        angleOf(event.getX(i), event.getY(i)),
                        spinAngleDown);
                listener.spin(twist);
                return;
            }
        }
    }

    private void stopSpin(MotionEvent event, TouchController controller) {
        cancelSpin();
        listener.stopSpin();
        int indexUp = event.getActionIndex();
        for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
            if (i == indexUp || event.getPointerId(i) == controller.getPrimaryId()) {
                continue;
            }
            initSpin(event, i);
            break;
        }
    }

    private void cancelSpin() {
        spinId = -1;
    }

    private void initSpin(MotionEvent event, int idx) {
        spinId = event.getPointerId(idx);
        spinAngleDown = angleOf(event.getX(idx), event.getY(idx));
        spinInitialTwist = listener.getPieTwist();
    }

    private double angleOf(float x, float y) {
        Point center = listener.getPieCenter();
        double a = Math.atan2(x - center.x, center.y - y);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    private void initExpandPanel(MotionEvent event, long eventTime) {
        if (!listener.expandPanelInDeadZone()) {
            return;
        }
        expandDownTime = eventTime;
        expandTouch.set(Math.round(event.getX()), Math.round(event.getY()));

        if (velocityTracker != null) {
            velocityTracker.recycle();
        }
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
    }

    private void expandPanel(Context context, MotionEvent event, TouchController controller) {
        if (velocityTracker == null) {
            return;
        }
        velocityTracker.addMovement(event);
        velocityTracker.computeCurrentVelocity(1000);
        float vy = velocityTracker.getYVelocity();
        if (expandDownTime > 0 &&
                event.getEventTime() - expandDownTime > controller.getTapOrScrollTimeout() &&
                distSq(expandTouch.x, expandTouch.y, event.getX(), event.getY()) > controller.getTouchSlopSq() &&
                vy > controller.getMinFlingVelocity() &&
                vy < controller.getMaxFlingVelocity()) {
            cancelExpandPanel();
            SystemBars.expandNotificationPanel(context);
        }
    }

    private void cancelExpandPanel() {
        expandDownTime = 0;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private static double distSq(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }
}
