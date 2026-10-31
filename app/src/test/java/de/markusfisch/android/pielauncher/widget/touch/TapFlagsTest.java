package de.markusfisch.android.pielauncher.widget.touch;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TapFlagsTest {

    @Test
    public void defaultFlagsAreFalse() {
        TapFlags flags = new TapFlags();
        assertFalse(flags.wasTap);
        assertFalse(flags.doubleTap);
        assertFalse(flags.twoFingerTap);
        assertFalse(flags.fourFingerTap);
        assertFalse(flags.longPress);
    }

    @Test
    public void flagsCanBeSetIndependently() {
        TapFlags flags = new TapFlags();
        flags.wasTap = true;
        flags.longPress = true;

        assertTrue(flags.wasTap);
        assertTrue(flags.longPress);
        assertFalse(flags.doubleTap);
        assertFalse(flags.twoFingerTap);
        assertFalse(flags.fourFingerTap);
    }
}
