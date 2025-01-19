package de.markusfisch.android.pielauncher.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import de.markusfisch.android.pielauncher.R;

public class HiddenAppsAdapter extends ArrayAdapter<HiddenAppsAdapter.HiddenApp> {
	public static class HiddenApp {
		public final ComponentName componentName;
		public final String name;
		public final Drawable icon;

		public HiddenApp(ComponentName componentName, String name, Drawable icon) {
			this.componentName = componentName;
			this.name = name;
			this.icon = icon;
		}
	}

	public HiddenAppsAdapter(Context context, ArrayList<HiddenApp> apps) {
		super(context, 0, apps);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater
					.from(parent.getContext())
					.inflate(R.layout.item_hidden_app, parent, false);
		}
		ViewHolder holder = getViewHolder(convertView);
		HiddenApp hiddenApp = getItem(position);
		if (hiddenApp != null) {
			holder.nameView.setText(hiddenApp.name);
			holder.iconView.setImageDrawable(hiddenApp.icon);
		}
		return convertView;
	}

	ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.iconView = view.findViewById(R.id.icon);
			holder.nameView = view.findViewById(R.id.name);
			view.setTag(holder);
		}
		return holder;
	}

	private static final class ViewHolder {
		private ImageView iconView;
		private TextView nameView;
	}
}
