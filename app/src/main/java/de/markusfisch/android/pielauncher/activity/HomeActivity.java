package de.markusfisch.android.pielauncher.activity;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.widget.PieLauncherSurfaceView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.Window;

public class HomeActivity extends Activity
{
	private PieLauncherSurfaceView surfaceView;

	@Override
	public void onBackPressed()
	{
		// ignore back on home screen
	}

	@Override
	protected void onCreate( Bundle state )
	{
		super.onCreate( state );

		surfaceView = new PieLauncherSurfaceView(
			new AppMenu( this ) );

		setContentView( surfaceView );
		setTransparentSystemBars( getWindow() );
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		surfaceView.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		surfaceView.onPause();
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
