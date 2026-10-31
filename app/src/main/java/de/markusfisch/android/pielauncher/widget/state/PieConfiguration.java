package de.markusfisch.android.pielauncher.widget.state;

import android.content.res.Resources;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.graphics.PieMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;

public class PieConfiguration {

    private int minRadius;
    private int medRadius;
    private int maxRadius;
    private int radius;
    private float twist;
    private float minIconScale;
    private float iconScale = 1f;

    public void layout(int viewMin, float dp) {
        float maxIconSize = 64f * dp;
        if (Math.floor(viewMin * .28f) > maxIconSize) {
            viewMin = Math.round(maxIconSize / .28f);
        }

        maxRadius = Math.round(viewMin * .5f);
        minRadius = Math.round(maxRadius * .5f);
        medRadius = minRadius + (maxRadius - minRadius) / 2;
        minIconScale = (48f * dp) / maxIconSize;
    }

    public int getMinRadius() {
        return minRadius;
    }

    public int getMedRadius() {
        return medRadius;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public float getTwist() {
        return twist;
    }

    public void setTwist(float twist) {
        this.twist = twist;
    }

    public float getMinIconScale() {
        return minIconScale;
    }

    public float getIconScale() {
        return iconScale;
    }

    public void setIconScale(float iconScale) {
        this.iconScale = iconScale;
    }

    public int clampRadius(int r) {
        return Math.max(minRadius, Math.min(maxRadius, r));
    }

    public void loadPreferences(Preferences prefs, boolean primary) {
        radius = clampRadius(prefs.getRadius(primary, maxRadius));
        twist = prefs.getTwist(primary);
        iconScale = prefs.getIconScale(primary);
    }

    public void savePreferences(Preferences prefs, boolean primary) {
        prefs.setRadius(primary, radius);
        prefs.setTwist(primary, twist);
        prefs.setIconScale(primary, iconScale);
    }

    public void applyTo(CanvasPieMenu<?> pieMenu) {
        pieMenu.setRadiusTwistIconScale(radius, twist, iconScale);
    }

    public void cycleTwist() {
        twist = getTwistSegment(twist + PieMenu.HALF_PI) * (float) PieMenu.HALF_PI;
    }

    public void cycleIconScale() {
        iconScale = iconScale < 1f ? 1f : minIconScale;
    }

    public void cycleRadius() {
        radius = getNextRadius(radius);
    }

    public int getNextRadius(int r) {
        switch (getRadiusSegment(r)) {
            case 1:
                return maxRadius;
            case 2:
                return minRadius;
            default:
                return medRadius;
        }
    }

    public int getRadiusSegment(int r) {
        int dMin = Math.abs(r - minRadius);
        int dMed = Math.abs(r - medRadius);
        int dMax = Math.abs(r - maxRadius);
        if (dMin < dMed && dMin < dMax) {
            return 0;
        } else if (dMed < dMin && dMed < dMax) {
            return 1;
        }
        return 2;
    }

    public static int getTwistSegment(double rad) {
        rad = (rad + PieMenu.TAU) % PieMenu.TAU;
        if (rad > 5.497 || rad < .785) {
            return 0;
        } else if (rad < 2.356) {
            return 1;
        } else if (rad < 3.926) {
            return 2;
        }
        return 3;
    }

    public static int getDrawableForTwist(float twist) {
        switch (getTwistSegment(twist)) {
            case 1:
                return R.drawable.ic_twist_90;
            case 2:
                return R.drawable.ic_twist_180;
            case 3:
                return R.drawable.ic_twist_270;
            default:
                return R.drawable.ic_twist_0;
        }
    }

    public static int getDrawableForIconScale(float scale) {
        return scale == 1f
                ? R.drawable.ic_icon_scale_large
                : R.drawable.ic_icon_scale_small;
    }

    public int getDrawableForRadius(int r) {
        switch (getRadiusSegment(r)) {
            case 1:
                return R.drawable.ic_radius_medium;
            case 2:
                return R.drawable.ic_radius_large;
            default:
                return R.drawable.ic_radius_small;
        }
    }

    public static int getDrawableForSplitPie(boolean isSecondary) {
        return isSecondary
                ? R.drawable.ic_screen_upper
                : R.drawable.ic_screen_lower;
    }

    public void updateChangeIcons(Resources res, EditorRenderer editorRenderer) {
        updateChangeTwistIcon(res, editorRenderer);
        updateChangeIconScaleIcon(res, editorRenderer);
        updateChangeRadiusIcon(res, editorRenderer);
    }

    public void updateChangeTwistIcon(Resources res, EditorRenderer editorRenderer) {
        editorRenderer.iconChangeTwist = Converter.getBitmapFromDrawable(res,
                getDrawableForTwist(twist));
    }

    public void updateChangeIconScaleIcon(Resources res, EditorRenderer editorRenderer) {
        editorRenderer.iconChangeIconScale = Converter.getBitmapFromDrawable(res,
                getDrawableForIconScale(iconScale));
    }

    public void updateChangeRadiusIcon(Resources res, EditorRenderer editorRenderer) {
        editorRenderer.iconChangeRadius = Converter.getBitmapFromDrawable(res,
                getDrawableForRadius(radius));
    }

    public void updateSplitPieIcon(Resources res, EditorRenderer editorRenderer, boolean isSecondary) {
        editorRenderer.iconSplitPie = Converter.getBitmapFromDrawable(res,
                getDrawableForSplitPie(isSecondary));
    }
}
