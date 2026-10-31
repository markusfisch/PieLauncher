package de.markusfisch.android.pielauncher.widget.renderer;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.widget.state.Fader;

public class ListRenderer {

    private final TextPaint paintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintList = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final ColorMatrixColorFilter dimFilter;
    private final Bitmap iconLaunchFirst;
    private final int iconLaunchFirstHalf;
    private final Rect drawRect = new Rect();

    private int listPadding;
    private int searchInputHeight;
    private int iconSize;
    private int spaceBetween;
    private int iconTextPadding;
    private float textHeight;
    private float textOffset;
    private int listFadeHeight;

    public ListRenderer(Context context) {
        Resources res = context.getResources();

        paintText.setColor(res.getColor(R.color.text_color));
        paintText.setTextAlign(Paint.Align.CENTER);

        ColorMatrix colorMatrix = new ColorMatrix();
        float dimAmount = .5f;
        colorMatrix.setScale(dimAmount, dimAmount, dimAmount, 1);
        dimFilter = new ColorMatrixColorFilter(colorMatrix);

        iconLaunchFirst = Converter.getBitmapFromDrawable(res, R.drawable.ic_launch_first);
        iconLaunchFirstHalf = iconLaunchFirst.getWidth() >> 1;
    }

    public TextPaint getPaintText() {
        return paintText;
    }

    public float getTextOffset() {
        return textOffset;
    }

    public float getTextHeight() {
        return textHeight;
    }

    public void layout(int listPadding, int searchInputHeight, int iconSize, int spaceBetween, int iconTextPadding, int listFadeHeight, float sp) {
        this.listPadding = listPadding;
        this.searchInputHeight = searchInputHeight;
        this.iconSize = iconSize;
        this.spaceBetween = spaceBetween;
        this.iconTextPadding = iconTextPadding;
        this.listFadeHeight = listFadeHeight;
        paintText.setTextSize(14f * sp);
        this.textHeight = paintText.descent() - paintText.ascent();
        this.textOffset = (this.textHeight / 2) - paintText.descent();
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeList,
            List<Apps.AppIcon> appList,
            int scrollY,
            int viewWidth,
            int viewHeight,
            Apps.AppIcon touchedIcon,
            Apps.AppIcon highlightedIcon,
            boolean isDraggingDownList,
            int selectedApp,
            long highlightedFrom,
            float animationDuration,
            boolean showAppNames,
            int listAppearanceAnimation,
            float dragProgress,
            android.graphics.RenderNode listRenderNode) {
        return draw(canvas, fadeList, appList, scrollY, viewWidth, viewHeight,
                touchedIcon, highlightedIcon, isDraggingDownList, selectedApp,
                highlightedFrom, animationDuration, showAppNames,
                listAppearanceAnimation, dragProgress, listRenderNode, 0);
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeList,
            List<Apps.AppIcon> appList,
            int scrollY,
            int viewWidth,
            int viewHeight,
            Apps.AppIcon touchedIcon,
            Apps.AppIcon highlightedIcon,
            boolean isDraggingDownList,
            int selectedApp,
            long highlightedFrom,
            float animationDuration,
            boolean showAppNames,
            int listAppearanceAnimation,
            float dragProgress,
            android.graphics.RenderNode listRenderNode,
            int shortcutsHeight) {

        long now = SystemClock.uptimeMillis();
        float f = Math.min(fadeList.get(now, animationDuration), dragProgress);
        if (f <= 0) {
            return false;
        }

        int innerWidth = viewWidth - listPadding * 2;
        int columns = Math.min(5, innerWidth / (iconSize + spaceBetween));
        int iconAndTextHeight = iconSize + (showAppNames ? iconTextPadding + Math.round(textHeight) : 0);
        int cellWidth = innerWidth / columns;
        int cellHeight = iconAndTextHeight + iconTextPadding * 2;
        int wrapX = listPadding + cellWidth * columns;
        int size = appList.size();

        int maxTextWidth = cellWidth - iconTextPadding;
        int hpad = (cellWidth - iconSize) / 2;
        int vpad = (cellHeight - iconAndTextHeight) / 2;
        int labelX = cellWidth >> 1;
        int labelY = cellHeight - vpad - Math.round(textOffset);
        int viewTop = scrollY - cellHeight;
        int viewBottom = scrollY + viewHeight;
        int x = listPadding;
        int y = searchInputHeight + listPadding;

        if (listAppearanceAnimation == 1 || isDraggingDownList) {
            y += Math.round((1f - f) * listFadeHeight);
        }

        paintList.setAlpha(Math.round(f * 255f));
        paintText.setAlpha(Math.round(f * 255f));

        if (selectedApp > -1 && size > 0) {
            int offset = Math.min(selectedApp, size - 1);
            int ix = x + offset % columns * cellWidth;
            int iy = y + offset / columns * cellHeight;
            canvas.drawBitmap(iconLaunchFirst, ix + labelX - iconLaunchFirstHalf, iy - listPadding, paintList);
        }

        int magSize = Math.round(Math.max(cellWidth, cellHeight) * .2f);
        boolean invalidate = f < 1f;
        if (highlightedFrom > 0) {
            if (animationDuration > 0) {
                float t = Math.min(1f, (now - highlightedFrom) / animationDuration);
                if (t < 1f) {
                    invalidate = true;
                }
                magSize = Math.round(magSize * t);
            } else {
                magSize = 0;
            }
        }

        for (int i = 0; i < size; ++i) {
            if (y > viewTop && y < viewBottom) {
                Apps.AppIcon appIcon = appList.get(i);
                appIcon.hitRect.set(x, y, x + cellWidth, y + cellHeight);
                int ix = x + hpad;
                int iy = y + vpad;
                int mag = appIcon == highlightedIcon ? magSize : 0;
                drawRect.set(ix - mag, iy - mag, ix + iconSize + mag, iy + iconSize + mag);
                if (mag == 0 && appIcon == touchedIcon) {
                    paintList.setColorFilter(dimFilter);
                }
                canvas.drawBitmap(appIcon.bitmap, null, drawRect, paintList);
                paintList.setColorFilter(null);
                if (showAppNames) {
                    CharSequence label = TextUtils.ellipsize(appIcon.label, paintText, maxTextWidth, TextUtils.TruncateAt.END);
                    canvas.drawText(label, 0, label.length(), x + labelX, y + labelY, paintText);
                }
            }
            x += cellWidth;
            if (x >= wrapX) {
                x = listPadding;
                y += cellHeight;
            }
        }

        int maxHeight = y + listPadding + (x > listPadding ? cellHeight : 0);
        maxHeight += shortcutsHeight;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && listRenderNode != null) {
            listRenderNode.setPosition(0, 0, viewWidth, maxHeight);
        }

        return invalidate;
    }
}

