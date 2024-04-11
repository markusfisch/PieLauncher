package de.markusfisch.android.pielauncher.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class Converter {
	// Limit the icon size, as some apps incorrectly use too large resources
	// for their icon.
	private static final int MAX_ICON_SIZE = 256;

	public static Bitmap getBitmapFromDrawable(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
			if (max <= MAX_ICON_SIZE) {
				return bitmap;
			}
			float f = (float) MAX_ICON_SIZE / max;
			return Bitmap.createScaledBitmap(
					bitmap,
					Math.round(bitmap.getWidth() * f),
					Math.round(bitmap.getHeight() * f),
					true);
		}
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(
				Math.min(MAX_ICON_SIZE, width > 0 ? width : 48),
				Math.min(MAX_ICON_SIZE, height > 0 ? height : 48),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap getBitmapFromDrawable(Resources res, int resId) {
		return getBitmapFromDrawable(getDrawable(res, resId));
	}

	public static Drawable getDrawable(Resources res, int resId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return res.getDrawable(resId, null);
		} else {
			//noinspection deprecation
			return res.getDrawable(resId);
		}
	}
}
