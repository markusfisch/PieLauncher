package de.markusfisch.android.pielauncher.activity;

import de.markusfisch.android.pielauncher.widget.PieView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.Window;

public class HomeActivity extends Activity
{
	@Override
	public void onBackPressed()
	{
		// ignore back on home screen
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );

		setContentView( new PieView( this ) );
		setTransparentSystemBars( getWindow() );
	}

	@TargetApi( Build.VERSION_CODES.LOLLIPOP )
	private static boolean setTransparentSystemBars( Window window )
	{
		if( window == null ||
			Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP )
			return false;

		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
		window.setStatusBarColor( 0 );
		window.setNavigationBarColor( 0 );

		return true;
	}
}
