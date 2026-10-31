package de.markusfisch.android.pielauncher.widget.shortcuts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.OverScroller;

import java.util.HashMap;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;

public class AppShortcutsController {

    public static final int MAX_VISIBLE_APP_SHORTCUTS = 4;

    private final HashMap<Apps.AppIcon, List<Apps.AppIcon>> appShortcutsCache =
            new HashMap<>();
    private final Paint paintAppShortcuts = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAppShortcutsThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint paintAppShortcutText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintList = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect appShortcutsRect = new Rect();
    private final RectF appShortcutsCard = new RectF();
    private final Path appShortcutsClip = new Path();
    private final RectF appShortcutsThumb = new RectF();
    private final Rect drawRect = new Rect();
    private final OverScroller appShortcutsScroller;
    private final int appShortcutHeight;
    private final int appShortcutIconSize;
    private final int appShortcutsVerticalPadding;
    private final int spaceBetween;
    private final int alphaAppShortcuts;
    private final int alphaAppShortcutsThumb;
    private final float maxBlurRadius;
    private final RenderNode appShortcutsBackdropNode;

    private int appShortcutsScrollY = 0;
    private int maxAppShortcutsScrollY = 0;
    private int appShortcutsScrollOffset = 0;
    private long appShortcutsExpandFrom = 0L;
    private boolean appShortcutsVisible = false;
    private boolean appShortcutsTouched = false;
    private Apps.AppIcon appShortcutsOwner;
    private List<Apps.AppIcon> appShortcuts;

    public AppShortcutsController(Context context, float dp) {
        Resources res = context.getResources();
        appShortcutsScroller = new OverScroller(context);
        appShortcutHeight = Math.round(48f * dp);
        appShortcutIconSize = Math.round(32f * dp);
        appShortcutsVerticalPadding = Math.round(8f * dp);
        spaceBetween = Math.round(4f * dp);
        maxBlurRadius = Math.round(48f * dp) * .35f;

        paintAppShortcuts.setColor(res.getColor(R.color.bg_app_shortcuts));
        paintAppShortcuts.setStyle(Paint.Style.FILL);
        alphaAppShortcuts = paintAppShortcuts.getAlpha();

        paintAppShortcutsThumb.setColor(res.getColor(R.color.separator));
        paintAppShortcutsThumb.setStyle(Paint.Style.FILL);
        alphaAppShortcutsThumb = paintAppShortcutsThumb.getAlpha();

        paintAppShortcutText.setColor(res.getColor(R.color.text_color));
        paintAppShortcutText.setTextAlign(Paint.Align.LEFT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appShortcutsBackdropNode = new RenderNode("app_shortcuts_backdrop");
            appShortcutsBackdropNode.setRenderEffect(RenderEffect.createBlurEffect(
                    maxBlurRadius, maxBlurRadius, Shader.TileMode.CLAMP));
        } else {
            appShortcutsBackdropNode = null;
        }
    }

    public void setTextSize(float sp) {
        paintAppShortcutText.setTextSize(14f * sp);
    }

    public int getAppShortcutHeight() {
        return appShortcutHeight;
    }

    public int getAppShortcutsVerticalPadding() {
        return appShortcutsVerticalPadding;
    }

    public Rect getRect() {
        return appShortcutsRect;
    }

    public int getScrollY() {
        return appShortcutsScrollY;
    }

    public boolean isVisible() {
        return appShortcutsVisible;
    }

    public void setVisible(boolean visible) {
        this.appShortcutsVisible = visible;
    }

    public boolean isTouched() {
        return appShortcutsTouched;
    }

    public void setTouched(boolean touched) {
        this.appShortcutsTouched = touched;
    }

    public long getExpandFrom() {
        return appShortcutsExpandFrom;
    }

    public void setExpandFrom(long expandFrom) {
        this.appShortcutsExpandFrom = expandFrom;
    }

    public boolean contains(int x, int y) {
        return appShortcutsRect.contains(x, y);
    }

