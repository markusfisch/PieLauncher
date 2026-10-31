package de.markusfisch.android.pielauncher.widget.renderer;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Process;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.widget.state.EditorAction;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import de.markusfisch.android.pielauncher.widget.state.Fader;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EditorRendererTest {

    @Mock
    private Canvas mockCanvas;

    @Mock
    private Fader mockFaderEdit;

    private EditorRenderer editorRenderer;
    private Bitmap mockBitmap;
    private Rect dummyRect;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        editorRenderer = new EditorRenderer(context);
        mockBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        dummyRect = new Rect(0, 0, 50, 50);
        editorRenderer.setActionSize(100);
        editorRenderer.iconSplitPie = mockBitmap;
        editorRenderer.iconChangeTwist = mockBitmap;
        editorRenderer.iconChangeIconScale = mockBitmap;
        editorRenderer.iconChangeRadius = mockBitmap;
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
                    new ComponentName("com.example", "TestActivity"),
                    label,
                    mockBitmap,
                    Process.myUserHandle()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AppIcon", e);
        }
    }

    private boolean invokeDraw(Fader fader, Apps.AppIcon grabbedIcon) {
        Paint paint = new Paint();
        de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
                new de.markusfisch.android.pielauncher.graphics.CanvasPieMenu();
        return editorRenderer.draw(
                mockCanvas,
                fader,
                pieMenu,
                grabbedIcon,
                android.os.SystemClock.uptimeMillis() - 300L,
                null,
                300f,
                false, false);
    }

    @Test
    public void testDraw_FaderEditInvisible_DoesNotDraw() {
        when(mockFaderEdit.isVisible()).thenReturn(false);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(0f);

        invokeDraw(mockFaderEdit, null);

        verify(mockCanvas, never()).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
    }

    @Test
    public void testDraw_FaderEditVisible_NoGrabbedIcon_DrawsDefaultActions() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        invokeDraw(mockFaderEdit, null);

        // Without a grabbed icon, 6 action icons are drawn (twist, scale, radius, add, preferences, done)
        verify(mockCanvas, times(6)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
    }

    @Test
    public void testDraw_FaderEditVisible_WithGrabbedIcon_DrawsGrabbedIconActionsAndCircles() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        // With a grabbed icon, 3 action icons are drawn (remove, edit/hide, details)
        verify(mockCanvas, times(3)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        verify(mockCanvas, atLeastOnce()).drawCircle(anyFloat(), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testGetActionAt_WithGrabbedIcon() {
        // Assume default rect size of 100x100 for testing
        editorRenderer.iconStartRect.set(0, 0, 100, 100);
        editorRenderer.iconCenterRect.set(100, 0, 200, 100);
        editorRenderer.iconEndRect.set(200, 0, 300, 100);
        editorRenderer.iconChangeTwistRect.set(0, 100, 100, 200);
        editorRenderer.iconChangeIconScaleRect.set(100, 100, 200, 200);
        editorRenderer.iconChangeRadiusRect.set(200, 100, 300, 200);

        editorRenderer.setActionSize(100);
        boolean hasGrabbedIcon = true;
        assertEquals(EditorAction.BACK_OR_REMOVE_ICON, editorRenderer.getActionAt(new Point(50, 50), hasGrabbedIcon));
        assertEquals(EditorAction.EDIT_SETTINGS_OR_UNDO, editorRenderer.getActionAt(new Point(150, 50), hasGrabbedIcon));
        assertEquals(EditorAction.END_EDIT_MODE, editorRenderer.getActionAt(new Point(250, 50), hasGrabbedIcon));

        // Change rects are ignored when holding an icon
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(50, 150), hasGrabbedIcon));
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(150, 150), hasGrabbedIcon));
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(250, 150), hasGrabbedIcon));

        // Outside everything
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(400, 400), hasGrabbedIcon));
    }

    @Test
    public void testGetActionAt_WithoutGrabbedIcon() {
        // Assume default rect size of 100x100 for testing
        editorRenderer.iconStartRect.set(0, 0, 100, 100);
        editorRenderer.iconCenterRect.set(100, 0, 200, 100);
        editorRenderer.iconEndRect.set(200, 0, 300, 100);
        editorRenderer.iconChangeTwistRect.set(0, 100, 100, 200);
        editorRenderer.iconChangeIconScaleRect.set(100, 100, 200, 200);
        editorRenderer.iconChangeRadiusRect.set(200, 100, 300, 200);

        editorRenderer.setActionSize(100);
        boolean hasGrabbedIcon = false;
        assertEquals(EditorAction.BACK_OR_REMOVE_ICON, editorRenderer.getActionAt(new Point(50, 50), hasGrabbedIcon));
        assertEquals(EditorAction.EDIT_SETTINGS_OR_UNDO, editorRenderer.getActionAt(new Point(150, 50), hasGrabbedIcon));
        assertEquals(EditorAction.END_EDIT_MODE, editorRenderer.getActionAt(new Point(250, 50), hasGrabbedIcon));

        assertEquals(EditorAction.CHANGE_TWIST, editorRenderer.getActionAt(new Point(50, 150), hasGrabbedIcon));
        assertEquals(EditorAction.CHANGE_ICON_SCALE, editorRenderer.getActionAt(new Point(150, 150), hasGrabbedIcon));
        assertEquals(EditorAction.CHANGE_RADIUS, editorRenderer.getActionAt(new Point(250, 150), hasGrabbedIcon));

        // Outside everything
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(400, 400), hasGrabbedIcon));
    }

    @Test
    public void testDraw_InPortraitWithGrabbedIcon_DrawsDragToOrderTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1080);
        when(mockCanvas.getHeight()).thenReturn(1920);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");
        editorRenderer.setTouch(new Point(500, 500));

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas).drawText(
                org.mockito.ArgumentMatchers.eq("Drag to arrange, circle to change order"),
                org.mockito.ArgumentMatchers.eq(540f),
                org.mockito.ArgumentMatchers.anyFloat(),
                any(Paint.class));
    }

    @Test
    public void testDraw_InPortraitWithGrabbedIcon_HoveringStartRect_DrawsRemoveIconTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1080);
        when(mockCanvas.getHeight()).thenReturn(1920);

        editorRenderer.iconStartRect.set(100, 100, 200, 200);
        editorRenderer.setActionSize(100);
        editorRenderer.setTouch(new Point(150, 150));

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas).drawText(
                org.mockito.ArgumentMatchers.eq("Remove icon from pie menu"),
                org.mockito.ArgumentMatchers.eq(540f),
                org.mockito.ArgumentMatchers.anyFloat(),
                any(Paint.class));
    }

    @Test
    public void testDraw_InLandscape_DoesNotDrawTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1920);
        when(mockCanvas.getHeight()).thenReturn(1080);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas, never()).drawText(
                any(String.class),
                anyFloat(),
                anyFloat(),
                any(Paint.class));
    }
}
