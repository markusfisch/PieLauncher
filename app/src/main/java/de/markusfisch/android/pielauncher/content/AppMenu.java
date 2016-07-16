package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.List;

public class AppMenu extends PieMenu
{
	private Context context;

	public AppMenu( Context context )
	{
		this.context = context;
		restoreAsync();
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

	private void restoreAsync()
	{
		new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground( Void... nothing )
			{
				restore();
				return null;
			}
		}.execute();
	}

	private void restore()
	{
		numberOfIcons = 0;
		icons.clear();

		PackageManager pm = context.getPackageManager();
		List<PackageInfo> packs = pm.getInstalledPackages( 0 );

		for( int n = 0, len = packs.size();
			n < len;
			++n )
		{
			PackageInfo p = packs.get( n );
			Intent intent;

			if( p == null ||
				(intent = pm.getLaunchIntentForPackage(
					p.packageName )) == null )
				continue;

			AppIcon icon = new AppIcon();
			icon.appName = p.applicationInfo.loadLabel( pm ).toString();
			icon.packageName = p.packageName;
			icon.icon = p.applicationInfo.loadIcon( pm );
			icon.intent = intent;

			icons.add( icon );

			// DEBUG ONLY
			if( icons.size() > 8 )
				break;
		}

		numberOfIcons = icons.size();
	}

	private static class AppIcon extends PieMenu.Icon
	{
		public String appName;
		public String packageName;
		public Drawable icon;
		public Intent intent;

		public void launch( Context context )
		{
			if( intent == null )
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

			icon.setBounds( left, top, left+s, top+s );
			icon.draw( canvas );
		}
	}
}
