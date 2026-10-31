package de.markusfisch.android.pielauncher.widget.renderer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.widget.state.Fader;
import de.markusfisch.android.pielauncher.widget.state.EditorAction;

public class EditorRenderer {

    public final Rect iconStartRect = new Rect();
    public final Rect iconCenterRect = new Rect();
    public final Rect iconEndRect = new Rect();
    public final Rect iconChangeTwistRect = new Rect();
    public final Rect iconChangeIconScaleRect = new Rect();
    public final Rect iconChangeRadiusRect = new Rect();

    private final Paint paintDropZone;
    private final Paint paintPressed;
    private final Paint paintAction;
    private final Paint paintText;
    private final int alphaDropZone;
    private final int alphaPressed;
    private final int alphaText;

    private final String dragToOrderTip;
    private final String removeIconTip;
    private final String editAppTip;
    private final String hideAppTip;
    private final String removeAppTip;

    private final Point touch = new Point();
    private boolean hasTouch = false;
    private int controlsPadding;
    private float textOffset;

    private final Bitmap iconRemove;
    private final Bitmap iconEdit;
    private final Bitmap iconHide;
    private final Bitmap iconDetails;
    public Bitmap iconChangeTwist;
    public Bitmap iconChangeIconScale;
    public Bitmap iconChangeRadius;
    public Bitmap iconAdd;
    public Bitmap iconSplitPie;
    public Bitmap iconPreferences;
    public Bitmap iconDone;

    private float actionSize;
    private float actionSizeSq;

    public EditorRenderer(Context context) {
        Resources res = context.getResources();

        paintDropZone = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDropZone.setColor(res.getColor(R.color.bg_drop_zone));
        paintDropZone.setStyle(Paint.Style.FILL);
        alphaDropZone = paintDropZone.getAlpha();

        paintPressed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPressed.setColor(res.getColor(R.color.bg_action_pressed));
        paintPressed.setStyle(Paint.Style.FILL);
        alphaPressed = paintPressed.getAlpha();

        paintAction = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAction.setFilterBitmap(true);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(res.getColor(R.color.text_color));
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(14f * res.getDisplayMetrics().scaledDensity);
        alphaText = paintText.getAlpha();

        dragToOrderTip = context.getString(R.string.tip_drag_to_order);
        removeIconTip = context.getString(R.string.tip_remove_icon);
        editAppTip = context.getString(R.string.change_icon);
        hideAppTip = context.getString(R.string.hide_app);
        removeAppTip = context.getString(R.string.tip_remove_app);

        iconAdd = Converter.getBitmapFromDrawable(res, R.drawable.ic_add);
        iconEdit = Converter.getBitmapFromDrawable(res, R.drawable.ic_edit);
        iconHide = Converter.getBitmapFromDrawable(res, R.drawable.ic_hide);
        iconRemove = Converter.getBitmapFromDrawable(res, R.drawable.ic_remove);
        iconDetails = Converter.getBitmapFromDrawable(res, R.drawable.ic_details);
        iconDone = Converter.getBitmapFromDrawable(res, R.drawable.ic_done);
        iconPreferences = Converter.getBitmapFromDrawable(res, R.drawable.ic_preferences);
    }