    public int getHeight(boolean showAppShortcuts, boolean inListMode) {
        return !showAppShortcuts ||
                !inListMode ||
                appShortcuts == null ||
                appShortcuts.isEmpty()
                ? 0
                : Math.min(appShortcuts.size(), MAX_VISIBLE_APP_SHORTCUTS) *
                appShortcutHeight + appShortcutsVerticalPadding * 2;
    }

    public void update(
            Apps.AppIcon app,
            boolean isFiltered,
            boolean showAppShortcuts,
            Context context,
            Runnable onInvalidate) {
        if (!showAppShortcuts || app == null) {
            appShortcutsOwner = null;
            appShortcuts = null;
            if (!isFiltered) {
                appShortcutsCache.clear();
            }
            return;
        }
        if (app == appShortcutsOwner) {
            return;
        }
        appShortcutsOwner = app;
        appShortcuts = appShortcutsCache.get(app);
        appShortcutsScroller.forceFinished(true);
        appShortcutsScrollY = 0;
        if (appShortcuts != null) {
            return;
        }
        PieLauncherApp.apps.loadAppShortcutsAsync(context, app,
                (owner, shortcuts) -> {
                    appShortcutsCache.put(owner, shortcuts);
                    if (owner == appShortcutsOwner) {
                        appShortcuts = shortcuts;
                        if (onInvalidate != null) {
                            onInvalidate.run();
                        }
                    }
                });
    }

    public void layout(
            int viewWidth,
            int viewHeight,
            int bottomInset,
            int listPadding,
            int shortcutsHeight,
            int scrollY) {
        int bottom = viewHeight - bottomInset - listPadding;
        appShortcutsRect.set(listPadding, bottom - shortcutsHeight,
                viewWidth - listPadding, bottom);
        appShortcutsCard.set(appShortcutsRect);
        appShortcutsCard.offset(0, scrollY);
    }

    public void initScroll(Point touch, boolean isVisible) {
        appShortcutsTouched = isVisible &&
                maxAppShortcutsScrollY > 0 &&
                appShortcutsRect.contains(touch.x, touch.y);
        if (appShortcutsTouched) {
            appShortcutsScroller.forceFinished(true);
            appShortcutsScrollOffset = appShortcutsScrollY;
        }
    }

    public void scrollBy(int delta) {
        appShortcutsScrollY = clamp(appShortcutsScrollOffset + delta,
                0, maxAppShortcutsScrollY);
    }

    public void fling(int velocityY) {
        appShortcutsScroller.fling(
                0, appShortcutsScrollY,
                0, -velocityY,
                0, 0,
                0, maxAppShortcutsScrollY);
    }

    public Apps.AppIcon getShortcutAt(int x, int y) {
        if (appShortcuts == null) {
            return null;
        }
        for (Apps.AppIcon shortcut : appShortcuts) {
            if (shortcut.hitRect.contains(x, y)) {
                return shortcut;
            }
        }
        return null;
    }

