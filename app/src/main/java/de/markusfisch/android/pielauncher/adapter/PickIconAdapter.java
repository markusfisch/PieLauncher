package de.markusfisch.android.pielauncher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.IconPack;

public class PickIconAdapter extends ArrayAdapter<String> {
	private final IconPack.Pack pack;

	public PickIconAdapter(Context context,
			String iconPackageName, ArrayList<String> icons) {
		super(context, 0, icons);
		pack = PieLauncherApp.iconPack.packs.get(iconPackageName);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater
					.from(parent.getContext())
					.inflate(R.layout.item_icon, parent, false);
		}
		ViewHolder holder = getViewHolder(convertView);
		String name = getItem(position);
		if (pack != null) {
			holder.iconView.setImageDrawable(pack.getDrawable(name));
		}
		holder.iconView.setContentDescription(name);
		return convertView;
	}

	ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.iconView = view.findViewById(R.id.icon);
			view.setTag(holder);
		}
		return holder;
	}

	private static final class ViewHolder {
		private ImageView iconView;
	}
}
