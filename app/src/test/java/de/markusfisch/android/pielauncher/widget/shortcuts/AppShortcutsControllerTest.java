package de.markusfisch.android.pielauncher.widget.shortcuts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.content.Apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppShortcutsControllerTest {

    private Context context;
    private AppShortcutsController controller;
    private Apps.AppIcon ownerApp;
    private List<Apps.AppIcon> mockShortcuts;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        float dp = context.getResources().getDisplayMetrics().density;
        controller = new AppShortcutsController(context, dp);

        Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                android.content.ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
        constructor.setAccessible(true);

        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        ownerApp = constructor.newInstance(
                new android.content.ComponentName("com.test.app", "MainActivity"),
                "Test App", bitmap, android.os.Process.myUserHandle());

        mockShortcuts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Apps.AppIcon shortcut = constructor.newInstance(
                    new android.content.ComponentName("com.test.app", "Shortcut" + i),
                    "Shortcut " + i, bitmap, android.os.Process.myUserHandle());
            mockShortcuts.add(shortcut);
        }
    }

    @Test
    public void initialHeightIsZero() {
        assertEquals(0, controller.getHeight(true, true));
        assertFalse(controller.isVisible());
        assertFalse(controller.isTouched());
    }

    @Test
    public void heightReflectsShortcutsCount() {
        controller.setShortcutsForTesting(ownerApp, mockShortcuts);
        int expectedHeight = 3 * controller.getAppShortcutHeight() + controller.getAppShortcutsVerticalPadding() * 2;
        assertEquals(expectedHeight, controller.getHeight(true, true));
    }

    @Test
    public void heightCappedAtMaxVisibleShortcuts() throws Exception {
        Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                android.content.ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
        constructor.setAccessible(true);
        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);

        List<Apps.AppIcon> sixShortcuts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixShortcuts.add(constructor.newInstance(
                    new android.content.ComponentName("com.test.app", "Shortcut" + i),
                    "Shortcut " + i, bitmap, android.os.Process.myUserHandle()));
        }

        controller.setShortcutsForTesting(ownerApp, sixShortcuts);
        int expectedHeight = 4 * controller.getAppShortcutHeight() + controller.getAppShortcutsVerticalPadding() * 2;
        assertEquals(expectedHeight, controller.getHeight(true, true));
    }

    @Test
    public void layoutPositionsRectCorrectly() {
        controller.setShortcutsForTesting(ownerApp, mockShortcuts);
        int shortcutsHeight = controller.getHeight(true, true);
        int viewWidth = 1080;
        int viewHeight = 2400;
        int bottomInset = 100;
        int listPadding = 32;

        controller.layout(viewWidth, viewHeight, bottomInset, listPadding, shortcutsHeight, 0);

        Rect rect = controller.getRect();
        assertEquals(listPadding, rect.left);
        assertEquals(viewWidth - listPadding, rect.right);
        assertEquals(viewHeight - bottomInset - listPadding, rect.bottom);
        assertEquals(viewHeight - bottomInset - listPadding - shortcutsHeight, rect.top);
        assertTrue(controller.contains(viewWidth / 2, rect.top + 10));
        assertFalse(controller.contains(viewWidth / 2, rect.top - 10));
    }

    @Test
    public void hitTestIdentifiesShortcut() {
        controller.setShortcutsForTesting(ownerApp, mockShortcuts);
        int shortcutsHeight = controller.getHeight(true, true);
        controller.layout(1000, 2000, 50, 20, shortcutsHeight, 0);

        // Assign hitRect as layout would
        Apps.AppIcon first = mockShortcuts.get(0);
        first.hitRect.set(20, 100, 980, 150);

        assertEquals(first, controller.getShortcutAt(50, 120));
        assertNull(controller.getShortcutAt(50, 200));
    }

    @Test
    public void scrollInteractionClampsWithinBounds() {
        controller.setShortcutsForTesting(ownerApp, mockShortcuts);
        controller.layout(1000, 2000, 50, 20, 100, 0);
        controller.setMaxScrollYForTesting(200);

        controller.initScroll(new Point(100, 1900), true);
        controller.scrollBy(50);
        assertEquals(50, controller.getScrollY());

        controller.scrollBy(300);
        assertEquals(200, controller.getScrollY());

        controller.scrollBy(-500);
        assertEquals(0, controller.getScrollY());
    }
}
