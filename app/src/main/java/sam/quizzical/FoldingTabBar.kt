package sam.quizzical

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.support.annotation.MenuRes
import android.support.v7.view.SupportMenuInflater
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuItemImpl
import android.util.AttributeSet
import android.view.*
import android.view.animation.BounceInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.LinearLayout

class FoldingTabBar : LinearLayout {

    interface OnFoldingItemSelectedListener { fun onFoldingItemSelected(item: MenuItem): Boolean }
    var onFoldingItemClickListener: OnFoldingItemSelectedListener? = null

    private val CLOSE_ON_SELECT = true
    private val ALLOW_USER_EXPAND = false
    private val ANIMATION_DURATION = 500L
    private val START_DELAY = 150L
    private val MAIN_ROTATION_START = 0f
    private val MAIN_ROTATION_END = 405f
    private val ITEM_ROTATION_START = 180f
    private val ITEM_ROTATION_END = 360f
    private val ROLL_UP_ROTATION_START = -45f
    private val ROLL_UP_ROTATION_END = 360f
    private lateinit var mData: List<ImageView>
    private var mExpandingSet: AnimatorSet = AnimatorSet()
    private var mRollupSet: AnimatorSet = AnimatorSet()
    private var isAnimating: Boolean = false
    private var mMenu: MenuBuilder
    private var mSize: Int = 0
    private var indexCounter = 0
    private var mainImageView: ImageView = ImageView(context)
    private var itemsPadding: Int = 0
    private var drawableResource: Int = 0
    private val scaleAnimator = ValueAnimator.AnimatorUpdateListener {
        valueAnimator -> layoutParams = layoutParams.apply { width = valueAnimator.animatedValue as Int }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleRes: Int) : super(context, attrs, defStyleRes) {
        mMenu = MenuBuilder(context)
        gravity = Gravity.CENTER
        if (background == null) { setBackgroundResource(R.drawable.background_tabbar) }
        val a: TypedArray =context.obtainStyledAttributes(attrs, R.styleable.FoldingTabBar, 0, defStyleRes)
        initViewTreeObserver(a)
        mSize = getSizeDimension()
    }

    private fun getSizeDimension(): Int = resources.getDimensionPixelSize(R.dimen.ftb_size_mini)

    private fun getItemsPadding(): Int = resources.getDimensionPixelSize(R.dimen.ftb_item_padding)

