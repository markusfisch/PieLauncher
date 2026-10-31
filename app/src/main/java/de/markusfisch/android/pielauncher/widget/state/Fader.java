package de.markusfisch.android.pielauncher.widget.state;

import android.os.SystemClock;

public class Fader {
    private long fadeInFrom;
    private long fadeInTo;
    private long fadeOutFrom;
    private float maxIn = 1f;

    public void setMaxIn(float maxIn) {
        this.maxIn = maxIn;
    }

    private float minOut = 0f;

    public void fadeIn() {
        fadeIn(SystemClock.uptimeMillis());
    }

    public void fadeIn(long now) {
        fadeInFrom = now;
        fadeInTo = 0;
        fadeOutFrom = 0;
    }

    public void fadeOut() {
        fadeOut(SystemClock.uptimeMillis());
    }

    public void fadeOut(long now) {
        if (!isVisible()) {
            return;
        }
        fadeInFrom = 0;
        fadeInTo = 0;
        fadeOutFrom = now;
    }

    public boolean isVisible() {
        return fadeInFrom > 0;
    }

    public boolean isFadingIn(long now) {
        return now < fadeInTo;
    }

    public boolean isFadingOut(long now) {
        return fadeOutFrom > 0 && fadeOutFrom <= now;
    }

    public float get(long now, float duration) {
        if (fadeInTo == 0) {
            fadeInTo = fadeInFrom + Math.round(duration);
        }
        if (duration == 0) {
            return fadeInFrom > 0 ? 1f : 0f;
        }
        if (fadeInFrom > 0) {
            float x = (now - fadeInFrom) / duration;
            maxIn = Math.min(1f, minOut + easeSlowerIn(x));
            return maxIn;
        }
        long delta = now - fadeOutFrom;
        if (delta < duration) {
            minOut = maxIn - easeSlowerOut(delta / duration);
            // Ensure < 1f so invalidate() is invoked one last time.
            minOut = Math.min(.999f, Math.max(0f, minOut));
            return minOut;
        }
        return 0;
    }

    public float get() {
        // Fallback for tests or direct calls where duration isn't specified
        return maxIn;
    }

    private static float easeSlowerIn(float x) {
        return x * x;
    }

    private static float easeSlowerOut(float x) {
        return 1f - (1f - x) * (1f - x);
    }
}
