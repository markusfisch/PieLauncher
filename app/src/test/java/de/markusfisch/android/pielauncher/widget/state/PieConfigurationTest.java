package de.markusfisch.android.pielauncher.widget.state;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.graphics.PieMenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PieConfigurationTest {

    private PieConfiguration config;

    @Before
    public void setUp() {
        config = new PieConfiguration();
        // Setup layout with viewMin = 400, dp = 2.0 (400 * 0.28 = 112 <= 64 * 2.0 = 128)
        config.layout(400, 2.0f);
    }

    @Test
    public void layoutCalculatesRadiiCorrectly() {
        // maxRadius = viewMin * 0.5 = 400 * 0.5 = 200
        // minRadius = maxRadius * 0.5 = 100
        // medRadius = minRadius + (maxRadius - minRadius) / 2 = 150
        assertEquals(200, config.getMaxRadius());
        assertEquals(100, config.getMinRadius());
        assertEquals(150, config.getMedRadius());
    }

    @Test
    public void layoutCapsViewMinWhenExceedingMaxIconSize() {
        PieConfiguration cappedConfig = new PieConfiguration();
        // viewMin = 800, dp = 2.0f -> maxIconSize = 128, 800 * 0.28 = 224 > 128
        // capped viewMin = Math.round(128 / 0.28f) = 457
        // maxRadius = Math.round(457 * 0.5f) = 229
        cappedConfig.layout(800, 2.0f);
        assertEquals(229, cappedConfig.getMaxRadius());
    }

    @Test
    public void radiusCyclesInOrder() {
        // Default radius initialized in layout
        config.setRadius(config.getMinRadius());
        config.cycleRadius();
        assertEquals(config.getMedRadius(), config.getRadius());

        config.cycleRadius();
        assertEquals(config.getMaxRadius(), config.getRadius());

        config.cycleRadius();
        assertEquals(config.getMinRadius(), config.getRadius());
    }

    @Test
    public void radiusClampsWithinMinAndMax() {
        assertEquals(config.getMinRadius(), config.clampRadius(50));
        assertEquals(config.getMaxRadius(), config.clampRadius(9999));
        assertEquals(150, config.clampRadius(150));
    }

    @Test
    public void getDrawableForRadiusReturnsCorrectResources() {
        assertEquals(R.drawable.ic_radius_small, config.getDrawableForRadius(config.getMinRadius()));
        assertEquals(R.drawable.ic_radius_medium, config.getDrawableForRadius(config.getMedRadius()));
        assertEquals(R.drawable.ic_radius_large, config.getDrawableForRadius(config.getMaxRadius()));
    }

    @Test
    public void twistSegmentsAndCycling() {
        config.setTwist(0f);
        assertEquals(0, PieConfiguration.getTwistSegment(0));
        assertEquals(R.drawable.ic_twist_0, PieConfiguration.getDrawableForTwist(0f));

        config.cycleTwist();
        assertEquals(1, PieConfiguration.getTwistSegment(config.getTwist()));
        assertEquals(R.drawable.ic_twist_90, PieConfiguration.getDrawableForTwist(config.getTwist()));

        config.cycleTwist();
        assertEquals(2, PieConfiguration.getTwistSegment(config.getTwist()));
        assertEquals(R.drawable.ic_twist_180, PieConfiguration.getDrawableForTwist(config.getTwist()));

        config.cycleTwist();
        assertEquals(3, PieConfiguration.getTwistSegment(config.getTwist()));
        assertEquals(R.drawable.ic_twist_270, PieConfiguration.getDrawableForTwist(config.getTwist()));

        config.cycleTwist();
        assertEquals(0, PieConfiguration.getTwistSegment(config.getTwist()));
    }

    @Test
    public void iconScaleCyclesBetweenOneAndMinScale() {
        config.setIconScale(1.0f);
        assertEquals(R.drawable.ic_icon_scale_large, PieConfiguration.getDrawableForIconScale(1.0f));

        config.cycleIconScale();
        assertEquals(config.getMinIconScale(), config.getIconScale(), 0.001f);
        assertEquals(R.drawable.ic_icon_scale_small, PieConfiguration.getDrawableForIconScale(config.getMinIconScale()));

        config.cycleIconScale();
        assertEquals(1.0f, config.getIconScale(), 0.001f);
    }

    @Test
    public void splitPieDrawables() {
        assertEquals(R.drawable.ic_screen_lower, PieConfiguration.getDrawableForSplitPie(false));
        assertEquals(R.drawable.ic_screen_upper, PieConfiguration.getDrawableForSplitPie(true));
    }
}
