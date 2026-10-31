package de.markusfisch.android.pielauncher.widget.state;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.PieMenu;

public class PieEditor {

    public interface Listener {
        void invalidate();

        void storeMenu();

        void releaseIcon();
    }

    private final Listener listener;
    private final CanvasPieMenu<Apps.AppIcon> pieMenu;

    private int lastCenterX;
    private int lastCenterY;

    private final List<Apps.AppIcon> ungrabbedIcons = new ArrayList<>();
    private Apps.AppIcon grabbedIcon;
    private long grabbedIconAt;
    private int lastInsertAt;
    private double twist;
    private Point touch;

    private final List<Apps.AppIcon> backup = new ArrayList<>();

    public PieEditor(Listener listener, CanvasPieMenu<Apps.AppIcon> pieMenu) {
        this.listener = listener;
        this.pieMenu = pieMenu;
    }

    public void beginEdit(List<Apps.AppIcon> currentIcons) {
        backup.clear();
        backup.addAll(currentIcons);
    }

    public void commit() {
        backup.clear();
    }

    public void rollback(List<Apps.AppIcon> currentIcons) {
        currentIcons.clear();
        currentIcons.addAll(backup);
        backup.clear();
    }

    private boolean neverDropped = false;

    public void setNeverDropped(boolean neverDropped) {
        this.neverDropped = neverDropped;
    }

    public boolean isNeverDropped() {
        return neverDropped;
    }

    public boolean isMenuChanged(List<? extends PieMenu.Icon> currentIcons) {
        int size = backup.size();
        if (size != currentIcons.size()) {
            return true;
        }
        if (size == 0) {
            return false;
        }
        PieMenu.Icon icon = backup.get(0);
        int i;
        for (i = 0; i < size; ++i) {
            if (currentIcons.get(i) == icon) {
                break;
            }
        }
        if (i >= size) {
            return true;
        }
        for (int j = 1; j < size; ++j) {
            if (backup.get(j) != currentIcons.get(++i % size)) {
                return true;
            }
        }
        return false;
    }

    public void setTouch(Point touch) {
        this.touch = touch;
    }

    public void setTwist(double twist) {
        this.twist = twist;
    }

    public Apps.AppIcon getGrabbedIcon() {
        return grabbedIcon;
    }

    public long getGrabbedIconAt() {
        return grabbedIconAt;
    }

    public int getLastInsertAt() {
        return lastInsertAt;
    }

    public List<Apps.AppIcon> getUngrabbedIcons() {
        return ungrabbedIcons;
    }

    public boolean hasGrabbedIcon() {
        return grabbedIcon != null;
    }

    public void editIcon(Apps.AppIcon icon) {
        grabbedIcon = icon;
        grabbedIconAt = android.os.SystemClock.uptimeMillis();
        ungrabbedIcons.clear();
        ungrabbedIcons.addAll(pieMenu.icons);

        if (grabbedIcon != null) {
            ungrabbedIcons.remove(grabbedIcon);
            lastInsertAt = -1;
        } else {
            pieMenu.icons.clear();
            pieMenu.icons.addAll(ungrabbedIcons);
        }
        listener.invalidate();
    }

    public void resetGrabbedItem() {
        if (grabbedIcon == null) {
            return;
        }
        pieMenu.icons.clear();
        pieMenu.icons.addAll(ungrabbedIcons);
        pieMenu.icons.add(grabbedIcon);
        grabbedIcon = null;
        pieMenu.calculate(lastCenterX, lastCenterY);
        listener.invalidate();
    }

    public void storeMenu() {
        listener.storeMenu();
    }

    public void commitGrabbedItem() {
        if (grabbedIcon == null) {
            return;
        }
        listener.storeMenu();
        grabbedIcon = null;
        pieMenu.calculate(lastCenterX, lastCenterY);
        listener.invalidate();
    }

    public void calculateEditablePie(int centerX, int centerY) {
        if (grabbedIcon == null || touch == null) {
            return;
        }
        lastCenterX = centerX;
        lastCenterY = centerY;
        int endIndex = ungrabbedIcons.size();
        double step = PieMenu.TAU / (endIndex + 1);
        double angle = PieMenu.getPositiveAngle(Math.atan2(
                touch.y - centerY,
                touch.x - centerX) - twist + step * .5);
        int insertAt = Math.min(endIndex, (int) Math.floor(angle / step));
        if (insertAt != lastInsertAt) {
            if (lastInsertAt == 0 && insertAt == endIndex) {
                Collections.rotate(ungrabbedIcons, 1);
            } else if (lastInsertAt == endIndex && insertAt == 0) {
                Collections.rotate(ungrabbedIcons, -1);
            }

            pieMenu.icons.clear();
            pieMenu.icons.addAll(ungrabbedIcons);
            pieMenu.icons.add(insertAt, grabbedIcon);
            pieMenu.updateSmoothing();

            if (lastInsertAt < 0) {
                pieMenu.calculate(centerX, centerY);
                grabbedIcon.x = touch.x;
                grabbedIcon.y = touch.y;
                pieMenu.initSmoothing();
            }
            lastInsertAt = insertAt;
        }

        pieMenu.calculate(touch.x, touch.y);
        grabbedIcon.x = touch.x;
        grabbedIcon.y = touch.y;
    }
}
