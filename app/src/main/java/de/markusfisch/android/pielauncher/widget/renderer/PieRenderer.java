package de.markusfisch.android.pielauncher.widget.renderer;

import android.graphics.Canvas;
import android.graphics.Point;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.state.Fader;

public class PieRenderer {
    private final CanvasPieMenu pieMenu;
    private final Fader fadePie;
    private final Preferences prefs;
    private final ArrayList<CanvasPieMenu.CanvasIcon> outgoingPieIcons = new ArrayList<>();

    private int circleDirection;
    private long lastCircleAt = 0;
    public Apps.AppIcon launchingIcon;

    public PieRenderer(CanvasPieMenu pieMenu, Fader fadePie, Preferences prefs) {
        this.pieMenu = pieMenu;
        this.fadePie = fadePie;
        this.prefs = prefs;
    }

    public void setLastCircleAt(long time) {
        this.lastCircleAt = time;
    }

    public void startCircleTransition(int direction, List<Apps.AppIcon> currentIcons, long now) {
        outgoingPieIcons.clear();
        for (Apps.AppIcon icon : currentIcons) {
            CanvasPieMenu.CanvasIcon snapshot = new CanvasPieMenu.CanvasIcon(icon.bitmap);
            snapshot.x = icon.x;
            snapshot.y = icon.y;
            snapshot.size = icon.size;
            outgoingPieIcons.add(snapshot);
        }
        this.circleDirection = direction;
        this.lastCircleAt = now;
    }

    public void setLaunchingIcon(Apps.AppIcon icon) {
        this.launchingIcon = icon;
    }

    public Apps.AppIcon getLaunchingIcon() {
        return launchingIcon;
    }

    public int getSelectedIcon() {
        return pieMenu.getSelectedIcon();
    }

    public boolean draw(Canvas canvas, Point touch, float fPie, float ad, boolean inEditMode) {
        return draw(canvas, touch, fPie, SystemClock.uptimeMillis(), ad, inEditMode);
    }

    public boolean draw(Canvas canvas, Point touch, float fPie, long now, float ad, boolean inEditMode) {
        float f = fPie;
        if (inEditMode) {
            f = 1f;
        }
        if (f <= 0) {
            outgoingPieIcons.clear();
            return false;
        }

        boolean animateInOut = prefs.animateInOut();
        f = easeSlowerOut(f);

        if (!inEditMode) {
            pieMenu.calculate(touch.x, touch.y, animateInOut ? f : 1f, launchingIcon);

            boolean switching = !outgoingPieIcons.isEmpty() && animateInOut && ad > 0;
            float t = switching ? Math.max(0f, (now - lastCircleAt) / ad) : 1f;
            if (t < 1f) {
                drawCircleTransition(canvas, t, f);
            } else {
                outgoingPieIcons.clear();
                CanvasPieMenu.paint.setAlpha(Math.round(f * 255f));
                pieMenu.draw(canvas);
            }
            return f < 1f || t < 1f;
        } else {
            CanvasPieMenu.paint.setAlpha(Math.round(f * 255f));
            return pieMenu.drawSmoothed(canvas);
        }
    }

    private void drawCircleTransition(Canvas canvas, float t, float f) {
        int cx = pieMenu.getCenterX();
        int cy = pieMenu.getCenterY();
        float progress = easeSlowerOut(t);
        float angle = circleDirection * 25f;
        float scale = 1f - .15f * progress;
        canvas.save();
        canvas.rotate(angle * progress, cx, cy);
        canvas.scale(scale, scale, cx, cy);
        CanvasPieMenu.paint.setAlpha(Math.round(f * (1f - progress) * 255f));
        for (int i = outgoingPieIcons.size(); i-- > 0; ) {
            CanvasPieMenu.CanvasIcon icon = outgoingPieIcons.get(i);
            icon.draw(canvas, icon.size, icon.x, icon.y);
        }
        canvas.restore();

        float x = t - 1f;
        float spring = 1f + x * x * (2.5f * x + 1.5f);
        scale = .85f + .15f * spring;
        canvas.save();
        canvas.rotate(-angle * (1f - progress), cx, cy);
        canvas.scale(scale, scale, cx, cy);
        CanvasPieMenu.paint.setAlpha(Math.round(f * progress * 255f));
        pieMenu.draw(canvas);
        canvas.restore();
    }

    private static float easeSlowerOut(float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 1.4011f * t3 * t2
                - 5.3789f * t2 * t2
                + 7.2185f * t3
                - 4.2505f * t2
                + 2.0098f * t;
    }
}