    public boolean draw(
            Canvas canvas,
            Fader fadeEdit,
            CanvasPieMenu pieMenu,
            Apps.AppIcon grabbedIcon,
            long grabbedIconAt,
            Rect highlightedAction,
            float animationDuration,
            boolean hasSecondaryMenu,
            boolean hasPacks) {

        long now = SystemClock.uptimeMillis();
        float f = fadeEdit.get(now, animationDuration);
        if (f <= 0) {
            return false;
        }

        int alpha = Math.round(f * 255f);
        CanvasPieMenu.paint.setAlpha(alpha);
        paintDropZone.setAlpha(Math.round(f * alphaDropZone));
        paintPressed.setAlpha(Math.round(f * alphaPressed));
        paintAction.setAlpha(alpha);
        paintText.setAlpha(Math.round(f * alphaText));

        boolean hasIcon = grabbedIcon != null;
        boolean invalidate = f < 1f;

        // Only draw tips in portrait orientation.
        // There's probably not enough space in landscape.
        if (canvas.getWidth() < canvas.getHeight()) {
            drawTip(canvas, getTip(hasIcon, hasPacks));
        }

        if (hasIcon) {
            float r = actionSize;
            if (animationDuration > 0) {
                float t = Math.min(1f, (now - grabbedIconAt) / animationDuration);
                if (t < 1f) {
                    invalidate = true;
                }
                r *= t;
            }
            Rect activeHighlight = highlightedAction;
            if (hasTouch) {
                if (contains(iconStartRect, touch)) {
                    activeHighlight = iconStartRect;
                } else if (contains(iconCenterRect, touch)) {
                    activeHighlight = iconCenterRect;
                } else if (contains(iconEndRect, touch)) {
                    activeHighlight = iconEndRect;
                } else {
                    activeHighlight = null;
                }
            }
            drawAction(canvas, iconRemove, iconStartRect, r, activeHighlight);
            drawAction(canvas, hasPacks ? iconEdit : iconHide, iconCenterRect, r, activeHighlight);
            drawAction(canvas, iconDetails, iconEndRect, r, activeHighlight);
        } else {
            drawAction(canvas, iconChangeTwist, iconChangeTwistRect, 0f, highlightedAction);
            drawAction(canvas, iconChangeIconScale, iconChangeIconScaleRect, 0f, highlightedAction);
            drawAction(canvas, iconChangeRadius, iconChangeRadiusRect, 0f, highlightedAction);
            drawAction(canvas, iconAdd, iconStartRect, 0f, highlightedAction);
            drawAction(canvas, hasSecondaryMenu ? iconSplitPie : iconPreferences, iconCenterRect, 0f, highlightedAction);
            drawAction(canvas, iconDone, iconEndRect, 0f, highlightedAction);
        }

        return invalidate;
    }

    private String getTip(boolean hasIcon, boolean hasPacks) {
        if (hasIcon) {
            if (hasTouch) {
                if (contains(iconStartRect, touch)) {
                    return removeIconTip;
                } else if (contains(iconCenterRect, touch)) {
                    return hasPacks ? editAppTip : hideAppTip;
                } else if (contains(iconEndRect, touch)) {
                    return removeAppTip;
                }
            }
            return dragToOrderTip;
        }
        return null;
    }

    private void drawTip(Canvas canvas, String tip) {
        if (tip != null) {
            canvas.drawText(tip, canvas.getWidth() >> 1,
                    controlsPadding + textOffset, paintText);
        }
    }

