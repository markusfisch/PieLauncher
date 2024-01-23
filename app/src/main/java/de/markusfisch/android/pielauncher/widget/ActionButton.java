package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

import de.markusfisch.android.pielauncher.graphics.Ripple;

public class ActionButton extends ImageView {
	private final Ripple ripple = Ripple.newPressRipple();

	public ActionButton(Context context) {
		super(context);
		initTouchListener();
	}

	public ActionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTouchListener();
	}

	public ActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initTouchListener();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (ripple.draw(canvas)) {
			invalidate();
		}
	}

	private void initTouchListener() {
		setOnTouchListener(ripple.getOnTouchListener());
	}
}
