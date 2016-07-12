package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.util.List;

public class AppMenu extends PieMenu
{
	private Context context;

	public AppMenu( Context context )
	{
		this.context = context;
		load();
	}

	public Context getContext()
	{
		return context;
	}

	public void fire()
	{
		if( selectedIcon > -1 )
			((AppMenu.Icon)icons.get( selectedIcon )).launch( context );
	}

	private void load()
	{
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

			Icon icon = new Icon();

			icon.appName = p.applicationInfo.loadLabel( pm ).toString();
			icon.packageName = p.packageName;
			icon.icon = p.applicationInfo.loadIcon( pm );
			icon.intent = intent;

			icons.add( icon );
		}

		numberOfIcons = icons.size();
	}

	public static class Icon extends PieMenu.Icon
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
			int s = ((int)size)>>1<<1;

			if( s < 1 )
				return;

			int half = s/2;
			int left = x-half;
			int top = y-half;

			icon.setBounds( left, top, left+s, top+s );
			icon.draw( canvas );
		}
	}
}
