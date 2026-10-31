package de.markusfisch.android.pielauncher.widget.touch;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TouchControllerTest {

    private TouchController touchController;
    private MockGeneralListener generalListener;
    private MockPieListener pieListener;
    private MockListListener listListener;
    private PieTouchState pieState;
    private ListTouchState listState;
    private EditorTouchState editorState;
    private View mockView;

    private static class MockGeneralListener implements GeneralTouchListener {
        Context context = RuntimeEnvironment.getApplication();
        int performActionCount = 0;
        int releaseIconCount = 0;
        int editIconCount = 0;
        TapFlags lastTapFlags = null;
        Point lastEditPoint = null;
        private final java.util.List<Runnable> pendingRunnables = new java.util.ArrayList<>();

        void runPendingRunnables() {
            java.util.List<Runnable> copy = new java.util.ArrayList<>(pendingRunnables);
            pendingRunnables.clear();
            for (Runnable r : copy) {
                r.run();
            }
        }

        @Override
        public void performAction(View v, Point touch, TapFlags tapFlags) {
            performActionCount++;
            lastTapFlags = tapFlags;
        }

        @Override
        public void postDelayed(Runnable r, long delayMillis) {
            pendingRunnables.add(r);
        }

        @Override
        public void removeCallbacks(Runnable r) {
            pendingRunnables.remove(r);
        }

        @Override
        public void releaseIcon() {
            releaseIconCount++;
        }

        @Override
        public void setTouch(Point touch) {
        }

        @Override
        public void invalidate() {
        }

        @Override
        public Context getContext() {
            return context;
        }

        @Override
        public void editIconAt(Point touch) {
            editIconCount++;
            lastEditPoint = new Point(touch);
        }

        @Override
        public void updateEditablePie(Point touch) {
        }
    }

    private static class MockPieListener implements PieTouchListener {
        int openMenuCount = 0;
        int startSpinCount = 0;
        int spinCount = 0;
        int stopSpinCount = 0;
        int fadeOutModeCount = 0;
        boolean inDeadZone = false;

        @Override
        public void openMenu(long eventTime) {
            openMenuCount++;
        }

        @Override
        public void startSpin() {
            startSpinCount++;
        }

        @Override
        public void spin(double twist) {
            spinCount++;
        }

        @Override
        public void stopSpin() {
            stopSpinCount++;
        }

        @Override
        public double getPieTwist() {
            return 0.0;
        }

        @Override
        public Point getPieCenter() {
            return new Point(540, 960);
        }

        @Override
        public boolean inDeadZone(Point touch) {
            return inDeadZone;
        }

        boolean expandPanelInDeadZone = true;

        @Override
        public boolean expandPanelInDeadZone() {
            return expandPanelInDeadZone;
        }

        @Override
        public void fadeOutMode() {
            fadeOutModeCount++;
        }

        @Override
        public void evaluateSelectedIcon(Point touch) {
        }
    }

    private static class MockListListener implements ListTouchListener {
        boolean fadingIn = false;
        boolean draggingDownList = false;
        int initLongPressCount = 0;
        int cancelLongPressCount = 0;
        int scrollCount = 0;
        int keepScrollingCount = 0;
        int lastScrollY = -1;
        int lastVelocityY = 0;

        @Override
        public int getScrollY() {
            return 0;
        }

        @Override
        public int getMaxScrollY() {
            return 100;
        }

        @Override
        public void scroll(int scrollY, boolean anim) {
            scrollCount++;
            lastScrollY = scrollY;
        }

        @Override
        public void keepScrolling(int velocityY) {
            keepScrollingCount++;
            lastVelocityY = velocityY;
        }

        @Override
        public void initLongPress(Context context, Point touch) {
            initLongPressCount++;
        }

        @Override
        public void cancelLongPress() {
            cancelLongPressCount++;
        }

        @Override
        public boolean isFadingIn(long eventTime) {
            return fadingIn;
        }

        float dragProgress = 1f;
        int onDragDownCount = 0;
        int cancelDragDownListCount = 0;

        @Override
        public boolean isDraggingDownList() {
            return draggingDownList;
        }

        @Override
        public float getDragProgress() {
            return dragProgress;
        }

        @Override
        public void onDragDown(float dragProgress) {
            onDragDownCount++;
            this.dragProgress = dragProgress;
        }

        int stopFlingCount = 0;

        @Override
        public void cancelDragDownList() {
            cancelDragDownListCount++;
        }

        @Override
        public void stopFling() {
            stopFlingCount++;
        }

        boolean shortcutsTouched = false;
        int shortcutsScrollDelta = 0;

        @Override
        public void initAppShortcutsScroll(Point touch) {
        }

        @Override
        public void scrollAppShortcuts(int delta) {
            shortcutsScrollDelta = delta;
        }

        @Override
        public boolean isAppShortcutsTouched() {
            return shortcutsTouched;
        }

        @Override
        public void setAppShortcutsTouched(boolean touched) {
            shortcutsTouched = touched;
        }
    }

    @Before
    public void setUp() {
        generalListener = new MockGeneralListener();
        pieListener = new MockPieListener();
        listListener = new MockListListener();
        pieState = new PieTouchState(pieListener);
        listState = new ListTouchState(listListener);
        editorState = new EditorTouchState(generalListener);

        touchController = new TouchController(generalListener);
        mockView = new View(RuntimeEnvironment.getApplication());
    }

    @Test
    public void testPieModeTapOpensMenu() {
        touchController.setState(pieState);
        long downTime = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 500f, 0);

        touchController.onTouch(mockView, actionDown);

        assertEquals(1, pieListener.openMenuCount);
        actionDown.recycle();
    }

    @Test
    public void testPieModeDeadZoneIgnoresOpenMenu() {
        touchController.setState(pieState);
        pieListener.inDeadZone = true;
        long downTime = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 50f, 0);

        touchController.onTouch(mockView, actionDown);

        assertEquals("openMenu should not be called in dead zone", 0, pieListener.openMenuCount);
        actionDown.recycle();
    }

    @Test
    public void testPieModeDeadZone_WhenExpandPanelDisabled_DoesNotInitExpand() {
        touchController.setState(pieState);
        pieListener.inDeadZone = true;
        pieListener.expandPanelInDeadZone = false;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 50f, 0);
        touchController.onTouch(mockView, actionDown);

        // Verify expandDownTime is 0 (not initialized)
        try {
            java.lang.reflect.Field field = PieTouchState.class.getDeclaredField("expandDownTime");
            field.setAccessible(true);
            long expandDownTime = field.getLong(pieState);
            assertEquals("expandDownTime should not be set when expandPanelInDeadZone is false", 0, expandDownTime);
        } catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }

        actionDown.recycle();
    }

    @Test
    public void testPieModeDeadZone_WhenExpandPanelEnabled_InitsExpand() {
        touchController.setState(pieState);
        pieListener.inDeadZone = true;
        pieListener.expandPanelInDeadZone = true;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 50f, 0);
        touchController.onTouch(mockView, actionDown);

        // Verify expandDownTime is set
        try {
            java.lang.reflect.Field field = PieTouchState.class.getDeclaredField("expandDownTime");
            field.setAccessible(true);
            long expandDownTime = field.getLong(pieState);
            assertEquals("expandDownTime should be set when expandPanelInDeadZone is true", downTime, expandDownTime);
        } catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }

        actionDown.recycle();
    }

    @Test
    public void testPieModeMultiTouchStartsAndStopsSpin() {
        touchController.setState(pieState);
        long now = SystemClock.uptimeMillis();

        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        properties[0] = new MotionEvent.PointerProperties();
        properties[0].id = 0;
        properties[1] = new MotionEvent.PointerProperties();
        properties[1].id = 1;

        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        coords[0] = new MotionEvent.PointerCoords();
        coords[0].x = 500f;
        coords[0].y = 500f;
        coords[1] = new MotionEvent.PointerCoords();
        coords[1].x = 600f;
        coords[1].y = 600f;

        // 1. ACTION_DOWN for first pointer
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 1, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0);
        touchController.onTouch(mockView, actionDown);

        // 2. ACTION_POINTER_DOWN for second pointer
        int pointerDownAction = MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        MotionEvent actionPointerDown = MotionEvent.obtain(now, now + 10, pointerDownAction, 2, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0);
        touchController.onTouch(mockView, actionPointerDown);

        assertEquals("startSpin should be called", 1, pieListener.startSpinCount);

        // 3. ACTION_MOVE with second pointer
        coords[1].x = 700f;
        coords[1].y = 500f;
        MotionEvent actionMove = MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_MOVE, 2, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0);
        touchController.onTouch(mockView, actionMove);

        assertTrue("spin should be called", pieListener.spinCount > 0);

        // 4. ACTION_POINTER_UP
        int pointerUpAction = MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        MotionEvent actionPointerUp = MotionEvent.obtain(now, now + 30, pointerUpAction, 2, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0);
        touchController.onTouch(mockView, actionPointerUp);

        assertEquals("stopSpin should be called", 1, pieListener.stopSpinCount);

        actionDown.recycle();
        actionPointerDown.recycle();
        actionMove.recycle();
        actionPointerUp.recycle();
    }

    @Test
    public void testPieModeActionCancelFadesOutAndReleases() {
        touchController.setState(pieState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        MotionEvent actionCancel = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_CANCEL, 500f, 500f, 0);
        touchController.onTouch(mockView, actionCancel);

        assertEquals(1, pieListener.fadeOutModeCount);
        assertEquals(1, generalListener.releaseIconCount);

        actionDown.recycle();
        actionCancel.recycle();
    }

    @Test
    public void testListModeIgnoresTouchWhileFadingIn() {
        touchController.setState(listState);
        listListener.fadingIn = true;
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);

        boolean consumed = touchController.onTouch(mockView, actionDown);

        assertFalse("Touch should not be consumed while fading in list mode", consumed);
        assertEquals(0, listListener.initLongPressCount);

        actionDown.recycle();
    }

    @Test
    public void testListModeInitAndCancelLongPressOnMove() {
        touchController.setState(listState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        assertEquals(1, listListener.initLongPressCount);

        // Move beyond touch slop to trigger scroll & cancel long press
        MotionEvent actionMove = MotionEvent.obtain(now, now + 200, MotionEvent.ACTION_MOVE, 500f, 100f, 0);
        touchController.onTouch(mockView, actionMove);

        assertTrue("cancelLongPress should be called when scrolling", listListener.cancelLongPressCount > 0);

        actionDown.recycle();
        actionMove.recycle();
    }

    @Test
    public void testListModeDownStopsFling() {
        touchController.setState(listState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        assertEquals("stopFling should be called on ACTION_DOWN in list mode", 1, listListener.stopFlingCount);

        actionDown.recycle();
    }

    @Test
    public void testListModeScrollClamping() {
        touchController.setState(listState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        // Move pointer up (y 500 -> 100), scroll delta = +400. Max scroll is 100.
        MotionEvent actionMove = MotionEvent.obtain(now, now + 200, MotionEvent.ACTION_MOVE, 500f, 100f, 0);
        touchController.onTouch(mockView, actionMove);

        assertTrue("scroll should be called", listListener.scrollCount > 0);
        assertEquals("Scroll Y should be clamped to maxScrollY (100)", 100, listListener.lastScrollY);

        actionDown.recycle();
        actionMove.recycle();
    }

    @Test
    public void testListModeKeepScrollingOnActionUp() {
        touchController.setState(listState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        MotionEvent actionMove = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_MOVE, 500f, 300f, 0);
        touchController.onTouch(mockView, actionMove);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, 500f, 200f, 0);
        touchController.onTouch(mockView, actionUp);

        assertEquals(1, listListener.keepScrollingCount);

        actionDown.recycle();
        actionMove.recycle();
        actionUp.recycle();
    }

    @Test
    public void testListModeAppShortcutsScroll() {
        touchController.setState(listState);
        listListener.shortcutsTouched = true;

        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        MotionEvent actionMove = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_MOVE, 500f, 400f, 0);
        touchController.onTouch(mockView, actionMove);

        assertEquals("Should scroll shortcuts instead of regular list", 100, listListener.shortcutsScrollDelta);
        assertEquals("Regular list should not be scrolled", 0, listListener.scrollCount);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 150, MotionEvent.ACTION_UP, 500f, 400f, 0);
        touchController.onTouch(mockView, actionUp);
        assertFalse("shortcutsTouched should reset on ACTION_UP", listListener.shortcutsTouched);

        actionDown.recycle();
        actionMove.recycle();
        actionUp.recycle();
    }

    @Test
    public void testEditModeTriggersEditIconAt() {
        touchController.setState(editorState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 300f, 400f, 0);

        touchController.onTouch(mockView, actionDown);

        assertEquals(1, generalListener.editIconCount);
        assertNotNull(generalListener.lastEditPoint);
        assertEquals(300, generalListener.lastEditPoint.x);
        assertEquals(400, generalListener.lastEditPoint.y);

        actionDown.recycle();
    }

    @Test
    public void testPostPerformActionSingleTap() {
        touchController.setState(pieState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 500f, 500f, 0);
        touchController.onTouch(mockView, actionUp);

        // Run delayed action runnable
        generalListener.runPendingRunnables();

        assertEquals(1, generalListener.performActionCount);
        assertNotNull(generalListener.lastTapFlags);
        assertTrue("wasTap should be true for quick release without movement", generalListener.lastTapFlags.wasTap);

        actionDown.recycle();
        actionUp.recycle();
    }

    @Test
    public void testListModeDragDownNotifiesListenerAndCancelsOnUp() {
        touchController.setState(listState);
        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        touchController.onTouch(mockView, actionDown);

        listListener.draggingDownList = true;
        listListener.dragProgress = 0.4f;

        MotionEvent actionMove = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_MOVE, 500f, 600f, 0);
        touchController.onTouch(mockView, actionMove);

        assertEquals("onDragDown should be called during move when dragging down list", 1, listListener.onDragDownCount);
        assertEquals(0.4f, listListener.dragProgress, 0.001f);
        assertEquals("Regular scrolling should be bypassed when dragging down", 0, listListener.scrollCount);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 150, MotionEvent.ACTION_UP, 500f, 600f, 0);
        touchController.onTouch(mockView, actionUp);

        assertEquals("cancelDragDownList should be called on ACTION_UP", 1, listListener.cancelDragDownListCount);

        actionDown.recycle();
        actionMove.recycle();
        actionUp.recycle();
    }
}
