package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ScrollWithListenerView extends ScrollView {
	public interface OnScrollPositionListener {
		void onScroll(int y);
	}

	private OnScrollPositionListener onScrollPositionListener;

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
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		super.onScrollChanged(x, y, oldx, oldy);
		if (onScrollPositionListener != null) {
			onScrollPositionListener.onScroll(y);
		}
	}
}
