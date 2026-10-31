package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.core.app.ApplicationProvider;

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers;
import com.github.takahirom.roborazzi.RoborazziRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowApplication;

import android.content.Intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, sdk = 34)
public class AppPieViewTest {

    @Rule
    public RoborazziRule roborazziRule = new RoborazziRule(
            new com.github.takahirom.roborazzi.RoborazziRule.Options()
    );

    private AppPieView appPieView;

    private Apps.AppIcon redIcon;
    private Apps.AppIcon greenIcon;
    private Apps.AppIcon blueIcon;


    private AppPieView.ActionCallback mockActionCallback;
    private Apps.AppIcon tappedAppIcon;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        // Generate mock data for the Pie Menu
        Bitmap redSquare = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvasRed = new Canvas(redSquare);
        canvasRed.drawColor(Color.RED);

        Bitmap greenSquare = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvasGreen = new Canvas(greenSquare);
        canvasGreen.drawColor(Color.GREEN);

        Bitmap blueSquare = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvasBlue = new Canvas(blueSquare);
        canvasBlue.drawColor(Color.BLUE);

        PieLauncherApp.apps.menuPrimary.clear();
        try {
            java.lang.reflect.Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                    android.content.ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
            constructor.setAccessible(true);


            redIcon = constructor.newInstance(
                    new android.content.ComponentName("com.mock.red", "MockRedActivity"),
                    "Red App", redSquare, android.os.Process.myUserHandle());

            greenIcon = constructor.newInstance(
                    new android.content.ComponentName("com.mock.green", "MockGreenActivity"),
                    "Green App", greenSquare, android.os.Process.myUserHandle());

            blueIcon = constructor.newInstance(
                    new android.content.ComponentName("com.mock.blue", "MockBlueActivity"),
                    "Blue App", blueSquare, android.os.Process.myUserHandle());

            PieLauncherApp.apps.menuPrimary.add(redIcon);
            PieLauncherApp.apps.menuPrimary.add(greenIcon);
            PieLauncherApp.apps.menuPrimary.add(blueIcon);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock AppIcon", e);
        }

