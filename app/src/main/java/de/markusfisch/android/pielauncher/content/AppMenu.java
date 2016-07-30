package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AppMenu extends PieMenu
{
	private final HashMap<String, App> apps = new HashMap<>();

	private Context context;

	public AppMenu( Context context )
	{
		this.context = context;
		indexAppsAsync();
	}

	public void draw( Canvas canvas )
	{
		for( int n = numberOfIcons; n-- > 0; )
			((AppIcon)icons.get( n )).draw( canvas );
	}

	public void launch()
	{
		if( selectedIcon > -1 )
			((AppIcon)icons.get( selectedIcon )).launch( context );
	}

	private void indexAppsAsync()
	{
		new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground( Void... nothing )
			{
				indexApps();
				restore();
				return null;
			}
		}.execute();
	}

	private void restore()
	{
		numberOfIcons = 0;
		icons.clear();

		// if this is the first run, just take a few apps
		for( Iterator<App> it = apps.values().iterator(); it.hasNext(); )
		{
			icons.add( new AppIcon( it.next() ) );

			// DEBUG
			if( icons.size() > 8 )
				break;
		}

		numberOfIcons = icons.size();
	}

	private void indexApps()
	{
		apps.clear();

		PackageManager pm = context.getPackageManager();

		for( PackageInfo pkg : pm.getInstalledPackages( 0 ) )
			apps.put(
				pkg.packageName,
				new App(
					pkg.packageName,
					pkg.applicationInfo.loadLabel( pm ).toString(),
					pkg.applicationInfo.loadIcon( pm ) ) );
	}

	private static class App
	{
		public final String packageName;
		public final String appName;
		public final Drawable icon;

		public App(
			String packageName,
			String appName,
			Drawable icon )
		{
			this.packageName = packageName;
			this.appName = appName;
			this.icon = icon;
		}
	}

	private static class AppIcon extends PieMenu.Icon
	{
		public final App app;

		public AppIcon( App app )
		{
			this.app = app;
		}

		public void launch( Context context )
		{
			PackageManager pm = context.getPackageManager();
			Intent intent;

			if( pm == null ||
				(intent = pm.getLaunchIntentForPackage(
					app.packageName )) == null )
				return;

			context.startActivity( intent );
		}

		public void draw( Canvas canvas )
		{
			int s = (int)size >> 1;

			if( s < 1 )
				return;

			int left = x-s;
			int top = y-s;
			s <<= 1;

			app.icon.setBounds( left, top, left+s, top+s );
			app.icon.draw( canvas );
		}
	}
}
