package com.agilie;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.agilie.gesturedetectors.RotateGestureDetector;

import java.lang.reflect.Field;

import static android.R.attr.minWidth;


/**
 * Allows to scale and rotate text using multitouch.
 * Text size also scaling automatically when EditText is resizing.
 */
public class RotatableAutofitEditText extends EditText {

    private static float DEFAULT_MIN_TEXT_SIZE = 12f;
    private static int DEFAULT_MIN_WIDTH = 800;

    private final SparseIntArray textCachedSizes = new SparseIntArray();
    private final SizeTester sizeTester;
    private float maxTextSize;
    private float minTextSize;
    private int maxWidth;
    private TextPaint paint;

    ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;
    RotateGestureDetector rotateDetector;

    float startX;
    float startY;
    float deltaX;
    float deltaY;

    int startWidth;

    private boolean moveMode = true;
    private boolean isMoving = false;
    private boolean emojiMode = false;
    private boolean isHorizontal = true;

    private interface SizeTester {
        /**
         * AutoResizeEditText
         *
         * @param suggestedSize  Size of text to be tested
         * @param availableSpace available space in which text must fit
         * @return an integer < 0 if after applying {@code suggestedSize} to
         * text, it takes less space than {@code availableSpace}, > 0
         * otherwise
         */
        int onTestSize(int suggestedSize, RectF availableSpace);
    }

    public RotatableAutofitEditText(final Context context) {
        this(context, null, 0);
    }

