package com.agilie

import android.annotation.TargetApi
import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.support.v7.widget.AppCompatEditText
import android.text.*
import android.util.AttributeSet
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.agilie.gesturedetectors.RotateGestureDetector


/**
 * Allows to scale and rotate text using multitouch.
 * Text size also scaling automatically when EditText is resizing.
 */
class RotatableAutofitEditText : AppCompatEditText {

    private val textCachedSizes = SparseIntArray()
    private val sizeTester: SizeTester
    internal var scaleDetector: ScaleGestureDetector
    internal var rotateDetector: RotateGestureDetector
    internal var startX: Float = 0.toFloat()
    internal var startY: Float = 0.toFloat()
    internal var deltaX: Float = 0.toFloat()
    internal var deltaY: Float = 0.toFloat()
    internal var startWidth: Int = 0
    private var maxTextSize: Float = 0.toFloat()
    private var minTextSize: Float = 0.toFloat()
    private var maxWidthGlobal: Int = 0
    private var paintGlobal: TextPaint? = null
    private var scaleFactor = 1f
    private var moveMode = true
    private var isMoving = false
    var isEmojiMode = false
    private var isHorizontal = true
    private var isCursorVisibleGlobal = false
    private var shouldClipBounds: Boolean = false
    private var shouldRotate: Boolean = false
    private var shouldTranslate: Boolean = false
    private var shouldResize: Boolean = false
    private var onMoveListener: OnMoveListener? = null
    private var onEditTextActivateListener: OnEditTextActivateListener? = null
    private var onAdjustEmojiSizeListener: OnAdjustEmojiSizeListener? = null

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.RotatableAutofitEditText)
        if (a != null) {
            minTextSize = a.getDimension(R.styleable.RotatableAutofitEditText_minTextSize, DEFAULT_MIN_TEXT_SIZE)
            maxTextSize = a.getDimension(R.styleable.RotatableAutofitEditText_maxTextSize, textSize)
            minimumWidth = a.getDimensionPixelOffset(R.styleable.RotatableAutofitEditText_minWidth, DEFAULT_MIN_WIDTH)
            shouldClipBounds = a.getBoolean(R.styleable.RotatableAutofitEditText_clipBounds, true)
            shouldRotate = a.getBoolean(R.styleable.RotatableAutofitEditText_rotatable, true)
            shouldResize = a.getBoolean(R.styleable.RotatableAutofitEditText_resizable, true)
            shouldTranslate = a.getBoolean(R.styleable.RotatableAutofitEditText_movable, true)
            a.recycle()
        }

        sizeTester = object : SizeTester {
            internal val textRect = RectF()

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            override fun onTestSize(suggestedSize: Int, availableSpace: RectF): Int {
                paintGlobal!!.textSize = suggestedSize.toFloat()

                val text: String
                if (!TextUtils.isEmpty(hint)) {
                    text = hint.toString()
                } else {
                    text = getText().toString()
                }

                textRect.bottom = paintGlobal!!.fontSpacing
                textRect.right = paintGlobal!!.measureText(text)
                textRect.offsetTo(0f, 0f)

                if (availableSpace.contains(textRect)) {
                    return -1
                } else {
                    return 1
                }
            }
        }

        isFocusable = true
        isFocusableInTouchMode = true
        setTextIsSelectable(true)
        inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        addSelfRemovableTextWatcher()
        isDrawingCacheEnabled = true

        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        rotateDetector = RotateGestureDetector(context, RotateListener())

        val newWidth = (startWidth * scaleFactor).toInt()
        if (newWidth > minWidth && newWidth < (parent as View).width) {
            val params = layoutParams
            params.width = newWidth
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = params
        }
    }

    override fun setCursorVisible(visible: Boolean) {
        isCursorVisibleGlobal = visible
        //        super.setCursorVisible(visible);
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        setInsertionDisabled()

        scaleDetector.onTouchEvent(event)
        rotateDetector.onTouchEvent(event)

        val pointX = event.rawX.toInt()
        val pointY = event.rawY.toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (onEditTextActivateListener != null) {
                    onEditTextActivateListener!!.onEditTextActivated(this)
                }
                startX = translationX
                startY = translationY
                deltaX = pointX - translationX
                deltaY = pointY - translationY
                setTextIsSelectable(false)
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (moveMode) {
                startWidth = width
                if (onMoveListener != null) {
                    onMoveListener!!.onFinishMoving(this, event)
                }
                moveMode = false
                isMoving = false
            }

            MotionEvent.ACTION_MOVE -> {
                val translationX = pointX - deltaX
                val translationY = pointY - deltaY
                if (moveMode && shouldTranslate) {

                    if (isInBounds(translationX, translationY)) {
                        setTranslationX(translationX)
                        setTranslationY(translationY)
                    } else if (canMoveVertically(translationY)) {
                        setTranslationY(translationY)
                    } else if (canMoveHorizontally(translationX)) {
                        setTranslationX(translationX)
                    }

                    if (!isMoving) {
                        if (onMoveListener != null) {
                            onMoveListener!!.onStartMoving()
                        }
                        isMoving = true
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> scaleFactor = 1.0f

            MotionEvent.ACTION_UP -> {
                val isEditTextMoved = Math.abs(startX - translationX) > 25 || Math.abs(startY - translationY) > 25 || !moveMode
                if (isCursorVisibleGlobal) {
                    setTextIsSelectable(true)
                    isFocusable = true
                    isClickable = true
                    isFocusableInTouchMode = !isEditTextMoved
                } else {
                    setTextIsSelectable(isEditTextMoved)
                    isFocusable = !isEditTextMoved
                    isClickable = !isEditTextMoved
                    isFocusableInTouchMode = !isEditTextMoved
                }

                if (onMoveListener != null) {
                    onMoveListener!!.onFinishMoving(this, event)
                }
                isMoving = false
                moveMode = true
            }
        }

        return isEmojiMode || super.onTouchEvent(event)
    }

    override fun onTextChanged(text: CharSequence, start: Int, before: Int, after: Int) {
        super.onTextChanged(text, start, before, after)
        adjustTextSize()
    }

    override fun onSizeChanged(width: Int, height: Int, oldwidth: Int, oldheight: Int) {
        textCachedSizes.clear()
        super.onSizeChanged(width, height, oldwidth, oldheight)
        if (width != oldwidth || height != oldheight) {
            adjustTextSize()
        }
    }

    /**
     * Resizes text on layout changes
     */
    private fun adjustTextSize() {
        val startSize = minTextSize.toInt()
        val heightLimit = (measuredHeight * scaleFactor).toInt()
        -compoundPaddingBottom - compoundPaddingTop
        maxWidthGlobal = measuredWidth - compoundPaddingLeft
        -compoundPaddingRight

        if (maxWidthGlobal <= 0) {
            return
        }

        if (isEmojiMode && onAdjustEmojiSizeListener != null) {
            onAdjustEmojiSizeListener!!.onAdjustEmojiSize(text, layoutParams.width - paddingRight - paddingLeft)
        }

        super.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                efficientTextSizeSearch(startSize, maxTextSize.toInt(),
                        sizeTester, RectF(0f, 0f, maxWidthGlobal.toFloat(), heightLimit.toFloat())).toFloat())
    }

    /**
     * Gets cached text size from list of previously stored sizes
     */
    private fun efficientTextSizeSearch(start: Int, end: Int,
                                        sizeTester: SizeTester, availableSpace: RectF): Int {
        val text: String
        if (!TextUtils.isEmpty(hint)) {
            text = hint.toString()
        } else {
            text = getText().toString()
        }

        val key = text.length
        var size = textCachedSizes.get(key)
        if (size != 0) {
            return size
        }
        size = binarySearch(start, end, sizeTester, availableSpace)
        textCachedSizes.put(key, size)
        return size
    }

    /**
     * Calculates best text size for current EditText size
     */
    private fun binarySearch(start: Int, end: Int, sizeTester: SizeTester, availableSpace: RectF): Int {
        var lastBest = start
        var low = start
        var high = end - 1
        var middle: Int
        while (low <= high) {
            middle = (low + high).ushr(1)
            val midValCmp = sizeTester.onTestSize(middle, availableSpace)
            if (midValCmp < 0) {
                lastBest = low
                low = middle + 1
            } else if (midValCmp > 0) {
                high = middle - 1
                lastBest = high
            } else {
                return middle
            }
        }
        return lastBest
    }

    /**
     * This method sets TextView#Editor#mInsertionControllerEnabled field to false
     * to return false from the Editor#hasInsertionController() method to PREVENT showing
     * of the insertionController from EditText
     * The Editor#hasInsertionController() method is called in  Editor#onTouchUpEvent(MotionEvent event) method.
     */
    private fun setInsertionDisabled() {
        try {
            val editorField = TextView::class.java.getDeclaredField("mEditor")
            editorField.setAccessible(true)
            val editorObject = editorField.get(this)

            val editorClass = Class.forName("android.widget.Editor")
            val mInsertionControllerEnabledField = editorClass.getDeclaredField("mInsertionControllerEnabled")
            mInsertionControllerEnabledField.isAccessible = true
            mInsertionControllerEnabledField.set(editorObject, false)
        } catch (ignored: Exception) {
            // ignore exception here
        }

    }

    /**
     * Adds TextWatcher if EditText has hint
     */
    private fun addSelfRemovableTextWatcher() {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //empty
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                removeTextChangedListener(this)
                hint = null
            }

            override fun afterTextChanged(s: Editable) {
                //empty
            }
        })
    }

    /**
     * Check if current EditText is within parent container
     */
    private fun isInBounds(translationX: Float, translationY: Float): Boolean {
        if (parent == null || !shouldClipBounds) {
            return true
        }

        val parent = parent as View

        val degreeRemain = rotation % 360
        isHorizontal = !(degreeRemain > 45 && degreeRemain < 135
                || degreeRemain > 225 && degreeRemain < 315
                || degreeRemain > -135 && degreeRemain < -45
                || degreeRemain > -315 && degreeRemain < -225)

        if (isHorizontal) {
            return translationX + width < parent.width
                    && translationX > 0
                    && translationY + height < parent.height
                    && translationY > 0
        } else {
            val correction = (width / 2 - height / 2).toFloat()
            return translationX + correction + height.toFloat() < parent.width
                    && translationX + correction > 0
                    && translationY - correction + width < parent.height
                    && translationY - correction > 0
        }
    }

    /**
     * Check if current EditText can move up and down
     * when it reached the horizontal limits of parent view
     */
    private fun canMoveVertically(translationY: Float): Boolean {
        if (parent == null) {
            return true
        }
        val parent = parent as View

        if (isHorizontal) {
            return translationY + height < parent.height && translationY > 0
        } else {
            val correction = (width / 2 - height / 2).toFloat()
            return translationY - correction + width < parent.height && translationY - correction > 0
        }
    }

    /**
     * Check if current EditText can move left and right
     * when it reached the vertical limits of parent view
     */
    private fun canMoveHorizontally(translationX: Float): Boolean {
        if (parent == null) {
            return true
        }
        val parent = parent as View

        if (isHorizontal) {
            return translationX + width < parent.width && translationX > 0
        } else {
            val correction = (width / 2 - height / 2).toFloat()
            return translationX + correction + height.toFloat() < parent.width && translationX + correction > 0
        }
    }

    fun setOnMoveListener(listener: OnMoveListener) {
        onMoveListener = listener
    }

    fun setOnactivateListener(listener: OnEditTextActivateListener) {
        onEditTextActivateListener = listener
    }

    fun setOnAdjustEmojiSizeListener(onAdjustEmojiSizeListener: OnAdjustEmojiSizeListener) {
        this.onAdjustEmojiSizeListener = onAdjustEmojiSizeListener
    }

    /**
     * Sets the typeface in which the text should be displayed
     */
    override fun setTypeface(tf: Typeface?) {
        if (paintGlobal == null) {
            paintGlobal = TextPaint(paint)
        }
        paintGlobal!!.typeface = tf
        super.setTypeface(tf)
    }

    fun setMinTextSize(minTextSize: Float) {
        this.minTextSize = minTextSize
        adjustTextSize()
    }

    fun setMaxTextSize(maxTextSize: Float) {
        this.maxTextSize = maxTextSize
        adjustTextSize()
    }

    fun shouldClipBounds(): Boolean {
        return shouldClipBounds
    }

    /* Getters & Setters */

    fun setShouldClipBounds(shouldClipBounds: Boolean) {
        this.shouldClipBounds = shouldClipBounds
    }

    fun shouldRotate(): Boolean {
        return shouldRotate
    }

    fun shouldRotate(shouldRotate: Boolean) {
        this.shouldRotate = shouldRotate
    }

    fun shouldTranslate(): Boolean {
        return shouldTranslate
    }

    fun setShouldTranslate(shouldTranslate: Boolean) {
        this.shouldTranslate = shouldTranslate
    }

    fun shouldResize(): Boolean {
        return shouldResize
    }

    fun shouldResize(shouldResize: Boolean) {
        this.shouldResize = shouldResize
    }

    private interface SizeTester {
        /**
         * AutoResizeEditText

         * @param suggestedSize  Size of text to be tested
         * *
         * @param availableSpace available space in which text must fit
         * *
         * @return an integer < 0 if after applying `suggestedSize` to
         * * text, it takes less space than `availableSpace`, > 0
         * * otherwise
         */
        fun onTestSize(suggestedSize: Int, availableSpace: RectF): Int
    }

    /**
     * OnMoveListener
     */
    interface OnMoveListener {
        fun onStartMoving()

        fun onFinishMoving(autofitEditText: RotatableAutofitEditText, event: MotionEvent)
    }

    /**
     * OnEditTextActivateListener
     */
    interface OnEditTextActivateListener {
        fun onEditTextActivated(autofitEditText: RotatableAutofitEditText)
    }

    /**
     * OnAdjustEmojiSizeListener
     */
    interface OnAdjustEmojiSizeListener {
        fun onAdjustEmojiSize(text: Spannable, size: Int)
    }

    /**
     * Performs changes on pinch
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!shouldResize) {
                return true
            }

            scaleFactor *= detector.scaleFactor

            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f))

            if (!moveMode) {
                val x = translationX
                val y = translationY

                val newWidth = (startWidth * scaleFactor).toInt()
                if (newWidth > minimumWidth && newWidth < (parent as View).width) {
                    val params = layoutParams
                    params.width = newWidth
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    layoutParams = params
                }
                translationX = x
                translationY = y

                invalidate()
            }

            return true
        }
    }

    /**
     * Performs changes on rotation
     */
    private inner class RotateListener : RotateGestureDetector.SimpleOnRotateGestureListener() {

        override fun onRotate(detector: RotateGestureDetector): Boolean {
            if (!moveMode && shouldRotate) {
                rotation = rotation - detector.rotationDegreesDelta
            }
            return false
        }
    }

    companion object {

        private val DEFAULT_MIN_TEXT_SIZE = 12f
        private val DEFAULT_MIN_WIDTH = 800
    }
}