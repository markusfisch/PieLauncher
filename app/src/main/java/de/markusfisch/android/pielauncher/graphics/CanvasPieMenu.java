package de.markusfisch.android.pielauncher.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class CanvasPieMenu extends PieMenu {
	private static final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

	public static class CanvasIcon extends PieMenu.Icon {
		public final Rect dst = new Rect();
		public final Bitmap bitmap;

		public CanvasIcon(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		void draw(Canvas canvas) {
			int s = (int) size >> 1;
			if (s < 1) {
				return;
			}
			int left = x - s;
			int top = y - s;
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
	}

	public synchronized void draw(Canvas canvas) {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).draw(canvas);
		}
	}
}
