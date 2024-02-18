package de.markusfisch.android.pielauncher.preference;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Build;
import android.provider.Settings;

class SystemSettings {
	private static final boolean HAS_GLOBAL =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

	private final ContentResolver contentResolver;

	private float animatorDurationScale = 1f;

	public SystemSettings(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
		contentResolver.registerContentObserver(
				HAS_GLOBAL
						? Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
						: Settings.System.getUriFor(Settings.System.TRANSITION_ANIMATION_SCALE),
				false,
				new ContentObserver(null) {
					@Override
					public void onChange(boolean selfChange) {
						onAnimatorScaleChanged();
					}
				});
		onAnimatorScaleChanged();
	}

	public float getAnimatorDurationScale() {
		return animatorDurationScale;
	}

	private void onAnimatorScaleChanged() {
		if (HAS_GLOBAL) {
			animatorDurationScale = Settings.Global.getFloat(contentResolver,
					Settings.Global.ANIMATOR_DURATION_SCALE,
					animatorDurationScale);
		} else {
			animatorDurationScale = Settings.System.getFloat(contentResolver,
					Settings.System.TRANSITION_ANIMATION_SCALE,
					animatorDurationScale);
		}
	}
}
