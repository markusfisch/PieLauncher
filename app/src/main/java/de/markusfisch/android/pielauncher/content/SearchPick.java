package de.markusfisch.android.pielauncher.content;

import java.util.Locale;
import java.util.Map;

final class SearchPick {
	// Picks that weren't renewed for this long have decayed to less
	// than 1/256 of their score and are dropped.
	static final long MAX_AGE_MILLIS = 8L * Frecency.HALF_LIFE_MILLIS;

	double score;
	long updatedAt;

	SearchPick(double score, long updatedAt) {
		this.score = score;
		this.updatedAt = updatedAt;
	}

	static String getKey(String query) {
		return query == null
				? ""
				: query.trim().toLowerCase(Locale.getDefault());
	}

	// Sums the scores of all picks made for a search term that starts
	// with the given key. This way, the pick for "fir" is also suggested
	// for "f" and "fi".
	static double getScore(
			Map<String, SearchPick> picks,
			String key,
			long now) {
		if (picks == null || key.isEmpty()) {
			return 0d;
		}
		double sum = 0d;
		long minUpdatedAt = now - MAX_AGE_MILLIS;
		for (Map.Entry<String, SearchPick> entry : picks.entrySet()) {
			if (entry.getKey().startsWith(key)) {
				SearchPick pick = entry.getValue();
				if (pick.updatedAt >= minUpdatedAt) {
					sum += Frecency.getScore(pick.score, pick.updatedAt, now);
				}
			}
		}
		return sum;
	}
}
