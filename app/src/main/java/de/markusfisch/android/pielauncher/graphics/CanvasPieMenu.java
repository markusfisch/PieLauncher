package de.markusfisch.android.pielauncher.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class CanvasPieMenu extends PieMenu {
	private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

	public static class CanvasIcon extends PieMenu.Icon {
		private final Rect dst = new Rect();
		private final Bitmap bitmap;

		public CanvasIcon(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		public void draw(Canvas canvas) {
			int s = (int) size >> 1;
			if (s < 1) {
				return;
			}
			int left = x - s;
			int top = y - s;
			s <<= 1;
			dst.set(left, top, left + s, top + s);
			canvas.drawBitmap(bitmap, null, dst, paint);
		}
	}

	public void draw(Canvas canvas) {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).draw(canvas);
		}
	}
}
