package de.markusfisch.android.pielauncher.widget.renderer;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import android.text.TextPaint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.content.Apps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import de.markusfisch.android.pielauncher.widget.state.Fader;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ListRendererTest {

    @Mock
    private Canvas mockCanvas;

    @Mock
    private Fader mockFaderList;


    private ListRenderer listRenderer;
    private Bitmap mockBitmap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        listRenderer = new ListRenderer(context);
        listRenderer.layout(10, 50, 100, 10, 10, 100, 1f);
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

    private boolean invokeDraw(Fader fader, List<Apps.AppIcon> appList, int scrollY, int selectedApp) {
        return listRenderer.draw(
                mockCanvas,
                fader,
                appList,
                scrollY,
                1080,
                1920,
                null,
                null,
                false,
                selectedApp,
                0L,
                300f,
                true,
                0,
                1.0f,
                null);
    }

    @Test
    public void testDraw_FaderListInvisible_DoesNotDraw() {
        when(mockFaderList.isVisible()).thenReturn(false);
        when(mockFaderList.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(0f);

        invokeDraw(mockFaderList, Collections.emptyList(), 0, -1);

        verify(mockCanvas, never()).drawText(any(CharSequence.class), anyInt(), anyInt(), anyFloat(), anyFloat(), any(Paint.class));
        verify(mockCanvas, never()).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
    }

    @Test
    public void testDraw_FaderListVisible_DrawsIconsAndText() {
        when(mockFaderList.isVisible()).thenReturn(true);
        when(mockFaderList.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon mockIcon = createTestAppIcon("Test App");
        List<Apps.AppIcon> appList = Collections.singletonList(mockIcon);

        invokeDraw(mockFaderList, appList, 0, -1);

        verify(mockCanvas, times(1)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        verify(mockCanvas, times(1)).drawText(any(CharSequence.class), eq(0), eq(8), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testDraw_SelectedApp_DrawsLaunchFirstIcon() {
        when(mockFaderList.isVisible()).thenReturn(true);
        when(mockFaderList.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon mockIcon = createTestAppIcon("Test App");
        List<Apps.AppIcon> appList = Collections.singletonList(mockIcon);

        invokeDraw(mockFaderList, appList, 0, 0);

        // Draws launchFirst icon (float, float overload) + app icon (Rect, Rect overload)
        verify(mockCanvas, times(1)).drawBitmap(any(Bitmap.class), anyFloat(), anyFloat(), nullable(Paint.class));
        verify(mockCanvas, times(1)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
    }

    @Test
    public void testDraw_WithShortcutsHeight_AdjustsRenderNodeHeight() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when(mockFaderList.isVisible()).thenReturn(true);
            when(mockFaderList.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

            android.graphics.RenderNode mockNode = org.mockito.Mockito.mock(android.graphics.RenderNode.class);
            listRenderer.draw(
                    mockCanvas, mockFaderList, Collections.emptyList(), 0, 1080, 1920,
                    null, null, false, -1, 0L, 300f, true, 0, 1.0f, mockNode, 200);

            // maxHeight = searchInputHeight (0) + listPadding (10) + shortcutsHeight (200) = 210
            verify(mockNode).setPosition(eq(0), eq(0), eq(1080), org.mockito.AdditionalMatchers.geq(200));
        }
    }
}