    public void drawBackdrop(
            RenderNode node,
            Canvas canvas,
            int scrollY,
            int listPadding) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawRenderNode(node, canvas);
            return;
        } else if (node == null || !canvas.isHardwareAccelerated()) {
            return;
        }
        node.endRecording();

        int margin = (int) Math.ceil(maxBlurRadius * 3f);
        int left = appShortcutsRect.left - margin;
        int top = appShortcutsRect.top + scrollY - margin;
        appShortcutsBackdropNode.setPosition(left, top,
                appShortcutsRect.right + margin,
                appShortcutsRect.bottom + scrollY + margin);
        Canvas backdrop = appShortcutsBackdropNode.beginRecording();
        backdrop.translate(-left, -top);
        backdrop.drawRenderNode(node);
        appShortcutsBackdropNode.endRecording();

        appShortcutsClip.reset();
        appShortcutsClip.addRoundRect(appShortcutsCard,
                listPadding, listPadding, Path.Direction.CW);
        canvas.save();
        canvas.clipOutPath(appShortcutsClip);
        canvas.drawRenderNode(node);
        canvas.restore();
        canvas.save();
        canvas.clipPath(appShortcutsClip);
        canvas.drawRenderNode(appShortcutsBackdropNode);
        canvas.restore();
    }

    public boolean draw(
            Canvas canvas,
            float f,
            int shortcutsHeight,
            int scrollY,
            float textOffset,
            int listPadding) {
        if (appShortcuts == null) {
            return false;
        }
        int contentHeight = appShortcuts.size() * appShortcutHeight +
                appShortcutsVerticalPadding * 2;
        maxAppShortcutsScrollY = Math.max(0, contentHeight - shortcutsHeight);
        boolean invalidate = false;
        if (appShortcutsScroller.computeScrollOffset()) {
            appShortcutsScrollY = appShortcutsScroller.getCurrY();
            invalidate = true;
        }
        appShortcutsScrollY = clamp(appShortcutsScrollY, 0, maxAppShortcutsScrollY);

        float radius = listPadding;
        paintAppShortcuts.setAlpha(Math.round(f * alphaAppShortcuts));
        canvas.drawRoundRect(appShortcutsCard, radius, radius, paintAppShortcuts);

        canvas.save();
        canvas.clipRect(appShortcutsCard);
        paintAppShortcutText.setAlpha(Math.round(f * 255f));
        int x = appShortcutsRect.left + listPadding;
        int y = appShortcutsRect.top + appShortcutsVerticalPadding - appShortcutsScrollY;
        int vpad = (appShortcutHeight - appShortcutIconSize) / 2;
        int textX = x + appShortcutIconSize + listPadding;
        int maxTextWidth = appShortcutsRect.right - listPadding - textX;
        for (Apps.AppIcon shortcut : appShortcuts) {
            shortcut.hitRect.set(appShortcutsRect.left, y,
                    appShortcutsRect.right, y + appShortcutHeight);
            if (y + appShortcutHeight > appShortcutsRect.top &&
                    y < appShortcutsRect.bottom) {
                int iy = y + scrollY + vpad;
                drawRect.set(x, iy, x + appShortcutIconSize,
                        iy + appShortcutIconSize);
                canvas.drawBitmap(shortcut.bitmap, null, drawRect, paintList);
                CharSequence label = TextUtils.ellipsize(shortcut.label,
                        paintAppShortcutText, maxTextWidth,
                        TextUtils.TruncateAt.END);
                canvas.drawText(label, 0, label.length(), textX,
                        y + scrollY + appShortcutHeight / 2f + textOffset,
                        paintAppShortcutText);
            }
            y += appShortcutHeight;
        }
        canvas.restore();

        if (maxAppShortcutsScrollY > 0) {
            int trackTop = appShortcutsRect.top + appShortcutsVerticalPadding + scrollY;
            int trackHeight = shortcutsHeight - appShortcutsVerticalPadding * 2;
            int thumbHeight = trackHeight * shortcutsHeight / contentHeight;
            int thumbTop = trackTop + (trackHeight - thumbHeight) *
                    appShortcutsScrollY / maxAppShortcutsScrollY;
            float thumbRight = appShortcutsRect.right - listPadding / 2f;
            appShortcutsThumb.set(thumbRight - spaceBetween, thumbTop,
                    thumbRight, thumbTop + thumbHeight);
            paintAppShortcutsThumb.setAlpha(Math.round(f * alphaAppShortcutsThumb));
            canvas.drawRoundRect(appShortcutsThumb, spaceBetween,
                    spaceBetween, paintAppShortcutsThumb);
        }
        return invalidate;
    }

    private static void drawRenderNode(RenderNode node, Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                node != null &&
                canvas.isHardwareAccelerated()) {
            node.endRecording();
            canvas.drawRenderNode(node);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // Testing helpers
    void setShortcutsForTesting(Apps.AppIcon owner, List<Apps.AppIcon> shortcuts) {
        this.appShortcutsOwner = owner;
        this.appShortcuts = shortcuts;
    }

    void setMaxScrollYForTesting(int maxScrollY) {
        this.maxAppShortcutsScrollY = maxScrollY;
    }
}
