package de.markusfisch.android.pielauncher.widget.touch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class TouchController implements View.OnTouchListener {
    public static class TouchReference {
        public final float x;
        public final float y;
        public final long time;
        public int scrollRef;

        public TouchReference(float x, float y, long time) {
            this.x = x;
            this.y = y;
            this.time = time;
            this.scrollRef = Math.round(y);
        }
    }

    private final GeneralTouchListener generalListener;
    private TouchState currentState;

    private final Point touch = new Point();
    private final SparseArray<TouchReference> touchReferences = new SparseArray<>();
    private final TapFlags tapFlags = new TapFlags();

    private int primaryId;
    private int pointerCount;
    private long lastTapUpTime;

    private Runnable performActionRunnable;

    private final float touchSlopSq;
    private final long tapOrScrollTimeout;
    private final long doubleTapTimeout;
    private final long longPressTimeout;
    private final float minFlingVelocity;
    private final float maxFlingVelocity;

    public TouchController(GeneralTouchListener generalListener) {
        this.generalListener = generalListener;
        Context context = generalListener.getContext();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        float touchSlop = configuration.getScaledTouchSlop();
        touchSlopSq = touchSlop * touchSlop;
        tapOrScrollTimeout = ViewConfiguration.getTapTimeout();
        doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
        minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setState(TouchState state) {
        this.currentState = state;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (currentState == null) return false;

        touch.set(Math.round(event.getX()), Math.round(event.getY()));
        generalListener.setTouch(touch);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerCount = event.getPointerCount();
                currentState.onPointerDown(v, event, this);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                currentState.onPointerUp(v, event, this);
                break;
            case MotionEvent.ACTION_DOWN:
                if (cancelPerformAction()) {
                    break;
                }
                pointerCount = 1;
                addTouch(event);
                return currentState.onDown(v, event, this);
            case MotionEvent.ACTION_MOVE:
                currentState.onMove(v, event, this);
                break;
            case MotionEvent.ACTION_UP:
                currentState.onUp(v, event, this);
                postPerformAction(v, event);
                break;
            case MotionEvent.ACTION_CANCEL:
                currentState.onCancel(v, event, this);
                generalListener.releaseIcon();
                break;
        }
        return true;
    }

    void addTouch(MotionEvent event) {
        int index = event.getActionIndex();
        primaryId = event.getPointerId(index);
        setTouchReference(event, primaryId, index);
    }

    void updateReferences(MotionEvent event) {
        for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
            int id = event.getPointerId(i);
            setTouchReference(event, id, i);
        }
    }

    private void setTouchReference(MotionEvent event, int id, int index) {
        touchReferences.put(id, new TouchReference(
                event.getX(index),
                event.getY(index),
                event.getEventTime()));
    }

    TouchReference getTouchReference(MotionEvent event, int index) {
        return index > -1 && index < event.getPointerCount()
                ? touchReferences.get(event.getPointerId(index))
                : null;
    }

    boolean isScroll(MotionEvent event) {
        return !isTap(event, Long.MAX_VALUE);
    }

    private boolean isTap(MotionEvent event, long timeOut) {
        TouchReference tr = getTouchReference(event, getPrimaryIndex(event));
        return isTap(tr, event.getEventTime(), timeOut);
    }

    boolean isTap(TouchReference tr, long time, long timeOut) {
        return tr != null &&
                time - tr.time <= timeOut &&
                distSq(tr.x, tr.y, touch.x, touch.y) <= touchSlopSq;
    }

    int getPrimaryIndex(MotionEvent event) {
        int count = event.getPointerCount();
        int id = -1;
        for (int i = 0; i < count; ++i) {
            id = event.getPointerId(i);
            if (id == primaryId) {
                return i;
            }
        }
        primaryId = id;
        return count - 1;
    }

    private void postPerformAction(final View v, MotionEvent event) {
        cancelPerformAction();
        tapFlags.wasTap = isTap(event, longPressTimeout);
        tapFlags.longPress = !tapFlags.wasTap && isTap(event, Long.MAX_VALUE);
        long eventTime = event.getEventTime();
        tapFlags.doubleTap = tapFlags.wasTap && eventTime - lastTapUpTime < doubleTapTimeout;
        tapFlags.twoFingerTap = pointerCount == 2;
        tapFlags.fourFingerTap = pointerCount == 4;
        lastTapUpTime = eventTime;
        final Point at = new Point(touch.x, touch.y);

        performActionRunnable = () -> {
            v.performClick();
            generalListener.performAction(v, at, tapFlags);
            performActionRunnable = null;
            generalListener.invalidate();
        };
        generalListener.postDelayed(performActionRunnable, 16);
    }

    private boolean cancelPerformAction() {
        if (performActionRunnable != null) {
            generalListener.removeCallbacks(performActionRunnable);
            performActionRunnable = null;
            return true;
        }
        return false;
    }

    private static double distSq(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    // Package-private getters
    int getPrimaryId() {
        return primaryId;
    }

    void setPrimaryId(int id) {
        primaryId = id;
    }

    GeneralTouchListener getGeneralListener() {
        return generalListener;
    }

    float getTouchSlopSq() {
        return touchSlopSq;
    }

    long getTapOrScrollTimeout() {
        return tapOrScrollTimeout;
    }

    float getMinFlingVelocity() {
        return minFlingVelocity;
    }

    float getMaxFlingVelocity() {
        return maxFlingVelocity;
    }

    Point getTouch() {
        return touch;
    }
}
