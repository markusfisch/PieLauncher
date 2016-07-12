package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;

public class PieLauncherSurfaceView
	extends SurfaceView
	implements Runnable, View.OnTouchListener
{
	private PieLauncherRenderer renderer;
	private SurfaceHolder surfaceHolder;
	private volatile boolean running = false;
	private Thread thread;
	private long time;

	public PieLauncherSurfaceView( AppMenu appMenu )
	{
		super( appMenu.getContext() );

		setZOrderOnTop( true );

		surfaceHolder = getHolder();
		surfaceHolder.setFormat( PixelFormat.TRANSPARENT );

		renderer = new PieLauncherRenderer( appMenu );

		setOnTouchListener( this );
	}

	public void onResume()
	{
		time = SystemClock.elapsedRealtime();
		running = true;

		thread = new Thread( this );
		thread.start();
	}

	public void onPause()
	{
		running = false;

		for( boolean retry = true; retry; )
		{
			try
			{
				thread.join();
				retry = false;
			}
			catch( InterruptedException e )
			{
			}
		}
	}

	@Override
	public boolean onTouch( View v, MotionEvent e )
	{
		renderer.touch( e );
		return true;
	}

	@Override
	public void run()
	{
		for( boolean initialized = false; running; )
		{
			if( !surfaceHolder.getSurface().isValid() )
				continue;

			Canvas canvas = surfaceHolder.lockCanvas();

			if( !initialized )
			{
				renderer.setup(
					canvas.getWidth(),
					canvas.getHeight() );

				initialized = true;
			}

			long now = SystemClock.elapsedRealtime();
			long elapsed = now-time;

			renderer.draw( canvas, elapsed );
			time = now;

			surfaceHolder.unlockCanvasAndPost( canvas );
		}
	}
}