    private void drawAction(Canvas canvas, Bitmap icon, Rect rect, float radius, Rect highlightedAction) {
        boolean pressed = rect == highlightedAction;
        Paint circlePaint = null;
        if (radius > 0) {
            circlePaint = pressed ? paintPressed : paintDropZone;
        } else if (pressed) {
            radius = actionSize;
            circlePaint = paintPressed;
        }
        if (circlePaint != null) {
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius, circlePaint);
        }
        canvas.drawBitmap(icon, null, rect, paintAction);
    }


    public void setTouch(Point touch) {
        if (touch != null) {
            this.touch.set(touch.x, touch.y);
            this.hasTouch = true;
        } else {
            this.hasTouch = false;
        }
    }

    public void setTextDimensions(int controlsPadding, float textOffset, float sp) {
        this.controlsPadding = controlsPadding;
        this.textOffset = textOffset;
        paintText.setTextSize(14f * sp);
    }

    public void setActionSize(int actionSize) {
        this.actionSize = actionSize;
        this.actionSizeSq = actionSize * actionSize;
    }

    public float getActionSize() {
        return actionSize;
    }

    public void layout(
            int viewWidth,
            int viewHeight,
            int controlsPadding,
            float textOffset,
            float sp,
            int spaceBetween,
            int iconSize) {
        boolean portrait = viewHeight > viewWidth;
        int max = distributeButtons(
                iconAdd,
                iconPreferences,
                iconDone,
                iconStartRect,
                iconCenterRect,
                iconEndRect,
                -controlsPadding,
                viewWidth,
                viewHeight,
                portrait);

        distributeButtons(
                iconChangeTwist,
                iconChangeIconScale,
                iconChangeRadius,
                iconChangeTwistRect,
                iconChangeIconScaleRect,
                iconChangeRadiusRect,
                controlsPadding,
                viewWidth,
                viewHeight,
                portrait);

        setActionSize(Math.min(iconSize, max / 2 - spaceBetween));
        setTextDimensions(controlsPadding, textOffset, sp);
    }

    private int distributeButtons(
            Bitmap icon1, Bitmap icon2, Bitmap icon3,
            Rect rect1, Rect rect2, Rect rect3,
            int base,
            int viewWidth,
            int viewHeight,
            boolean portrait) {
        int w1 = icon1 != null ? icon1.getWidth() : 0;
        int h1 = icon1 != null ? icon1.getHeight() : 0;
        int w2 = icon2 != null ? icon2.getWidth() : 0;
        int h2 = icon2 != null ? icon2.getHeight() : 0;
        int w3 = icon3 != null ? icon3.getWidth() : 0;
        int h3 = icon3 != null ? icon3.getHeight() : 0;

        rect1.set(0, 0, w1, h1);
        rect2.set(0, 0, w2, h2);
        rect3.set(0, 0, w3, h3);

        int totalWidth = w1 + w2 + w3;
        int totalHeight = h1 + h2 + h3;
        int largestWidth = Math.max(w1, Math.max(w2, w3));
        int largestHeight = Math.max(h1, Math.max(h2, h3));

        int spaces = 4;
        int max;
        if (portrait) {
            int step = Math.round((float) (viewWidth - totalWidth) / spaces);
            max = largestWidth + step;
            int x = step;
            int y = (base > 0 ? base : viewHeight + base) - largestHeight / 2;

            rect1.offset(x, y);
            x += step + rect1.width();

            rect2.offset(x, y);
            x += step + rect2.width();

            rect3.offset(x, y);
        } else {
            int step = Math.round((float) (viewHeight - totalHeight) / spaces);
            max = largestHeight + step;
            int x = (base > 0 ? base : viewWidth + base) - largestWidth / 2;
            int y = step;

            rect1.offset(x, y);
            y += step + rect1.height();

            rect2.offset(x, y);
            y += step + rect2.height();

            rect3.offset(x, y);
        }
        return max;
    }

    public EditorAction getActionAt(Point touch, boolean hasGrabbedIcon) {
        if (!hasGrabbedIcon) {
            if (contains(iconChangeTwistRect, touch)) {
                return EditorAction.CHANGE_TWIST;
            } else if (contains(iconChangeIconScaleRect, touch)) {
                return EditorAction.CHANGE_ICON_SCALE;
            } else if (contains(iconChangeRadiusRect, touch)) {
                return EditorAction.CHANGE_RADIUS;
            }
        }

        if (contains(iconStartRect, touch)) {
            return EditorAction.BACK_OR_REMOVE_ICON;
        } else if (contains(iconCenterRect, touch)) {
            return EditorAction.EDIT_SETTINGS_OR_UNDO;
        } else if (contains(iconEndRect, touch)) {
            return EditorAction.END_EDIT_MODE;
        }

        return EditorAction.NONE;
    }

    private boolean contains(Rect rect, Point point) {
        return distSq(rect.centerX(), rect.centerY(), point.x, point.y) <
                actionSizeSq;
    }

    private static float distSq(int ax, int ay, int bx, int by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }
}