        // We initialize the view first
        android.app.Activity activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).create().start().resume().visible().get();

        appPieView = new AppPieView(activity, null);
        mockActionCallback = new AppPieView.ActionCallback() {
            @Override
            public void onAppTapped(Apps.AppIcon icon) {
                tappedAppIcon = icon;
            }

            @Override
            public void onAppInfoRequested(Apps.AppIcon icon) {
            }

            @Override
            public void onEditOptionsRequested(Apps.AppIcon icon) {
            }
        };
        appPieView.setActionCallback(mockActionCallback);
        activity.setContentView(appPieView);

        // Layout the view to a default size so it has width/height for drawing
        appPieView.layout(0, 0, 1080, 2280);
    }

    @Test
    public void testDefaultState() {
        // Render the default state
        // RoborazziRule will automatically capture the screenshot at the end of the test
    }

    @Test
    public void testTouchState() {
        // Simulate a touch event in the center
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent actionDown = MotionEvent.obtain(
                downTime, eventTime, MotionEvent.ACTION_DOWN,
                540f, 1140f, 0
        );

        appPieView.dispatchTouchEvent(actionDown);
        ShadowLooper.idleMainLooper();

        // RoborazziRule will automatically capture the screenshot at the end of the test

        actionDown.recycle();
    }

    @Test
    @org.robolectric.annotation.Config(sdk = 19)
    public void testPieMenuDragLaunchesApp() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Use reflection to set pieMenu.icons directly, circumventing any background
        // indexing thread race conditions in Apps.java that clear the lists.
        try {
            java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
            pieMenuField.setAccessible(true);
            Object pieMenu = pieMenuField.get(appPieView);
            java.lang.reflect.Field iconsField = pieMenu.getClass().getSuperclass().getDeclaredField("icons");
            iconsField.setAccessible(true);
            java.util.List<Apps.AppIcon> mockIcons = new java.util.ArrayList<>();
            mockIcons.add(redIcon);
            mockIcons.add(greenIcon);
            mockIcons.add(blueIcon);
            iconsField.set(pieMenu, mockIcons);

            // Also override the menuPrimary field in AppPieView to avoid setMenu clearing it back
            java.lang.reflect.Field menuPrimaryField = AppPieView.class.getDeclaredField("menuPrimary");
            menuPrimaryField.setAccessible(true);
            menuPrimaryField.set(appPieView, mockIcons);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Force a layout and draw pass so the view initializes its center coordinates
        // and sets pieMenuHasBeenCalculated to true!
        appPieView.layout(0, 0, 1080, 2400);
        appPieView.draw(new android.graphics.Canvas());

        // Mock the package manager to return an intent for our mock package
        org.robolectric.shadows.ShadowPackageManager shadowPm =
                org.robolectric.shadow.api.Shadow.extract(
                        org.robolectric.RuntimeEnvironment.getApplication().getPackageManager());

        Intent mockIntent = new Intent(Intent.ACTION_MAIN);
        mockIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mockIntent.setPackage("com.mock.red");
        android.content.pm.ResolveInfo resolveInfo = new android.content.pm.ResolveInfo();
        resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
        resolveInfo.activityInfo.packageName = "com.mock.red";
        resolveInfo.activityInfo.name = "MockRedActivity";
        shadowPm.addResolveInfoForIntent(mockIntent, resolveInfo);

        Intent greenIntent = new Intent(Intent.ACTION_MAIN);
        greenIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        greenIntent.setPackage("com.mock.green");
        android.content.pm.ResolveInfo greenResolve = new android.content.pm.ResolveInfo();
        greenResolve.activityInfo = new android.content.pm.ActivityInfo();
        greenResolve.activityInfo.packageName = "com.mock.green";
        greenResolve.activityInfo.name = "MockGreenActivity";
        shadowPm.addResolveInfoForIntent(greenIntent, greenResolve);

        Intent blueIntent = new Intent(Intent.ACTION_MAIN);
        blueIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        blueIntent.setPackage("com.mock.blue");
        android.content.pm.ResolveInfo blueResolve = new android.content.pm.ResolveInfo();
        blueResolve.activityInfo = new android.content.pm.ActivityInfo();
        blueResolve.activityInfo.packageName = "com.mock.blue";
        blueResolve.activityInfo.name = "MockBlueActivity";
        shadowPm.addResolveInfoForIntent(blueIntent, blueResolve);

        // Center of the screen
        float startX = 540f;
        float startY = 1200f;

        // Touch down at the center
        MotionEvent actionDown = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                startX, startY, 0
        );
        appPieView.dispatchTouchEvent(actionDown);

        // Move to the right, far enough to select an icon and break the dead zone
        float endX = startX + 400f;
        float endY = startY;

        MotionEvent actionMove = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis() + 100,
                MotionEvent.ACTION_MOVE,
                endX, endY, 0
        );
        appPieView.dispatchTouchEvent(actionMove);

        // Force the view to draw so pieMenu.calculate() is invoked with the new touch coordinates
        // Advance time so fadePie animation progresses and f > 0
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(200));
        appPieView.draw(new android.graphics.Canvas());

        // Release
        MotionEvent actionUp = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis() + 200,
                MotionEvent.ACTION_UP,
                endX, endY, 0
        );
        appPieView.dispatchTouchEvent(actionUp);

        // Run any pending runnables (like postPerformAction)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Assert that the correct icon was selected by the drag
        org.junit.Assert.assertNotNull("Expected an app to be tapped", tappedAppIcon);
        org.junit.Assert.assertEquals("Expected redIcon to be tapped", redIcon, tappedAppIcon);
        actionMove.recycle();
        actionUp.recycle();
    }

    @Test
    public void testPieMenuSpin() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        int finalSelectedIcon1 = -1;
        int finalSelectedIcon2 = -1;

        try {
            java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
            pieMenuField.setAccessible(true);
            Object pieMenu = pieMenuField.get(appPieView);
            java.lang.reflect.Field iconsField = pieMenu.getClass().getSuperclass().getDeclaredField("icons");
            iconsField.setAccessible(true);
            java.util.List<Apps.AppIcon> mockIcons = new java.util.ArrayList<>();
            mockIcons.add(redIcon);
            mockIcons.add(greenIcon);
            mockIcons.add(blueIcon);
            iconsField.set(pieMenu, mockIcons);

            java.lang.reflect.Field menuPrimaryField = AppPieView.class.getDeclaredField("menuPrimary");
            menuPrimaryField.setAccessible(true);
            menuPrimaryField.set(appPieView, mockIcons);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        appPieView.layout(0, 0, 1080, 2400);
        appPieView.draw(new android.graphics.Canvas());

        long downTime = SystemClock.uptimeMillis();

        // Touch down at center
        MotionEvent actionDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN,
                540f, 1200f, 0
        );
        appPieView.dispatchTouchEvent(actionDown);

        // Move to the right (angle 0)
        MotionEvent actionMove1 = MotionEvent.obtain(
                downTime, downTime + 100, MotionEvent.ACTION_MOVE,
                940f, 1200f, 0
        );
        appPieView.dispatchTouchEvent(actionMove1);
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(200));
        appPieView.draw(new android.graphics.Canvas());

        try {
            java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
            pieMenuField.setAccessible(true);
            Object pieMenu = pieMenuField.get(appPieView);
            java.lang.reflect.Method getSelectedIconMethod = pieMenu.getClass().getSuperclass().getDeclaredMethod("getSelectedIcon");
            getSelectedIconMethod.setAccessible(true);
            finalSelectedIcon1 = (int) getSelectedIconMethod.invoke(pieMenu);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Move to the bottom left (angle ~ 120/150)
        MotionEvent actionMove2 = MotionEvent.obtain(
                downTime, downTime + 200, MotionEvent.ACTION_MOVE,
                140f, 1600f, 0
        );
        appPieView.dispatchTouchEvent(actionMove2);
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(200));
        appPieView.draw(new android.graphics.Canvas());

        try {
            java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
            pieMenuField.setAccessible(true);
            Object pieMenu = pieMenuField.get(appPieView);
            java.lang.reflect.Method getSelectedIconMethod = pieMenu.getClass().getSuperclass().getDeclaredMethod("getSelectedIcon");
            getSelectedIconMethod.setAccessible(true);
            finalSelectedIcon2 = (int) getSelectedIconMethod.invoke(pieMenu);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Release
        MotionEvent actionUp = MotionEvent.obtain(
                downTime, downTime + 300, MotionEvent.ACTION_UP,
                140f, 1600f, 0
        );
        appPieView.dispatchTouchEvent(actionUp);

        org.junit.Assert.assertNotEquals("Icon should have changed during spin", finalSelectedIcon1, finalSelectedIcon2);

        actionDown.recycle();
        actionMove1.recycle();
        actionMove2.recycle();
        actionUp.recycle();
    }

    @Test
    public void testTapAction() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        final boolean[] listOpened = {false};
        appPieView.setListListener(new AppPieView.ListListener() {
            @Override
            public void onOpenList(boolean resume) {
                listOpened[0] = true;
            }

            @Override
            public void onHideList() {
            }

            @Override
            public void onScrollList(int y, boolean isScrolling) {
            }

            @Override
            public void onDragDown(float alpha) {
            }
        });

        appPieView.layout(0, 0, 1080, 2400);
        appPieView.draw(new android.graphics.Canvas());

        long downTime = SystemClock.uptimeMillis();

        // Touch down at center
        MotionEvent actionDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN,
                540f, 1200f, 0
        );
        appPieView.dispatchTouchEvent(actionDown);

        // Release quickly to simulate a tap
        MotionEvent actionUp = MotionEvent.obtain(
                downTime, downTime + 50, MotionEvent.ACTION_UP,
                540f, 1200f, 0
        );
        appPieView.dispatchTouchEvent(actionUp);

        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(500));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


        org.junit.Assert.assertTrue("Tap should have opened the list", listOpened[0]);

        actionDown.recycle();
        actionUp.recycle();
    }

    @Test
    public void testLongPressOpensEditor() {
        android.app.Activity activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).create().start().resume().visible().get();

        appPieView = new AppPieView(activity, null);
        mockActionCallback = new AppPieView.ActionCallback() {
            @Override
            public void onAppTapped(Apps.AppIcon icon) {
                tappedAppIcon = icon;
            }

            @Override
            public void onAppInfoRequested(Apps.AppIcon icon) {
            }

            @Override
            public void onEditOptionsRequested(Apps.AppIcon icon) {
            }
        };
        appPieView.setActionCallback(mockActionCallback);
        activity.setContentView(appPieView);

        // Populate appList using reflection
        de.markusfisch.android.pielauncher.content.Apps.AppIcon appIcon = null;
        try {
            java.lang.reflect.Constructor<de.markusfisch.android.pielauncher.content.Apps.AppIcon> constructor = de.markusfisch.android.pielauncher.content.Apps.AppIcon.class.getDeclaredConstructor(android.content.ComponentName.class, String.class, android.graphics.drawable.Drawable.class, android.os.UserHandle.class);
            constructor.setAccessible(true);
            appIcon = constructor.newInstance(new android.content.ComponentName("com.example.app", "TestApp"), "Test App", new android.graphics.drawable.ColorDrawable(0), android.os.Process.myUserHandle());
        } catch (Exception e) {
            e.printStackTrace();
        }
        java.util.List<de.markusfisch.android.pielauncher.content.Apps.AppIcon> mockList = new java.util.ArrayList<>();
        mockList.add(appIcon);

        try {
            java.lang.reflect.Field appListField = AppPieView.class.getDeclaredField("appList");
            appListField.setAccessible(true);
            appListField.set(appPieView, mockList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        appPieView.layout(0, 0, 1080, 2400);
        appPieView.draw(new android.graphics.Canvas());

        appPieView.showList();

        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(300));
        long downTime = android.os.SystemClock.uptimeMillis();

        // Calculate Y position for the first icon
        // listPaddingTop = Math.round(64f * dp)
        // iconSize = Math.round(48f * dp)
        float dp = activity.getResources().getDisplayMetrics().density;
        float y = Math.round(64f * dp) + (Math.round(48f * dp) / 2f);

        // Manually set the hit rect for the icon so getListIconAt works
        appIcon.hitRect.set(0, Math.round(y - 20), 1080, Math.round(y + 20));

        // Touch down
        android.view.MotionEvent actionDown = android.view.MotionEvent.obtain(
                downTime, downTime, android.view.MotionEvent.ACTION_DOWN,
                540f, y, 0
        );
        appPieView.dispatchTouchEvent(actionDown);

        // Advance time for long press timeout
        long longPressTimeout = android.view.ViewConfiguration.getLongPressTimeout();
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(longPressTimeout + 100));
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        org.junit.Assert.assertTrue("Mode should be MODE_EDIT", appPieView.inEditMode());

        actionDown.recycle();
    }

    @Test
    public void testIsFilteredAndGetQuery() {
        org.junit.Assert.assertFalse(appPieView.isFiltered());
        org.junit.Assert.assertEquals("", appPieView.getQuery());

        appPieView.filterAppList("calculator", false);
        org.junit.Assert.assertTrue(appPieView.isFiltered());
        org.junit.Assert.assertEquals("calculator", appPieView.getQuery());

        appPieView.filterAppList("", false);
        org.junit.Assert.assertFalse(appPieView.isFiltered());
        org.junit.Assert.assertEquals("", appPieView.getQuery());
    }

    @Test
    public void testIsScrollingAppShortcutsDefaultFalse() {
        org.junit.Assert.assertFalse(appPieView.isScrollingAppShortcuts());
    }

    @Test
    public void testEditModeDragChangesIconSizes() {
        appPieView.showEditor();
        org.junit.Assert.assertTrue("Should be in edit mode", appPieView.inEditMode());

        // Grab redIcon
        appPieView.addIconInteractively(redIcon);

        // Move touch away from center towards one of the icons
        long now = SystemClock.uptimeMillis();
        MotionEvent actionMove = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 700f, 1200f, 0);
        appPieView.dispatchTouchEvent(actionMove);

        // Draw view to trigger layout & render
        appPieView.draw(new Canvas());

        try {
            java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
            pieMenuField.setAccessible(true);
            de.markusfisch.android.pielauncher.graphics.CanvasPieMenu<?> pieMenu =
                    (de.markusfisch.android.pielauncher.graphics.CanvasPieMenu<?>) pieMenuField.get(appPieView);

            double minSize = Double.MAX_VALUE;
            double maxSize = Double.MIN_VALUE;
            for (Object obj : pieMenu.icons) {
                de.markusfisch.android.pielauncher.graphics.PieMenu.Icon ic =
                        (de.markusfisch.android.pielauncher.graphics.PieMenu.Icon) obj;
                if (ic.size < minSize) minSize = ic.size;
                if (ic.size > maxSize) maxSize = ic.size;
            }

            org.junit.Assert.assertTrue("Icons should have varying sizes in edit mode when moving: min=" + minSize + " max=" + maxSize,
                    maxSize > minSize);
        } catch (Exception e) {
            e.printStackTrace();
            org.junit.Assert.fail("Reflection failed: " + e.getMessage());
        }

        actionMove.recycle();
    }

    @Test
    public void testDragDownListBy_NotifiesListListener() {
        appPieView.layout(0, 0, 1080, 2400);
        appPieView.showList();
        final float[] lastProgress = new float[]{-1f};
        appPieView.setListListener(new AppPieView.ListListener() {
            @Override
            public void onOpenList(boolean resume) {}
            @Override
            public void onHideList() {}
            @Override
            public void onScrollList(int y, boolean isScrolling) {}
            @Override
            public void onDragDown(float alpha) {
                lastProgress[0] = alpha;
            }
        });

        appPieView.dragDownListBy(-50f);
        org.junit.Assert.assertTrue("onDragDown should be notified with progress < 1f",
                lastProgress[0] >= 0f && lastProgress[0] < 1f);
    }

    @Test
    public void testDragDownList_WhenDragProgressSmall_DismissesList() {
        appPieView.layout(0, 0, 1080, 2400);
        appPieView.showList();
        final boolean[] onHideCalled = new boolean[]{false};
        appPieView.setListListener(new AppPieView.ListListener() {
            @Override
            public void onOpenList(boolean resume) {}
            @Override
            public void onHideList() {
                onHideCalled[0] = true;
            }
            @Override
            public void onScrollList(int y, boolean isScrolling) {}
            @Override
            public void onDragDown(float alpha) {}
        });

        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        appPieView.dispatchTouchEvent(actionDown);
        actionDown.recycle();

        // Drag down by a large enough amount to bring dragProgress <= 0.5f (listFadeHeight is 1200)
        appPieView.dragDownListBy(-1000f);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 500f, 550f, 0);
        appPieView.dispatchTouchEvent(actionUp);
        actionUp.recycle();

        org.junit.Assert.assertTrue("onHideList should be called when dragged down past 50%", onHideCalled[0]);
    }

    @Test
    public void testDragDownList_WhenDragProgressLarge_SnapsBack() {
        appPieView.layout(0, 0, 1080, 2400);
        appPieView.showList();
        final boolean[] onHideCalled = new boolean[]{false};
        final float[] restoredProgress = new float[]{-1f};
        appPieView.setListListener(new AppPieView.ListListener() {
            @Override
            public void onOpenList(boolean resume) {}
            @Override
            public void onHideList() {
                onHideCalled[0] = true;
            }
            @Override
            public void onScrollList(int y, boolean isScrolling) {}
            @Override
            public void onDragDown(float alpha) {
                restoredProgress[0] = alpha;
            }
        });

        long now = SystemClock.uptimeMillis();
        MotionEvent actionDown = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        appPieView.dispatchTouchEvent(actionDown);
        actionDown.recycle();

        // Drag down only slightly (dragProgress > 0.5f)
        appPieView.dragDownListBy(-50f);

        MotionEvent actionUp = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 500f, 550f, 0);
        appPieView.dispatchTouchEvent(actionUp);
        actionUp.recycle();

        org.junit.Assert.assertFalse("onHideList should NOT be called for minor drag", onHideCalled[0]);
        org.junit.Assert.assertEquals("Progress should snap back to 1.0", 1.0f, restoredProgress[0], 0.001f);
    }

    @Test
    public void testListScrollRendersAppsBeyondInitialViewport() {
        java.util.List<Apps.AppIcon> mockList = new java.util.ArrayList<>();
        try {
            java.lang.reflect.Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                    android.content.ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
            constructor.setAccessible(true);
            Bitmap iconBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < 60; i++) {
                Apps.AppIcon icon = constructor.newInstance(
                        new android.content.ComponentName("com.mock.test" + i, "MockActivity" + i),
                        "App " + i, iconBitmap, android.os.Process.myUserHandle());
                mockList.add(icon);
            }

            java.lang.reflect.Field appListField = AppPieView.class.getDeclaredField("appList");
            appListField.setAccessible(true);
            appListField.set(appPieView, mockList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        appPieView.layout(0, 0, 1080, 1000);
        appPieView.showList();

        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(500));

        // Draw at scroll 0
        appPieView.draw(new Canvas());

        // First app should be rendered (hitRect non-empty)
        org.junit.Assert.assertFalse("First app hitRect should not be empty", mockList.get(0).hitRect.isEmpty());
        // Last app (app 59) is far down and should NOT be rendered at scroll 0
        org.junit.Assert.assertTrue("App 59 hitRect should be empty before scrolling", mockList.get(59).hitRect.isEmpty());

        // Now scroll the list down via scrollList
        try {
            java.lang.reflect.Method scrollListMethod = AppPieView.class.getDeclaredMethod("scrollList", int.class, boolean.class);
            scrollListMethod.setAccessible(true);
            scrollListMethod.invoke(appPieView, 2500, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Draw again at new scroll position
        appPieView.draw(new Canvas());

        // Now app 59 should have been rendered and its hitRect should not be empty
        org.junit.Assert.assertFalse("App 59 hitRect should be non-empty after scrolling down", mockList.get(59).hitRect.isEmpty());
    }

    @Test
    public void testListRetainsScrollPositionAcrossHideAndShow() {
        appPieView.layout(0, 0, 1080, 1000);
        appPieView.showList();
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(500));

        try {
            java.lang.reflect.Method scrollListMethod = AppPieView.class.getDeclaredMethod("scrollList", int.class, boolean.class);
            scrollListMethod.setAccessible(true);
            scrollListMethod.invoke(appPieView, 350, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        org.junit.Assert.assertEquals(350, appPieView.getScrollY());

        // Hide list
        appPieView.hideList();
        org.junit.Assert.assertEquals("Scroll offset should be reset to 0 in pie mode", 0, appPieView.getScrollY());

        // Show list again
        appPieView.showList();
        org.junit.Assert.assertEquals("Scroll offset should be restored to lastScrollY when reopening list", 350, appPieView.getScrollY());
    }
}