    private fun initViewTreeObserver(a: TypedArray) {
        viewTreeObserver.addOnPreDrawListener(object: ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                isAnimating = true
                initAttributesValues(a)
                initExpandAnimators()
                initRollUpAnimators()
                return true
            }
        })
    }

    private fun initAttributesValues(a: TypedArray) {
        drawableResource = R.drawable.ic_action_plus
        itemsPadding = getItemsPadding()
        if (a.hasValue(R.styleable.FoldingTabBar_mainImage)) { drawableResource = a.getResourceId(R.styleable.FoldingTabBar_mainImage, 0) }
        if (a.hasValue(R.styleable.FoldingTabBar_itemPadding)) { itemsPadding = a.getDimensionPixelSize(R.styleable.FoldingTabBar_itemPadding, 0) }
        if (a.hasValue(R.styleable.FoldingTabBar_menu)) { inflateMenu(a.getResourceId(R.styleable.FoldingTabBar_menu, 0)) }
    }

    private fun initExpandAnimators() {
        mExpandingSet.duration = ANIMATION_DURATION
        val destWidth = childCount.times(mSize)
        val rotationSet = AnimatorSet()
        val scalingSet = AnimatorSet()
        val scalingAnimator = ValueAnimator.ofInt(mSize, destWidth).apply {
            addUpdateListener(scaleAnimator)
            addListener(rollUpListener)
        }
        val rotationAnimator = ValueAnimator.ofFloat(MAIN_ROTATION_START, MAIN_ROTATION_END).apply {
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Float
                mainImageView.rotation = value
            }
        }
        mData.forEach { item ->
            ValueAnimator.ofFloat(ITEM_ROTATION_START, ITEM_ROTATION_END).apply {
                addUpdateListener {
                    val fraction = it.animatedFraction
                    item.scaleX = fraction
                    item.scaleY = fraction
                    item.rotation = it.animatedValue as Float
                }
                addListener(expandingListener)
                rotationSet.playTogether(this)
            }
        }
        scalingSet.playTogether(scalingAnimator, rotationAnimator)
        scalingSet.interpolator = CustomBounceInterpolator()
        rotationSet.interpolator = BounceInterpolator()
        rotationSet.startDelay = START_DELAY
        mExpandingSet.playTogether(scalingSet, rotationSet)
    }

    private fun initRollUpAnimators() {
        mRollupSet.duration = ANIMATION_DURATION
        val destWidth = mMenu.size().times(mSize)
        val rotationSet = AnimatorSet()
        val scalingAnimator = ValueAnimator.ofInt(destWidth, mSize)
        val rotationAnimator = ValueAnimator.ofFloat(ROLL_UP_ROTATION_START, ROLL_UP_ROTATION_END)
        scalingAnimator.addUpdateListener(scaleAnimator)
        mRollupSet.addListener(rollUpListener)
        rotationAnimator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            mainImageView.rotation = value
        }
        val scalingSet = AnimatorSet().apply {
            playTogether(scalingAnimator, rotationAnimator)
            interpolator = CustomBounceInterpolator()
        }
        rotationSet.interpolator = BounceInterpolator()
        mRollupSet.playTogether(scalingSet, rotationSet)
    }

    private fun inflateMenu(@MenuRes resId: Int) {
        getMenuInflater().inflate(resId, mMenu)
        if (mMenu.visibleItems.size % 2 != 0) { throw Exception("menu items must be odd") }
        mData = mMenu.visibleItems.map { initAndAddMenuItem(it) }
        initMainButton(mMenu.visibleItems.size / 2)
    }

    private fun resolveAdjustedSize(desiredSize: Int, measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            View.MeasureSpec.UNSPECIFIED -> desiredSize
            View.MeasureSpec.AT_MOST -> Math.min(desiredSize, specSize)
            View.MeasureSpec.EXACTLY -> specSize
            else -> desiredSize
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!isAnimating) {
            val preferredSize = getSizeDimension()
            mSize = resolveAdjustedSize(preferredSize, widthMeasureSpec)
            setMeasuredDimension(mSize, mSize)
        }
    }


    private fun initMainButton(mainButtonIndex: Int) {
        mainImageView.setImageResource(drawableResource)
        mainImageView.layoutParams = ViewGroup.LayoutParams(mSize, mSize)
        mainImageView.setOnClickListener {
            if(ALLOW_USER_EXPAND){
                animateMenu()
            }
        }
        addView(mainImageView, mainButtonIndex)
        mainImageView.setPadding(itemsPadding, itemsPadding, itemsPadding, itemsPadding)
    }

    private fun initAndAddMenuItem(menuItem: MenuItemImpl): ImageView {
        return ImageView(context).apply {
            setImageDrawable(menuItem.icon)
            layoutParams = ViewGroup.LayoutParams(mSize, mSize)
            setPadding(itemsPadding, itemsPadding, itemsPadding, itemsPadding)
            visibility = View.GONE
            addView(this, indexCounter)
            setOnClickListener {
                onFoldingItemClickListener?.onFoldingItemSelected(menuItem)
                if(CLOSE_ON_SELECT) animateMenu()
            }
            indexCounter++
        }
    }

    fun animateMenu() { if ((measuredWidth - mSize) in -2..2) expand() else  rollUp() }

    fun expand() { mExpandingSet.start() }

    fun rollUp() { mRollupSet.start() }

    private fun getMenuInflater(): MenuInflater = SupportMenuInflater(context)

    private val rollUpListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) { mData.forEach { it.visibility = View.GONE } }
        override fun onAnimationEnd(animator: Animator) {}
        override fun onAnimationCancel(animator: Animator) {}
        override fun onAnimationRepeat(animator: Animator) {}
    }

    private val expandingListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) { mData.forEach { it.visibility = View.VISIBLE } }
        override fun onAnimationEnd(animator: Animator) {}
        override fun onAnimationCancel(animator: Animator) {}
        override fun onAnimationRepeat(animator: Animator) {}
    }

    class CustomBounceInterpolator(val amplitude: Double = 0.1, val frequency: Double = 0.8) : Interpolator {
        override fun getInterpolation(time: Float) = (-1.0 * Math.exp(-time / amplitude) * Math.cos(frequency * time) + 1).toFloat()
    }
}


