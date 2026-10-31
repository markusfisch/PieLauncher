package de.markusfisch.android.pielauncher.widget.state;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FaderTest {

    private Fader fader;

    @Before
    public void setUp() {
        fader = new Fader();
    }

    @Test
    public void initialState() {
        assertFalse(fader.isVisible());
        assertFalse(fader.isFadingIn(1000L));
        assertFalse(fader.isFadingOut(1000L));
        assertEquals(0f, fader.get(1000L, 300f), 0.001f);
    }

    @Test
    public void fadeInProgressesAccordingToDuration() {
        long start = 1000L;
        float duration = 200f;

        fader.fadeIn(start);
        assertTrue(fader.isVisible());

        // At start (t = 0), easing gives 0
        assertEquals(0f, fader.get(start, duration), 0.001f);
        assertTrue(fader.isFadingIn(start + 50L));

        // Halfway (t = 0.5), easeSlowerIn gives 0.5 * 0.5 = 0.25
        assertEquals(0.25f, fader.get(start + 100L, duration), 0.001f);

        // At end (t = 1.0), value reaches 1.0
        assertEquals(1.0f, fader.get(start + 200L, duration), 0.001f);
        assertFalse(fader.isFadingIn(start + 200L));

        // Beyond duration, clamped to 1.0
        assertEquals(1.0f, fader.get(start + 300L, duration), 0.001f);
    }

    @Test
    public void fadeOutProgressesAccordingToDuration() {
        long start = 1000L;
        float duration = 200f;

        fader.fadeIn(start);
        fader.get(start + 200L, duration); // reach 1.0

        long fadeOutStart = 2000L;
        fader.fadeOut(fadeOutStart);

        assertFalse(fader.isVisible());
        assertTrue(fader.isFadingOut(fadeOutStart + 50L));

        // Halfway through fade out (t = 0.5)
        // delta = 100, x = 0.5, easeSlowerOut = 1 - (0.5)^2 = 0.75
        // minOut = 1.0 - 0.75 = 0.25
        assertEquals(0.25f, fader.get(fadeOutStart + 100L, duration), 0.001f);

        // Beyond duration, returns 0
        assertEquals(0f, fader.get(fadeOutStart + 200L, duration), 0.001f);
        assertFalse(fader.isFadingOut(fadeOutStart - 10L));
    }

    @Test
    public void fadeOutWhenNotVisibleIsNoOp() {
        fader.fadeOut(1000L);
        assertFalse(fader.isVisible());
        assertFalse(fader.isFadingOut(1050L));
        assertEquals(0f, fader.get(1050L, 200f), 0.001f);
    }

    @Test
    public void zeroDurationReturnsImmediately() {
        fader.fadeIn(1000L);
        assertEquals(1.0f, fader.get(1000L, 0f), 0.001f);

        fader.fadeOut(2000L);
        assertEquals(0f, fader.get(2000L, 0f), 0.001f);
    }

    @Test
    public void setMaxInCapsFadeOutStartLevel() {
        fader.fadeIn(1000L);
        fader.get(1200L, 200f); // fully faded in to 1.0

        // When user drags list partially down, hideList sets maxIn to dragProgress
        fader.setMaxIn(0.5f);
        fader.fadeOut(2000L);

        // Fade out starts from capped maxIn (0.5)
        assertEquals(0.5f, fader.get(2000L, 200f), 0.001f);
    }
}
