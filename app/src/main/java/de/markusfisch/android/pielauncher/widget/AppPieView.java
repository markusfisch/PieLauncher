package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;

public class AppPieView extends SurfaceView {
	public static final AppMenu appMenu = new AppMenu();

	private final Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				drawView();
			}
		}
	};

	private final float dp;
	private final SurfaceHolder surfaceHolder;

	private int width;
	private int height;
	private int radius;
	private int touchX;
	private int touchY;
	private int lastTouchX;
	private int lastTouchY;
	private Thread animationThread;

	public AppPieView(Context context) {
		super(context);

		dp = context.getResources().getDisplayMetrics().density;
		surfaceHolder = getHolder();
		appMenu.indexAppsAsync(context);

		initSurfaceHolder(surfaceHolder);
		initTouchListener();

		setZOrderOnTop(true);
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
				stopAnimationThread();
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
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				touchX = Math.round(event.getX());
				touchY = Math.round(event.getY());
				switch (event.getActionMasked()) {
					default:
						break;
					case MotionEvent.ACTION_CANCEL:
						touchX = -1;
						drawView();
						break;
					case MotionEvent.ACTION_MOVE:
						drawView();
						break;
					case MotionEvent.ACTION_DOWN:
						setCenter(touchX, touchY);
						drawView();
						break;
					case MotionEvent.ACTION_UP:
						v.performClick();
						appMenu.launch(v.getContext());
						touchX = -1;
						drawView();
						break;
				}
				return true;
			}
		});
	}

	private void setCenter(int x, int y) {
		appMenu.set(
				Math.max(radius, Math.min(width - radius, x)),
				Math.max(radius, Math.min(height - radius, y)),
				radius);
	}

	private void drawView() {
		if (touchX == lastTouchX && touchY == lastTouchY) {
			return;
		}
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		if (touchX > -1) {
			appMenu.calculate(touchX, touchY);
			appMenu.draw(canvas);
		}
		lastTouchX = touchX;
		lastTouchY = touchY;
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private void startAnimationThread() {
		stopAnimationThread();
		animationThread = new Thread(animationRunnable);
		animationThread.start();
	}

	private void stopAnimationThread() {
		if (animationThread == null) {
			return;
		}
		animationThread.interrupt();
		try {
			animationThread.join();
		} catch (InterruptedException e) {
			// parent thread was interrupted
		}
	}
}
