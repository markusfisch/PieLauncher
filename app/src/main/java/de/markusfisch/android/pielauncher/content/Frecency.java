package de.markusfisch.android.pielauncher.content;

final class Frecency {
	static final long HALF_LIFE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

	private static final double LN_2 = Math.log(2d);

	private Frecency() {
	}

	static double getScore(double score, long updatedAt, long now) {
		if (score <= 0d || updatedAt <= 0L) {
			return 0d;
		}
		long elapsed = Math.max(0L, now - updatedAt);
		return score * Math.exp(-LN_2 * elapsed / HALF_LIFE_MILLIS);
	}

	static double addLaunch(double score, long updatedAt, long now) {
		return getScore(score, updatedAt, now) + 1d;
	}

	static int compare(double leftScore, long leftUpdatedAt,
			double rightScore, long rightUpdatedAt, long now) {
		return Double.compare(
				getScore(rightScore, rightUpdatedAt, now),
				getScore(leftScore, leftUpdatedAt, now));
	}
}
