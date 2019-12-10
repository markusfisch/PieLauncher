package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;

public class AppPieView extends SurfaceView {
	public interface OpenListListener {
		void onOpenList();
	}

	public static final AppMenu appMenu = new AppMenu();

	private final Point touch = new Point();
	private final Point lastTouch = new Point();
	private final float dp;
	private final SurfaceHolder surfaceHolder;

	private int width;
	private int height;
	private int radius;
	private float touchSlopSq;
	private int tapTimeout;
	private OpenListListener listListener;
	private boolean editMode = false;

	public AppPieView(Context context, AttributeSet attr) {
		super(context, attr);

		dp = context.getResources().getDisplayMetrics().density;
		surfaceHolder = getHolder();
		appMenu.indexAppsAsync(context);

		ViewConfiguration vc = ViewConfiguration.get(context);
		float touchSlop = vc.getScaledTouchSlop();
		touchSlopSq = touchSlop * touchSlop;
		tapTimeout = vc.getTapTimeout();

		initSurfaceHolder(surfaceHolder);
		initTouchListener();

		setZOrderOnTop(true);
	}

	public void setOpenListListener(OpenListListener listener) {
		listListener = listener;
	}

	public void addIconInteractive(AppMenu.AppIcon appIcon, Point from) {
		int centerX = width >> 1;
		int centerY = height >> 1;
		double step = AppMenu.TAU / (appMenu.icons.size() + 1);
		double a = Math.atan2(from.y - centerY, from.x - centerX);
		a = (a + AppMenu.TAU) % AppMenu.TAU;
		appMenu.icons.add((int) Math.floor(a / step), appIcon);
		editMode = true;
		touch.set(from.x, from.y);
		setCenter(centerX, centerY);
		drawView();
	}

	private void initSurfaceHolder(SurfaceHolder holder) {
		holder.setFormat(PixelFormat.TRANSPARENT);
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				initMenu(width, height);
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}
		});
	}

	private void initMenu(int width, int height) {
		int min = Math.min(width, height);
		float maxIconSize = 64f * dp;
		if (Math.floor(min * .28f) > maxIconSize) {
			min = Math.round(maxIconSize / .28f);
		}
		radius = Math.round(min * .5f);
		this.width = width;
		this.height = height;
	}

	private void initTouchListener() {
		setOnTouchListener(new View.OnTouchListener() {
			private Point down = new Point();
			private long downAt;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				touch.x = Math.round(event.getX());
				touch.y = Math.round(event.getY());
				switch (event.getActionMasked()) {
					default:
						break;
					case MotionEvent.ACTION_CANCEL:
						touch.x = -1;
						drawView();
						break;
					case MotionEvent.ACTION_MOVE:
						drawView();
						break;
					case MotionEvent.ACTION_DOWN:
						editMode = false;
						down.set(touch.x, touch.y);
						downAt = event.getEventTime();
						setCenter(touch);
						drawView();
						break;
					case MotionEvent.ACTION_UP:
						v.performClick();
						if (!editMode) {
							if (SystemClock.uptimeMillis() - downAt <= tapTimeout &&
									distSq(down, touch) <= touchSlopSq) {
								if (listListener != null) {
									listListener.onOpenList();
								}
							} else {
								appMenu.launch(v.getContext());
							}
						}
						touch.x = -1;
						drawView();
						break;
				}
				return true;
			}
		});
	}

	private void setCenter(Point point) {
		setCenter(point.x, point.y);
	}

	private void setCenter(int x, int y) {
		appMenu.set(
				Math.max(radius, Math.min(width - radius, x)),
				Math.max(radius, Math.min(height - radius, y)),
				radius);
	}

	private void drawView() {
		if (touch.equals(lastTouch)) {
			return;
		}
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		if (touch.x > -1) {
			appMenu.calculate(touch.x, touch.y);
			appMenu.draw(canvas);
		}
		lastTouch.set(touch.x, touch.y);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private static float distSq(Point a, Point b) {
		float dx = a.x - b.x;
		float dy = a.y - b.y;
		return dx*dx + dy*dy;
	}
}
