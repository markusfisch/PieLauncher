package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
	public static <T> void toast(Context context, T message) {
		String m;
		if (message instanceof Integer) {
			m = context.getString((Integer) message);
		} else if (message instanceof String) {
			m = (String) message;
		} else {
			return;
		}
		Toast.makeText(context, m, Toast.LENGTH_SHORT).show();
	}
}
