package de.markusfisch.android.pielauncher.graphics;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import de.markusfisch.android.pielauncher.preference.Preferences;

public class Ripple {
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Point hotSpot = new Point();
	private final boolean fade;

	private long offset = 0L;
	private boolean pressed;

	public static Ripple newFadingRipple() {
		return new Ripple(0xffffffff, true);
	}

	public static Ripple newPressRipple() {
		return new Ripple(0x22ffffff, false);
	}

	public Ripple(int color, boolean fade) {
		this.fade = fade;
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
	}

	@SuppressLint("ClickableViewAccessibility")
	public View.OnTouchListener getOnTouchListener() {
		final long tapTimeout = ViewConfiguration.getTapTimeout();
		final int touchSlop = ViewConfiguration.getTouchSlop();
		final Rect viewRect = new Rect();
		return (v, event) -> {
			int x = Math.round(event.getX());
			int y = Math.round(event.getY());
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					pressed = true;
					set(x, y, tapTimeout);
					v.invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					if (SystemClock.uptimeMillis() - offset < 0 &&
							Math.hypot(x - hotSpot.x, y - hotSpot.y) >
									touchSlop) {
						cancel();
						v.invalidate();
					} else {
						v.getLocalVisibleRect(viewRect);
						if (!viewRect.contains(x, y)) {
							cancel();
							v.invalidate();
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					pressed = false;
					v.invalidate();
					break;
				case MotionEvent.ACTION_CANCEL:
					cancel();
					v.invalidate();
					break;
			}
			return false;
		};
	}

	public void set(Point point) {
		set(point.x, point.y);
	}

	public void set(int x, int y) {
		set(x, y, 0L);
	}

	public void set(int x, int y, long delay) {
		hotSpot.set(x, y);
		offset = SystemClock.uptimeMillis() + delay;
	}

	public void cancel() {
		pressed = false;
		offset = 0;
	}

	public boolean draw(Canvas canvas, Preferences prefs) {
		long duration = Math.round(prefs.getAnimationDuration());
		if (duration <= 0) {
			return false;
		}
		long delta = SystemClock.uptimeMillis() - offset;
		if (delta < 0) {
			return offset > 0;
		}
		if (delta > duration) {
			if (!fade && pressed) {
				canvas.drawColor(paint.getColor());
			}
			return false;
		}
		float maxRadius = Math.max(canvas.getWidth(), canvas.getHeight());
		float radius = Math.max(48f, maxRadius / duration * delta);
		if (fade) {
			paint.setAlpha(Math.round(128f / duration * (duration - delta)));
		}
		canvas.drawCircle(hotSpot.x, hotSpot.y, radius, paint);
		return true;
	}
}
