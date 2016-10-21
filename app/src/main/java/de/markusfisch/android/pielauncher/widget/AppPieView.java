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
	private static final int MAX_ICON_SIZE = 128;

	private final Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			while (running) {
				Canvas canvas = surfaceHolder.lockCanvas();

				if (canvas == null) {
					continue;
				}

				synchronized (surfaceHolder) {
					drawMenu(canvas);
				}

				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	};

	private volatile boolean running = false;

	private final SurfaceHolder surfaceHolder;

	private AppMenu appMenu;
	private int width;
	private int height;
	private int radius;
	private int touchX;
	private int touchY;
	private int lastTouchX;
	private int lastTouchY;

	public AppPieView(final Context context) {
		super(context);

		appMenu = new AppMenu(context);

		surfaceHolder = getHolder();
		initSurfaceHolder(surfaceHolder);

		setZOrderOnTop(true);
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
						break;
					case MotionEvent.ACTION_DOWN:
						setMenu(touchX, touchY);
						break;
					case MotionEvent.ACTION_UP:
						v.performClick();
						appMenu.launch(context);
						touchX = -1;
						break;
				}

				return true;
			}
		});
	}

	private void initSurfaceHolder(SurfaceHolder holder) {
		holder.setFormat(PixelFormat.TRANSPARENT);
		holder.addCallback(new SurfaceHolder.Callback() {
			private Thread thread;

			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				initCanvas(width, height);

				running = true;

				thread = new Thread(animationRunnable);
				thread.start();
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				running = false;

				for (int retry = 100; retry-- > 0; ) {
					try {
						thread.join();
						retry = 0;
					} catch (InterruptedException e) {
						// try again
					}
				}
			}
		});
	}

	private void initCanvas(int width, int height) {
		int min = Math.min(width, height);

		if (Math.floor(min * .28f) > MAX_ICON_SIZE) {
			min = Math.round(MAX_ICON_SIZE / .28f);
		}

		radius = Math.round(min * .5f);
		this.width = width;
		this.height = height;
	}

	private void drawMenu(Canvas canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);

		if (touchX < 0) {
			return;
		}

		/*View rootView = ((Activity) getContext())
				.getWindow()
				.getDecorView()
				.getRootView();
		rootView.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
		rootView.setDrawingCacheEnabled(false);*/

		if (touchX != lastTouchX || touchY != lastTouchY) {
			appMenu.calculate(touchX, touchY);

			lastTouchX = touchX;
			lastTouchY = touchY;
		}

		appMenu.draw(canvas);
	}

	private void setMenu(int x, int y) {
		if (x + radius > width) {
			x = width - radius;
		} else if (x - radius < 0) {
			x = radius;
		}

		if (y + radius > height) {
			y = height - radius;
		} else if (y - radius < 0) {
			y = radius;
		}

		appMenu.set(x, y, radius);
	}
}
