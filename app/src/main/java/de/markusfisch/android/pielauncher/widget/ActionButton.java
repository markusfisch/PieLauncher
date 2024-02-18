package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.Ripple;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class ActionButton extends ImageView {
	private final Ripple ripple = Ripple.newPressRipple();

	private Preferences prefs;

	public ActionButton(Context context) {
		super(context);
		init(context);
	}

	public ActionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (ripple.draw(canvas, prefs)) {
			invalidate();
		}
	}

	private void init(Context context) {
		prefs = PieLauncherApp.getPrefs(context);
		setOnTouchListener(ripple.getOnTouchListener());
	}
}
