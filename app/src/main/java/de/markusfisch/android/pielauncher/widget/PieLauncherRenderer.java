package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.view.MotionEvent;

public class PieLauncherRenderer
{
	private static final int MAX_ICON_SIZE = 128;

	private AppMenu appMenu;
	private int width = 0;
	private int height = 0;
	private int centerX = -1;
	private int centerY = -1;
	private float touchX = 0;
	private float touchY = 0;
	private float lastTouchX = -1;
	private float lastTouchY = -1;

	public PieLauncherRenderer( AppMenu m )
	{
		appMenu = m;
	}

	public void setup( int w, int h )
	{
		int min = Math.min( w, h );

		if( Math.floor( min*.28f ) > MAX_ICON_SIZE )
			min = Math.round( MAX_ICON_SIZE/.28f );

		int radius = Math.round( min*.5f );

		width = w;
		height = h;
		touchX = centerX = w/2;
		touchY = centerY = h/2;

		appMenu.setup( centerX, centerY, radius );
	}

	public void draw( Canvas canvas, long e )
	{
		if( width < 1 )
			return;

		canvas.drawColor( 0x00000000, PorterDuff.Mode.CLEAR );

		if( touchX != lastTouchX ||
			touchY != lastTouchY )
		{
			appMenu.calculate( touchX, touchY );

			lastTouchX = touchX;
			lastTouchY = touchY;
		}

		for( int n = appMenu.numberOfIcons; n-- > 0; )
			((AppMenu.Icon)appMenu.icons.get( n )).draw( canvas );
	}

	public void touch( MotionEvent e )
	{
		touchX = e.getX();
		touchY = e.getY();

		switch( e.getActionMasked() )
		{
			default:
				break;
			case MotionEvent.ACTION_UP:
				appMenu.fire();
				break;
		}
	}
}
