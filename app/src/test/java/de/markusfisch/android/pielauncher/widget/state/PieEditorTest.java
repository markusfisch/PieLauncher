package de.markusfisch.android.pielauncher.widget.state;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Process;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PieEditorTest {

    @Mock
    private PieEditor.Listener mockListener;

    private PieEditor pieEditor;
    private CanvasPieMenu pieMenu;
    private Bitmap mockBitmap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        pieMenu = new CanvasPieMenu();
        pieEditor = new PieEditor(mockListener, pieMenu);
        mockBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }

    private Apps.AppIcon createTestAppIcon(String label) {
        try {
            java.lang.reflect.Constructor<Apps.AppIcon> constructor =
                    Apps.AppIcon.class.getDeclaredConstructor(
                            ComponentName.class,
                            String.class,
                            Bitmap.class,
                            android.os.UserHandle.class
                    );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    new ComponentName("com.test", "Activity"),
                    label,
                    mockBitmap,
                    Process.myUserHandle()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AppIcon", e);
        }
    }

    @Test
    public void testEditIcon_SetsGrabbedIconAndInvalidates() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);

        assertTrue(pieEditor.hasGrabbedIcon());
        assertEquals(icon, pieEditor.getGrabbedIcon());
        assertFalse(pieEditor.getUngrabbedIcons().contains(icon));
        verify(mockListener, times(1)).invalidate();
    }

    @Test
    public void testResetGrabbedItem_RestoresGrabbedIcon() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);
        pieEditor.resetGrabbedItem();

        assertFalse(pieEditor.hasGrabbedIcon());
        assertTrue(pieMenu.icons.contains(icon));
        verify(mockListener, times(2)).invalidate(); // 1 for editIcon, 1 for resetGrabbedItem
    }

    @Test
    public void testCalculateEditablePie_UpdatesIconPosition() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);
        pieEditor.setTwist(0f);
        pieEditor.setTouch(new Point(200, 200));

        pieEditor.calculateEditablePie(100, 100);

        assertEquals(200, icon.x);
        assertEquals(200, icon.y);
    }

    @Test
    public void testCalculateEditablePie_MagnifiesIconsNearTouch() {
        Apps.AppIcon icon1 = createTestAppIcon("Icon 1");
        Apps.AppIcon icon2 = createTestAppIcon("Icon 2");
        Apps.AppIcon icon3 = createTestAppIcon("Icon 3");
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);
        pieMenu.icons.add(icon3);

        pieMenu.set(100, 100, 80, 0.0, 1f);
        pieEditor.editIcon(icon1);
        pieEditor.setTwist(0f);

        // Move touch away from center
        pieEditor.setTouch(new Point(180, 100));
        pieEditor.calculateEditablePie(100, 100);

        double minSize = Double.MAX_VALUE;
        double maxSize = Double.MIN_VALUE;
        for (Object obj : pieMenu.icons) {
            Apps.AppIcon ic = (Apps.AppIcon) obj;
            if (ic.size < minSize) minSize = ic.size;
            if (ic.size > maxSize) maxSize = ic.size;
        }

        assertTrue("Icons should have varying sizes based on proximity to touch, but min=" + minSize + " max=" + maxSize,
                maxSize > minSize);
    }

    @Test
    public void testTransactionLifecycle() {
        Apps.AppIcon icon1 = createTestAppIcon("Test App 1");
        Apps.AppIcon icon2 = createTestAppIcon("Test App 2");
        java.util.List<Apps.AppIcon> initialIcons = new java.util.ArrayList<>();
        initialIcons.add(icon1);
        initialIcons.add(icon2);

        // Begin edit
        pieEditor.beginEdit(initialIcons);

        // Modify pieMenu directly (simulating an edit)
        pieMenu.icons.clear();
        pieMenu.icons.add(icon1);

        // Rollback should restore the original list
        pieEditor.rollback(pieMenu.icons);
        assertEquals(2, pieMenu.icons.size());
        assertTrue(pieMenu.icons.contains(icon2));

        // Begin edit again
        pieEditor.beginEdit(initialIcons);

        // Modify pieMenu directly
        pieMenu.icons.clear();
        pieMenu.icons.add(icon1);

        // Commit should leave the backup empty but keep pieMenu as is
        pieEditor.commit();
        assertEquals(1, pieMenu.icons.size());
        assertFalse(pieMenu.icons.contains(icon2));
    }
}
