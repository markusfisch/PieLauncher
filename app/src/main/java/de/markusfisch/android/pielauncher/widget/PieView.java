package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;

public class PieView extends SurfaceView
{
	private static final int MAX_ICON_SIZE = 128;

	private final Runnable animationRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				while( running )
				{
					Canvas canvas = surfaceHolder.lockCanvas();

					if( canvas == null )
						continue;

					synchronized( surfaceHolder )
					{
						long now = SystemClock.elapsedRealtime();
						long elapsed = now-last;
						last = now;

						drawMenu( canvas );
					}

					surfaceHolder.unlockCanvasAndPost( canvas );
				}
			}
		};

	private volatile boolean running = false;

	private AppMenu appMenu;
	private SurfaceHolder surfaceHolder;
	private long last;
	private int width;
	private int height;
	private int radius;
	private int touchX;
	private int touchY;
	private int lastTouchX;
	private int lastTouchY;

	public PieView( Context context )
	{
		super( context );

		appMenu = new AppMenu( context );

		initSurfaceHolder();

		setZOrderOnTop( true );
		setOnTouchListener(
			new View.OnTouchListener()
			{
				@Override
				public boolean onTouch( View v, MotionEvent event )
				{
					return handleTouch( event );
				}
			} );
	}

	private void initSurfaceHolder()
	{
		surfaceHolder = getHolder();
		surfaceHolder.setFormat( PixelFormat.TRANSPARENT );
		surfaceHolder.addCallback(
			new SurfaceHolder.Callback()
			{
				private Thread thread;

				@Override
				public void surfaceChanged(
						SurfaceHolder holder,
						int format,
						int width,
						int height)
				{
					initCanvas( width, height );

					last = SystemClock.elapsedRealtime();
					running = true;

					thread = new Thread( animationRunnable );
					thread.start();
				}

				@Override
				public void surfaceCreated( SurfaceHolder holder )
				{
				}

				@Override
				public void surfaceDestroyed( SurfaceHolder holder ) {
					running = false;

					for( int retry = 100; retry-- > 0; )
					{
						try
						{
							thread.join();
							retry = 0;
						}
						catch( InterruptedException e )
						{
							// try again
						}
					}
				}
			} );
	}

	private void initCanvas( int width, int height )
	{
		int min = Math.min( width, height );

		if( Math.floor( min*.28f ) > MAX_ICON_SIZE )
			min = Math.round( MAX_ICON_SIZE/.28f );

		radius = Math.round( min*.5f );
		this.width = width;
		this.height = height;
	}

	private void drawMenu( Canvas canvas )
	{
		canvas.drawColor( 0, PorterDuff.Mode.CLEAR );

		if( touchX < 0 )
			return;

		if( touchX != lastTouchX ||
			touchY != lastTouchY )
		{
			appMenu.calculate( touchX, touchY );

			lastTouchX = touchX;
			lastTouchY = touchY;
		}

		appMenu.draw( canvas );
	}

	private boolean handleTouch( MotionEvent event )
	{
		touchX = Math.round( event.getX() );
		touchY = Math.round( event.getY() );

		switch( event.getActionMasked() )
		{
			default:
				break;
			case MotionEvent.ACTION_DOWN:
				setMenu( touchX, touchY );
				break;
			case MotionEvent.ACTION_UP:
				appMenu.launch();
				touchX = -1;
				break;
		}

		return true;
	}

	private void setMenu( int x, int y )
	{
		if( x+radius > width )
			x = width-radius;
		else if( x-radius < 0 )
			x = radius;

		if( y+radius > height )
			y = height-radius;
		else if( y-radius < 0 )
			y = radius;

		appMenu.set( x, y, radius );
	}
}
