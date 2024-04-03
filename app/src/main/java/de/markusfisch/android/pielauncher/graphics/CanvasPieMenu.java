package de.markusfisch.android.pielauncher.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;

public class CanvasPieMenu extends PieMenu {
	public static final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

	private long lastChange;
	private float lastT;

	public static class CanvasIcon extends PieMenu.Icon {
		public final Rect rect = new Rect();
		public final Bitmap bitmap;

		private double smoothedSize;
		private int smoothedX;
		private int smoothedY;

		public CanvasIcon(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		void draw(Canvas canvas) {
			draw(canvas, size, x, y);
		}

		void drawSmoothed(Canvas canvas, float t, float lastT) {
			double ds = size - smoothedSize;
			int dx = x - smoothedX;
			int dy = y - smoothedY;
			float dt = t - lastT;
			float rt = 1f - lastT;
			float f = rt == 0 ? 1 : dt / rt;
			smoothedSize += ds * f;
			smoothedX += Math.round(dx * f);
			smoothedY += Math.round(dy * f);
			draw(canvas, smoothedSize, smoothedX, smoothedY);
		}

		private void draw(Canvas canvas, double iconSize, int centerX,
				int centerY) {
			int s = (int) iconSize >> 1;
			if (s < 1) {
				return;
			}
			int left = centerX - s;
			int top = centerY - s;
			s <<= 1;
			rect.set(left, top, left + s, top + s);
			canvas.drawBitmap(bitmap, null, rect, paint);
		}

		private void initSmoothing() {
			smoothedSize = size;
			smoothedX = x;
			smoothedY = y;
		}
	}

	public void draw(Canvas canvas) {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).draw(canvas);
		}
	}

	public boolean drawSmoothed(Canvas canvas) {
		long delta = SystemClock.uptimeMillis() - lastChange;
		float t = Math.min(1f, delta / 200f);
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).drawSmoothed(canvas, t, lastT);
		}
		lastT = t;
		return t < 1f;
	}

	public void updateSmoothing() {
		lastChange = SystemClock.uptimeMillis();
		lastT = 0;
	}

	public void initSmoothing() {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).initSmoothing();
		}
	}
}
