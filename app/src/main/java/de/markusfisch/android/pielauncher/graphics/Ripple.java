package de.markusfisch.android.pielauncher.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.SystemClock;

public class Ripple {
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Point hotSpot = new Point();

	private long duration = 300L;
	private long offset = 0L;

	public Ripple() {
		this(0xffffffff);
	}

	public Ripple(int color) {
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void set(Point point) {
		set(point.x, point.y);
	}

	public void set(int x, int y) {
		hotSpot.set(x, y);
		offset = SystemClock.uptimeMillis();
	}

	public void cancel() {
		offset = 0;
	}

	public boolean draw(Canvas canvas) {
		long delta = SystemClock.uptimeMillis() - offset;
		if (delta < 0 || delta > duration) {
			return false;
		}
		float maxRadius = Math.max(canvas.getWidth(), canvas.getHeight());
		float radius = Math.max(48f, maxRadius / duration * delta);
		paint.setAlpha(Math.round(128f / duration * (duration - delta)));
		canvas.drawCircle(hotSpot.x, hotSpot.y, radius, paint);
		return true;
	}
}
