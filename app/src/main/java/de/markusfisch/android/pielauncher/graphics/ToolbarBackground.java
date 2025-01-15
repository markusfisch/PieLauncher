package de.markusfisch.android.pielauncher.graphics;

import android.content.res.Resources;

import de.markusfisch.android.pielauncher.R;

public class ToolbarBackground {
	public final int backgroundColor;

	private final float threshold;

	public ToolbarBackground(Resources res) {
		backgroundColor = res.getColor(R.color.bg_search_bar);
		threshold = res.getDisplayMetrics().density * 48f;
	}

	public int getColorForY(int y) {
		y = Math.abs(y);
		return y > 0
				? fadeColor(backgroundColor, y / threshold)
				: 0;
	}

	private static int fadeColor(int argb, float fraction) {
		fraction = Math.max(0f, Math.min(1f, fraction));
		int alpha = (argb >> 24) & 0xff;
		int rgb = argb & 0xffffff;
		return rgb | Math.round(fraction * alpha) << 24;
	}
}
