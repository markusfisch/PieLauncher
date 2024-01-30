package de.markusfisch.android.pielauncher.io;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import de.markusfisch.android.pielauncher.graphics.IconPack;

public class IconMappings {
	private static final String MAPPINGS_FILE = "mappings";

	@SuppressWarnings("unchecked")
	public static void restore(Context context,
			HashMap<String, IconPack.PackAndDrawable> mappings) {
		try {
			FileInputStream fis = context.openFileInput(MAPPINGS_FILE);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mappings.clear();
			mappings.putAll((HashMap<String, IconPack.PackAndDrawable>)
					ois.readObject());
			ois.close();
			fis.close();
		} catch (IOException | ClassCastException | ClassNotFoundException e) {
			// Ignore, can't do nothing about this.
		}
	}

	public static void store(Context context,
			HashMap<String, IconPack.PackAndDrawable> mappings) {
		try {
			FileOutputStream fos = context.openFileOutput(
					MAPPINGS_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mappings);
			oos.close();
			fos.close();
		} catch (IOException e) {
			// Ignore, can't do nothing about this.
		}
	}
}
