package de.markusfisch.android.pielauncher.widget;

import de.markusfisch.android.pielauncher.content.AppMenu;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.view.MotionEvent;

public class PieLauncherRenderer
{
	private static final int MAX_ICON_SIZE = 128;

	private AppMenu appMenu;
	private int width = -1;
	private int height = -1;
	private int centerX = -1;
	private int centerY = -1;
	private int radius = -1;
	private float touchX = 0;
	private float touchY = 0;
	private float lastTouchX = -1;
	private float lastTouchY = -1;

	public PieLauncherRenderer( AppMenu m )
	{
		appMenu = m;
	}

	public void setup( int width, int height )
	{
		int min = Math.min( width, height );

		if( Math.floor( min*.28f ) > MAX_ICON_SIZE )
			min = Math.round( MAX_ICON_SIZE/.28f );

		/*centerX = width/2;
		centerY = height/2;*/
		radius = Math.round( min*.5f );

		this.width = width;
		this.height = height;

		//appMenu.setup( centerX, centerY, radius );
	}

	public void draw( Canvas canvas, long e )
	{
		canvas.drawColor( 0x00000000, PorterDuff.Mode.CLEAR );

		if( centerX < 0 ||
			radius < 0 )
			return;

		if( touchX != lastTouchX ||
			touchY != lastTouchY )
		{
			appMenu.calculate( touchX, touchY );

			lastTouchX = touchX;
			lastTouchY = touchY;
		}

		for( int n = appMenu.getNumberOfIcons(); n-- > 0; )
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
			case MotionEvent.ACTION_DOWN:
				centerX = Math.round( touchX );
				centerY = Math.round( touchY );

				if( centerX+radius > width )
					centerX = width-radius;
				else if( centerX-radius < 0 )
					centerX = radius;

				if( centerY+radius > height )
					centerY = height-radius;
				else if( centerY-radius < 0 )
					centerY = radius;

				appMenu.setup( centerX, centerY, radius );
				break;
			case MotionEvent.ACTION_UP:
				appMenu.fire();
				centerX = -1;
				break;
		}
	}
}
