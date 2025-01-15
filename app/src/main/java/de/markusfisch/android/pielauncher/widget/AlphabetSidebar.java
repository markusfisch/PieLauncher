package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.markusfisch.android.pielauncher.R;

public class AlphabetSidebar extends View {
    private String[] alphabet;
    private Map<String, Set<String>> letterAliases = new HashMap<>();

    public interface OnLetterSelectedListener {
        void onLetterSelected(String letter, Set<String> aliases);
        void onLetterDeselected();
    }

    public interface OnInteractionListener {
        void onInteractionStateChanged(boolean isInteracting);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final float[] letterHeights;
    private String selectedLetter = null;
    private OnLetterSelectedListener listener;
    private OnInteractionListener interactionListener;
    private boolean isScrolling = false;
    private float letterHeight;
    private float textSize;
    private int textColor;
    private float dp;
    private static final float BUBBLE_RADIUS = 24f;
    private static final float MAX_BUBBLE_DISTANCE = 3f;
    private static final float ANIMATION_SPEED = 0.2f;
    private static final double PI = Math.PI;
    private float touchY = -1;
    private float[] currentX;
    private float[] currentY;
    private float[] targetX;
    private float[] targetY;
    private long lastFrameTime;

    public AlphabetSidebar(Context context) {
        this(context, null);
    }

    public AlphabetSidebar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphabetSidebar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Load the alphabet from resources
        alphabet = context.getResources().getStringArray(R.array.alphabet_chars);
        letterHeights = new float[alphabet.length];
        currentX = new float[alphabet.length];
        currentY = new float[alphabet.length];
        targetX = new float[alphabet.length];
        targetY = new float[alphabet.length];
        dp = context.getResources().getDisplayMetrics().density;

        // Load aliases for all letters
        Resources res = context.getResources();
        for (String letter : alphabet) {
            if (letter.length() == 1) {  // Skip multi-char entries like "LL" or "#"
                String aliasResourceName = letter.toLowerCase() + "_aliases";
                int aliasResId = res.getIdentifier(aliasResourceName, "array", context.getPackageName());
                if (aliasResId != 0) {
                    try {
                        String[] aliases = res.getStringArray(aliasResId);
                        if (aliases.length > 0) {
                            Set<String> aliasSet = new HashSet<>();
                            for (String alias : aliases) {
                                aliasSet.add(alias);
                            }
                            letterAliases.put(letter, aliasSet);
                        }
                    } catch (Resources.NotFoundException ignored) {
                        // Resource not found for this letter, skip it
                    }
                }
            }
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AlphabetSidebar, defStyleAttr, 0);

        textSize = a.getDimensionPixelSize(R.styleable.AlphabetSidebar_android_textSize,
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
            res.getDisplayMetrics()));
        textColor = a.getColor(R.styleable.AlphabetSidebar_android_textColor,
            res.getColor(R.color.text_color));

        a.recycle();

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
    }

    public void setOnLetterSelectedListener(OnLetterSelectedListener listener) {
        this.listener = listener;
    }

    public void setOnInteractionListener(OnInteractionListener listener) {
        this.interactionListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float bottomPadding = 24f * dp;
        float availableHeight = h - bottomPadding;
        letterHeight = availableHeight / (float) alphabet.length;
        float centerX = w - textSize/2;

        // Initialize positions
        for (int i = 0; i < alphabet.length; i++) {
            letterHeights[i] = letterHeight * (i + 0.5f);
            currentX[i] = centerX;
            currentY[i] = letterHeights[i];
            targetX[i] = centerX;
            targetY[i] = letterHeights[i];
        }
        lastFrameTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() - textSize/2;
        float bubbleRadius = BUBBLE_RADIUS * dp;
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        lastFrameTime = currentTime;
        boolean needsAnotherFrame = false;

        // Calculate target positions
        for (int i = 0; i < alphabet.length; i++) {
            String letter = alphabet[i];
            targetX[i] = centerX;
            targetY[i] = letterHeights[i];

            if (touchY > 0) {
                float distanceFromTouch = Math.abs(letterHeights[i] - touchY);
                float letterHeight = getHeight() / (float) alphabet.length;
                float maxDistance = letterHeight * MAX_BUBBLE_DISTANCE;

                if (distanceFromTouch < maxDistance) {
                    float normalizedDistance = distanceFromTouch / maxDistance;
                    float fanOutDistance = bubbleRadius * (1 - normalizedDistance * normalizedDistance);
                    targetX[i] = centerX - fanOutDistance;

                    float verticalOffset = fanOutDistance * 0.2f;
                    targetY[i] = letterHeights[i] - verticalOffset * (1 - normalizedDistance);
                }
            }

            // Animate towards target
            float dx = targetX[i] - currentX[i];
            float dy = targetY[i] - currentY[i];
            if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f) {
                currentX[i] += dx * ANIMATION_SPEED * deltaTime * 60f;
                currentX[i] = Math.max(currentX[i], centerX - bubbleRadius);
                currentY[i] += dy * ANIMATION_SPEED * deltaTime * 60f;
                needsAnotherFrame = true;
            } else {
                currentX[i] = targetX[i];
                currentY[i] = targetY[i];
            }

            // Draw letter
            paint.getTextBounds(letter, 0, 1, textBounds);
            float textY = currentY[i] + (textBounds.height() / 2f);

            // Calculate alpha based on current position
            float currentDistance = Math.abs(letterHeights[i] - touchY);
            float maxDistance = letterHeight * MAX_BUBBLE_DISTANCE;
            if (touchY > 0 && currentDistance < maxDistance) {
                float normalizedDistance = currentDistance / maxDistance;
                paint.setAlpha((int)(255 * (1 - normalizedDistance * 0.5f)));
            } else {
                paint.setAlpha(letter.equals(selectedLetter) ? 255 : (touchY > 0 ? 128 : 180));
            }

            canvas.drawText(letter, currentX[i], textY, paint);
        }

        if (needsAnotherFrame) {
            postInvalidateOnAnimation();
        }
    }

    public boolean isTouchInside(float x, float y) {
        return x >= getLeft() && x <= getRight() && y >= getTop() && y <= getBottom();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (interactionListener != null) {
                    interactionListener.onInteractionStateChanged(true);
                }
            case MotionEvent.ACTION_MOVE:
                isScrolling = true;
                touchY = event.getY();
                float sectionHeight = getHeight() / (float) alphabet.length;
                int letterIndex = (int) (touchY / sectionHeight);
                letterIndex = Math.max(0, Math.min(alphabet.length - 1, letterIndex));

                String letter = alphabet[letterIndex];
                if (!letter.equals(selectedLetter)) {
                    selectedLetter = letter;
                    if (listener != null) {
                        Set<String> aliases = letterAliases.getOrDefault(letter, new HashSet<>());
                        listener.onLetterSelected(letter, aliases);
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (interactionListener != null) {
                    interactionListener.onInteractionStateChanged(false);
                }
                touchY = -1;
                invalidate();
                isScrolling = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void clearSelection() {
        if (selectedLetter != null) {
            selectedLetter = null;
            touchY = -1;
            if (listener != null) {
                listener.onLetterDeselected();
            }
            invalidate();
        }
    }
}
