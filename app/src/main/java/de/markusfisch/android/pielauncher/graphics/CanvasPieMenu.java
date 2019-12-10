package de.markusfisch.android.pielauncher.graphics;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class CanvasPieMenu extends PieMenu {
	public static class CanvasIcon extends PieMenu.Icon {
		public final Drawable icon;

		public CanvasIcon(Drawable icon) {
			this.icon = icon;
		}

		public void draw(Canvas canvas) {
			int s = (int) size >> 1;
			if (s < 1) {
				return;
			}
			int left = x - s;
			int top = y - s;
			s <<= 1;
			icon.setBounds(left, top, left + s, top + s);
			icon.draw(canvas);
		}
	}

	public void draw(Canvas canvas) {
		for (int n = icons.size(); n-- > 0; ) {
			((CanvasIcon) icons.get(n)).draw(canvas);
		}
	}
}
