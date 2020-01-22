package de.markusfisch.android.pielauncher.adapter;

import de.markusfisch.android.pielauncher.content.AppMenu;
import de.markusfisch.android.pielauncher.R;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppsAdapter extends BaseAdapter {
	private static final boolean USE_COMPOUND =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

	private final Rect iconRect = new Rect();

	private List<AppMenu.AppIcon> apps;

	public AppsAdapter(List<AppMenu.AppIcon> apps) {
		this.apps = apps;
	}

	@Override
	public int getCount() {
		return apps.size();
	}

	@Override
	public AppMenu.AppIcon getItem(int position) {
		return apps.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = parent.getContext();
		if (convertView == null) {
			if (USE_COMPOUND) {
				float dp = context.getResources().getDisplayMetrics().density;
				int iconSize = Math.round(dp * 48f);
				iconRect.set(0, 0, iconSize, iconSize);
			}

			convertView = LayoutInflater
					.from(context)
					.inflate(R.layout.item_app, parent, false);
		}

		AppMenu.AppIcon app = apps.get(position);

		ViewHolder holder = getViewHolder(convertView);
		holder.name.setText(app.label);
		if (USE_COMPOUND) {
			app.icon.setBounds(iconRect);
			holder.name.setCompoundDrawablesRelative(
					app.icon, null, null, null);
		} else {
			holder.icon.setImageDrawable(app.icon);
		}

		return convertView;
	}

	private ViewHolder getViewHolder(View view) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			if (!USE_COMPOUND) {
				holder.icon = view.findViewById(R.id.icon);
			}
			holder.name = view.findViewById(R.id.name);
			view.setTag(holder);
		}
		return holder;
	}

	private static class ViewHolder {
		private ImageView icon;
		private TextView name;
	}
}
