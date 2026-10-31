package de.markusfisch.android.pielauncher.widget.renderer;

import android.graphics.Canvas;
import android.graphics.Point;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import de.markusfisch.android.pielauncher.widget.state.Fader;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PieRendererTest {

    @Mock
    private Canvas mockCanvas;

    @Mock
    private CanvasPieMenu mockPieMenu;

    @Mock
    private Fader mockFaderPie;

    @Mock
    private Preferences mockPrefs;

    private PieRenderer pieRenderer;
    private Point touch;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        touch = new Point(500, 500);
        pieRenderer = new PieRenderer(mockPieMenu, mockFaderPie, mockPrefs);
    }

    @Test
    public void testDraw_FaderPieInvisible_DoesNotDraw() {
        pieRenderer.draw(mockCanvas, touch, 0.0f, 300f, false);

        verify(mockPieMenu, never()).draw(any());
        verify(mockPieMenu, never()).calculate(anyFloat(), anyFloat());
    }

    @Test
    public void testDraw_FaderPieVisible_CalculatesAndDraws() {
        pieRenderer.draw(mockCanvas, touch, 1.0f, 300f, false);

        verify(mockPieMenu, times(1)).calculate(anyFloat(), anyFloat(), anyFloat(), any());
        verify(mockPieMenu, times(1)).draw(mockCanvas);
    }

    @Test
    public void testDraw_FaderPieFadingOut_DrawsPie() {
        pieRenderer.draw(mockCanvas, touch, 0.5f, 300f, false);

        verify(mockPieMenu, times(1)).calculate(anyFloat(), anyFloat(), anyFloat(), any());
        verify(mockPieMenu, times(1)).draw(mockCanvas);
    }

    @Test
    public void testDraw_WithLaunchingIcon_PassesLaunchingIconToCalculate() {
        de.markusfisch.android.pielauncher.content.Apps.AppIcon icon =
                org.mockito.Mockito.mock(de.markusfisch.android.pielauncher.content.Apps.AppIcon.class);
        pieRenderer.setLaunchingIcon(icon);

        pieRenderer.draw(mockCanvas, touch, 0.8f, 300f, false);

        verify(mockPieMenu).calculate(anyFloat(), anyFloat(), anyFloat(), org.mockito.ArgumentMatchers.eq(icon));
    }

    @Test
    public void testDraw_CircleTransition_AnimatesBetweenMenus() {
        when(mockPrefs.animateInOut()).thenReturn(true);
        when(mockPieMenu.getCenterX()).thenReturn(500);
        when(mockPieMenu.getCenterY()).thenReturn(500);

        de.markusfisch.android.pielauncher.content.Apps.AppIcon icon = createTestAppIcon("App");
        java.util.List<de.markusfisch.android.pielauncher.content.Apps.AppIcon> icons =
                java.util.Collections.singletonList(icon);

        long start = 1000L;
        pieRenderer.startCircleTransition(1, icons, start);

        // Halfway through animation (t = 0.5)
        boolean needsInvalidate = pieRenderer.draw(mockCanvas, touch, 1.0f, start + 150L, 300f, false);

        org.junit.Assert.assertTrue("Should require invalidation during transition", needsInvalidate);
        verify(mockCanvas, times(2)).save();
        verify(mockCanvas, times(2)).restore();
        verify(mockPieMenu, times(1)).draw(mockCanvas);
    }

    private de.markusfisch.android.pielauncher.content.Apps.AppIcon createTestAppIcon(String label) {
        try {
            java.lang.reflect.Constructor<de.markusfisch.android.pielauncher.content.Apps.AppIcon> constructor =
                    de.markusfisch.android.pielauncher.content.Apps.AppIcon.class.getDeclaredConstructor(
                            android.content.ComponentName.class,
                            String.class,
                            android.graphics.Bitmap.class,
                            android.os.UserHandle.class
                    );
            constructor.setAccessible(true);
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888);
            return constructor.newInstance(
                    new android.content.ComponentName("pkg", "cls"),
                    label,
                    bmp,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