    public RotatableAutofitEditText(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotatableAutofitEditText(final Context context, final AttributeSet attrs,
                                    final int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotatableAutofitEditText);
        if (a != null) {
            minTextSize = a.getDimension(R.styleable.RotatableAutofitEditText_minTextSize, DEFAULT_MIN_TEXT_SIZE);
            setMinimumWidth(a.getDimensionPixelOffset(R.styleable.RotatableAutofitEditText_minWidth, DEFAULT_MIN_WIDTH));
            a.recycle();
        }

        maxTextSize = getTextSize();

        sizeTester = new SizeTester() {
            final RectF textRect = new RectF();

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public int onTestSize(final int suggestedSize,
                                  final RectF availableSPace) {
                paint.setTextSize(suggestedSize);

                String text;
                if (!TextUtils.isEmpty(getHint())) {
                    text = getHint().toString();
                } else {
                    text = getText().toString();
                }

                textRect.bottom = paint.getFontSpacing();
                textRect.right = paint.measureText(text);
                textRect.offsetTo(0, 0);

                if (availableSPace.contains(textRect)) return -1;
                else return 1;
            }
        };

        addSelfRemovableTextWatcher();
        setDrawingCacheEnabled(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        rotateDetector = new RotateGestureDetector(context, new RotateListener());

        int newWidth = (int) (startWidth * scaleFactor);
        if (newWidth > minWidth && newWidth < ((View) getParent()).getWidth()) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = newWidth;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            setLayoutParams(params);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        setInsertionDisabled();

        scaleDetector.onTouchEvent(event);
        rotateDetector.onTouchEvent(event);

        final int pointX = (int) event.getRawX();
        final int pointY = (int) event.getRawY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (onEditTextActivateListener != null) {
                    onEditTextActivateListener.onEditTextActivated(this);
                }
                startX = getTranslationX();
                startY = getTranslationY();
                deltaX = pointX - getTranslationX();
                deltaY = pointY - getTranslationY();
                setTextIsSelectable(false);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (moveMode) {
                    startWidth = getWidth();
                    if (onMoveListener != null)
                        onMoveListener.onFinishMoving(this, event);
                    moveMode = false;
                    isMoving = false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float translationX = pointX - deltaX;
                float translationY = pointY - deltaY;
                if (moveMode) {
                    if (isInBounds(translationX, translationY)) {
                        setTranslationX(translationX);
                        setTranslationY(translationY);
                    } else if (canMoveVertically(translationY)) {
                        setTranslationY(translationY);
                    } else if (canMoveHorizontally(translationX)) {
                        setTranslationX(translationX);
                    }

                    if (!isMoving) {
                        if (onMoveListener != null) {
                            onMoveListener.onStartMoving();
                        }
                        isMoving = true;
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                scaleFactor = 1.0f;
                break;

            case MotionEvent.ACTION_UP:
                boolean isEditTextMoved = Math.abs(startX - getTranslationX()) > 25 || Math.abs(startY - getTranslationY()) > 25 || !moveMode;
                setTextIsSelectable(isEditTextMoved);
                setFocusable(!isEditTextMoved);
                setClickable(!isEditTextMoved);
                setFocusableInTouchMode(!isEditTextMoved);

                if (onMoveListener != null) {
                    onMoveListener.onFinishMoving(this, event);
                }
                isMoving = false;
                moveMode = true;
                break;
        }

        return emojiMode || super.onTouchEvent(event);
    }

    /**
     * Performs changes on pinch
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));

            if (!moveMode) {
                float x = getTranslationX();
                float y = getTranslationY();

                int newWidth = (int) (startWidth * scaleFactor);
                if (newWidth > getMinimumWidth() && newWidth < ((View) getParent()).getWidth()) {
                    ViewGroup.LayoutParams params = getLayoutParams();
                    params.width = newWidth;
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    setLayoutParams(params);
                }
                setTranslationX(x);
                setTranslationY(y);

                invalidate();
            }

            return true;
        }
    }

    /**
     * Performs changes on rotation
     */
    private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {

        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            if (!moveMode) {
                setRotation(getRotation() - detector.getRotationDegreesDelta());
            }
            return false;
        }
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        super.onTextChanged(text, start, before, after);
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(final int width, final int height, final int oldwidth, final int oldheight) {
        textCachedSizes.clear();
        super.onSizeChanged(width, height, oldwidth, oldheight);
        if (width != oldwidth || height != oldheight)
            adjustTextSize();
    }

    /**
     * Resizes text on layout changes
     */
    private void adjustTextSize() {
        final int startSize = (int) minTextSize;
        final int heightLimit = getMeasuredHeight()
                - getCompoundPaddingBottom() - getCompoundPaddingTop();
        maxWidth = getMeasuredWidth() - getCompoundPaddingLeft()
                - getCompoundPaddingRight();
        if (maxWidth <= 0)
            return;

        if (emojiMode && onAdjustEmojiSizeListener != null) {
            onAdjustEmojiSizeListener.onAdjustEmojiSize(getText(), getLayoutParams().width - getPaddingRight() - getPaddingLeft());
        }

        super.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                efficientTextSizeSearch(startSize, (int) maxTextSize,
                        sizeTester, new RectF(0, 0, maxWidth, heightLimit)));

    }

    /**
     * Gets cached text size from list of previously stored sizes
     */
    private int efficientTextSizeSearch(final int start, final int end,
                                        final SizeTester sizeTester, final RectF availableSpace) {
        String text;
        if (!TextUtils.isEmpty(getHint())) {
            text = getHint().toString();
        } else {
            text = getText().toString();
        }

        final int key = text.length();
        int size = textCachedSizes.get(key);
        if (size != 0)
            return size;
        size = binarySearch(start, end, sizeTester, availableSpace);
        textCachedSizes.put(key, size);
        return size;
    }

    /**
     *  Calculates best text size for current EditText size
     */
    private int binarySearch(final int start, final int end, final SizeTester sizeTester, final RectF availableSpace) {
        int lastBest = start;
        int low = start;
        int high = end - 1;
        int middle;
        while (low <= high) {
            middle = low + high >>> 1;
            final int midValCmp = sizeTester.onTestSize(middle, availableSpace);
            if (midValCmp < 0) {
                lastBest = low;
                low = middle + 1;
            } else if (midValCmp > 0) {
                high = middle - 1;
                lastBest = high;
            } else
                return middle;
        }
        return lastBest;
    }

    /**
     * This method sets TextView#Editor#mInsertionControllerEnabled field to false
     * to return false from the Editor#hasInsertionController() method to PREVENT showing
     * of the insertionController from EditText
     * The Editor#hasInsertionController() method is called in  Editor#onTouchUpEvent(MotionEvent event) method.
     */
    private void setInsertionDisabled() {
        try {
            Field editorField = TextView.class.getDeclaredField("mEditor");
            editorField.setAccessible(true);
            Object editorObject = editorField.get(this);

            Class editorClass = Class.forName("android.widget.Editor");
            Field mInsertionControllerEnabledField = editorClass.getDeclaredField("mInsertionControllerEnabled");
            mInsertionControllerEnabledField.setAccessible(true);
            mInsertionControllerEnabledField.set(editorObject, false);
        } catch (Exception ignored) {
            // ignore exception here
        }
    }

    /**
     * Adds TextWatcher if EditText has hint
     */
    private void addSelfRemovableTextWatcher() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                removeTextChangedListener(this);
                setHint(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                //empty
            }
        });
    }

    /**
     * Check if current EditText is within parent container
     */
    private boolean isInBounds(float translationX, float translationY) {
        if (getParent() == null) return true;
        View parent = (View) getParent();

        float degreeRemain = getRotation() % 360;
        isHorizontal = !((degreeRemain > 45 && degreeRemain < 135)
                || (degreeRemain > 225 && degreeRemain < 315)
                || (degreeRemain > -135 && degreeRemain < -45)
                || (degreeRemain > -315 && degreeRemain < -225));

        if (isHorizontal) {
            return translationX + getWidth() < parent.getWidth()
                    && translationX > 0
                    && translationY + getHeight() < parent.getHeight()
                    && translationY > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationX + correction + getHeight() < parent.getWidth()
                    && translationX + correction > 0
                    && translationY - correction + getWidth() < parent.getHeight()
                    && translationY - correction > 0;
        }
    }

    /**
     * Check if current EditText can move up and down
     * when it reached the horizontal limits of parent view
     */
    private boolean canMoveVertically(float translationY) {
        if (getParent() == null) return true;
        View parent = (View) getParent();

        if (isHorizontal) {
            return translationY + getHeight() < parent.getHeight()
                    && translationY > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationY - correction + getWidth() < parent.getHeight()
                    && translationY - correction > 0;
        }
    }

    /**
     * Check if current EditText can move left and right
     * when it reached the vertical limits of parent view
     */
    private boolean canMoveHorizontally(float translationX) {
        if (getParent() == null) return true;
        View parent = (View) getParent();

        if (isHorizontal) {
            return translationX + getWidth() < parent.getWidth()
                    && translationX > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationX + correction + getHeight() < parent.getWidth()
                    && translationX + correction > 0;
        }
    }

    /**
     * Sets the typeface in which the text should be displayed
     */
    @Override
    public void setTypeface(final Typeface tf) {
        if (paint == null)
            paint = new TextPaint(getPaint());
        paint.setTypeface(tf);
        super.setTypeface(tf);
    }

    /**
     * Sets minimal text size
     */
    public void setMinTextSize(float minTextSize) {
        this.minTextSize = minTextSize;
        adjustTextSize();
    }

    /**
     * Sets maximal text size
     */
    public void setMaxTextSize(float maxTextSize) {
        this.maxTextSize = maxTextSize;
        adjustTextSize();
    }

    /**
     * OnMoveListener
     */
    public interface OnMoveListener {
        void onStartMoving();

        void onFinishMoving(RotatableAutofitEditText autofitEditText, MotionEvent event);
    }

    private OnMoveListener onMoveListener;

    public void setOnMoveListener(OnMoveListener listener) {
        onMoveListener = listener;
    }

    /**
     * OnEditTextActivateListener
     */
    public interface OnEditTextActivateListener {
        void onEditTextActivated(RotatableAutofitEditText autofitEditText);
    }

    private OnEditTextActivateListener onEditTextActivateListener;

    public void setOnactivateListener(OnEditTextActivateListener listener) {
        onEditTextActivateListener = listener;
    }

    /**
     * OnAdjustEmojiSizeListener
     */
    public interface OnAdjustEmojiSizeListener {
        void onAdjustEmojiSize(Spannable text, int size);
    }

    private OnAdjustEmojiSizeListener onAdjustEmojiSizeListener;

    public void setOnAdjustEmojiSizeListener(OnAdjustEmojiSizeListener onAdjustEmojiSizeListener) {
        this.onAdjustEmojiSizeListener = onAdjustEmojiSizeListener;
    }

    public void setEmojiMode(boolean emojiMode) {
        this.emojiMode = emojiMode;
    }

    public boolean isEmojiMode() {
        return emojiMode;
    }

}