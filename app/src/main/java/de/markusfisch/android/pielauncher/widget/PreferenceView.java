package de.markusfisch.android.pielauncher.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.graphics.Ripple;

public class PreferenceView extends TextView {
	private final Ripple ripple = new Ripple();

	public PreferenceView(Context context) {
		super(context);
		initTouchListener();
	}

	public PreferenceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTouchListener();
	}

	public PreferenceView(Context context, AttributeSet attrs, int defStyleAttr) {
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

	@SuppressLint("ClickableViewAccessibility")
	private void initTouchListener() {
		setOnTouchListener((v, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				ripple.set(Math.round(event.getX()), Math.round(event.getY()));
				invalidate();
			}
			return false;
		});
	}
}
