package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class ScrollWithListenerView extends ScrollView {
	public interface OnScrollPositionListener {
		void onScroll(int y, boolean scrollable);
	}

	private OnScrollPositionListener onScrollPositionListener;
	private boolean scrollable = false;

	public ScrollWithListenerView(Context context) {
		super(context);
	}

	public ScrollWithListenerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ScrollWithListenerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setOnScrollPositionListener(OnScrollPositionListener listener) {
		onScrollPositionListener = listener;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			// Give Android some time to settle down before running this,
			// not putting it on the queue makes it only work sometimes.
			post(() -> {
				View child = getChildAt(0);
				if (child == null) {
					return;
				}
				scrollable = getHeight() < child.getHeight() +
						getPaddingTop() + getPaddingBottom();
				if (onScrollPositionListener != null) {
					onScrollPositionListener.onScroll(
							getScrollY(), scrollable);
				}
			});
		}
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		super.onScrollChanged(x, y, oldx, oldy);
		if (onScrollPositionListener != null) {
			onScrollPositionListener.onScroll(y, scrollable);
		}
	}
}
