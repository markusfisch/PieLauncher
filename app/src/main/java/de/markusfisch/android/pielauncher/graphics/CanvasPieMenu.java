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
		public final Rect dst = new Rect();
		public final Bitmap bitmap;

		private double smoothedSize;
		private int smoothedX;
		private int smoothedY;

		public CanvasIcon(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		void drawSmoothed(Canvas canvas, float t, float lastT) {
			double ds = size - smoothedSize;
			int dx = x - smoothedX;
			int dy = y - smoothedY;
			float dt = t - lastT;
			float rt = 1f - lastT;
			float f = rt == 0 ? 1 : dt / rt;
			smoothedSize += ds * f;
			smoothedX += dx * f;
			smoothedY += dy * f;
			draw(canvas, smoothedSize, smoothedX, smoothedY);
		}

		void draw(Canvas canvas) {
			draw(canvas, size, x, y);
		}

		void draw(Canvas canvas, double iconSize, int centerX, int centerY) {
			int s = (int) iconSize >> 1;
			if (s < 1) {
				return;
			}
			int left = centerX - s;
			int top = centerY - s;
			s <<= 1;
			draw(canvas, left, top, left + s, top + s);
		}

		void draw(Canvas canvas, int left, int top, int right, int bottom) {
			dst.set(left, top, right, bottom);
			draw(canvas, dst);
		}

		void draw(Canvas canvas, Rect dst) {
			canvas.drawBitmap(bitmap, null, dst, paint);
		}

		private void initSmoothing() {
			smoothedSize = size;
			smoothedX = x;
			smoothedY = y;
		}
	}

	public synchronized void draw(Canvas canvas) {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).draw(canvas);
		}
	}

	public synchronized boolean drawSmoothed(Canvas canvas) {
		long delta = SystemClock.uptimeMillis() - lastChange;
		float t = Math.min(1f, delta / 200f);
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).drawSmoothed(canvas, t, lastT);
		}
		lastT = t;
		return t < 1f;
	}

	public synchronized void updateSmoothing() {
		lastChange = SystemClock.uptimeMillis();
		lastT = 0;
	}

	public synchronized void initSmoothing() {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).initSmoothing();
		}
	}
}
