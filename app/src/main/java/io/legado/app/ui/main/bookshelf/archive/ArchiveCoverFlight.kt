package io.legado.app.ui.main.bookshelf.archive

import android.app.Activity
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import java.util.UUID

/** 书架 → 详情页封面飞行的 Intent 参数：交接 token，位图走进程内单槽。 */
internal const val EXTRA_COVER_FLIGHT = "archive:coverFlight"

/** 快照的缩放比（0.5 表示半分辨率，够 600ms 的过渡用，省一半内存） */
internal const val ARCHIVE_BACKDROP_SCALE = 0.5f

/** 系统关掉动画（开发者选项 / 无障碍）时不播飞行 */
internal fun archiveFlightAnimationsEnabled(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

/** 关闭转场置空（回程动画结束、finish 时用）。 */
@Suppress("DEPRECATION")
internal fun Activity.overrideArchiveCloseTransitionNone() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
    } else {
        overridePendingTransition(0, 0)
    }
}

/**
 * 长按档案式书架 → 书籍详情之间的封面飞行。
 *
 * AzukiReader 那边是 Compose 的 `SharedTransitionLayout` + nav3 同窗目的地，
 * 直接把封面做成共享元素（`Wenku8Paper.wenku8CoverBounds` 的 0/120/350/580ms 关键帧）。
 * 阅读S 是纯 View 且详情页在另一个 Activity，于是这里用同一套几何与时间轴手写：
 *
 * - 去程：书架用平行投影画出来的**斜四边形**，用 `Matrix.setPolyToPoly` 逐帧拉直成详情页封面矩形；
 * - 底图：MainActivity 的静态快照（截图时先把这本封面隐掉，避免出现两个封面），
 *   去程淡出、回程淡入，完成书架与详情页之间的交叉过渡；
 * - 回程：反着飞回原位，落点与真实书架的书封重合后再 `finish()`。
 *
 * 载荷走进程内单槽 + token 交接，不把 Bitmap 塞进 Intent（Binder 有 1MB 上限）。
 */
internal class ArchiveCoverFlight(
    /** 来源书籍，回程前用它确认详情页没有换书 */
    val bookUrl: String,
    /** 按书架 center-crop 口径裁好的封面，飞行时直接映射到四边形 */
    val cover: Bitmap,
    /** 书架里的原始封面，落位后给详情页封面兜底 */
    val sourceCover: Bitmap,
    /** 书架封面图区四角，屏幕坐标，顺序 TL / TR / BR / BL */
    val shelfQuad: FloatArray,
    /** 书架页面快照（已把那本封面挖空） */
    val backdrop: Bitmap,
    /** 快照的缩放比（0.5 表示半分辨率） */
    val backdropScale: Float,
    /** 快照左上角在书架 Activity 里的屏幕坐标，用来和详情页窗口对齐 */
    val backdropOriginX: Int,
    val backdropOriginY: Int,
    val screenWidth: Int,
    val screenHeight: Int,
) {
    fun matchesScreen(width: Int, height: Int): Boolean =
        width == screenWidth && height == screenHeight
}

/** 单槽交接：`offer` 返回 token，详情页用 token 取走唯一的载荷。 */
internal object ArchiveCoverFlightStore {

    private var token: String? = null
    private var pending: ArchiveCoverFlight? = null

    @Synchronized
    fun offer(flight: ArchiveCoverFlight): String {
        val id = UUID.randomUUID().toString()
        token = id
        pending = flight
        return id
    }

    @Synchronized
    fun take(id: String?): ArchiveCoverFlight? {
        if (id == null || id != token) return null
        val flight = pending
        token = null
        pending = null
        return flight
    }

    @Synchronized
    fun drop(id: String?) {
        if (id == null || id == token) {
            token = null
            pending = null
        }
    }
}

/**
 * 时间轴照抄 AzukiReader / Wenku8：
 * `wenku8CoverBounds` 去程 0/120/350/580ms、回程 180/460/740/840ms，
 * 中段整体上抬 24；Compose keyframes 段间默认线性，所以这里也按线性插值，
 * 观感来自「起步停顿 + 两段等时的四角位移 + 落位停顿」，不是单条缓动曲线。
 */
internal object ArchiveFlightMotion {

    const val FORWARD_HOLD = 120L
    const val FORWARD_MID = 350L
    const val FORWARD_LAND = 580L
    const val FORWARD_END = 620L

    const val RETURN_HOLD = 180L
    const val RETURN_MID = 460L
    const val RETURN_LAND = 740L
    const val RETURN_END = 840L

    /** 中段上抬（dp） */
    const val LIFT_DP = 24f

    /** 快照淡入淡出用的缓动（Wenku8Motion.Ease） */
    val ease = PathInterpolator(.215f, .61f, .355f, 1f)

    /** 快照不透明度：去程 1→0，回程 0→1 */
    fun backdropAlpha(time: Long, returning: Boolean): Float {
        if (returning) {
            val t = ((time - 120f) / 300f).coerceIn(0f, 1f)
            return ease.getInterpolation(t)
        }
        val t = ((time - 300f) / 260f).coerceIn(0f, 1f)
        return 1f - ease.getInterpolation(t)
    }

    /**
     * 某一时刻封面四角（TL / TR / BR / BL）。
     *
     * @param from 起点四角；@param to 终点四角；[liftPx] 中段上抬的像素数。
     */
    fun quadAt(
        time: Long,
        from: FloatArray,
        to: FloatArray,
        liftPx: Float,
        returning: Boolean,
    ): FloatArray {
        val hold = if (returning) RETURN_HOLD else FORWARD_HOLD
        val mid = if (returning) RETURN_MID else FORWARD_MID
        val land = if (returning) RETURN_LAND else FORWARD_LAND
        return when {
            time <= hold -> from.copyOf()
            time < mid -> {
                val middle = middleQuad(from, to, liftPx)
                lerp(from, middle, (time - hold).toFloat() / (mid - hold))
            }

            time < land -> {
                val middle = middleQuad(from, to, liftPx)
                lerp(middle, to, (time - mid).toFloat() / (land - mid))
            }

            else -> to.copyOf()
        }
    }

    private fun middleQuad(from: FloatArray, to: FloatArray, liftPx: Float): FloatArray =
        FloatArray(8) { index ->
            val value = (from[index] + to[index]) / 2f
            if (index % 2 == 1) value - liftPx else value
        }

    private fun lerp(from: FloatArray, to: FloatArray, fraction: Float): FloatArray =
        FloatArray(8) { index -> from[index] + (to[index] - from[index]) * fraction }
}

/** 只负责把位图铺到当前四边形上；斜投影的“拉直”全靠四角插值。 */
internal class CoverFlightView(context: android.content.Context) : View(context) {

    private val matrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var bitmap: Bitmap? = null
    private var source = FloatArray(8)

    /** 四角顺序 TL / TR / BR / BL，坐标是覆盖层自己的坐标系 */
    var quad: FloatArray = FloatArray(8)
        set(value) {
            field = value
            invalidate()
        }

    fun setCover(cover: Bitmap) {
        bitmap = cover
        source = floatArrayOf(
            0f, 0f,
            cover.width.toFloat(), 0f,
            cover.width.toFloat(), cover.height.toFloat(),
            0f, cover.height.toFloat(),
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cover = bitmap ?: return
        matrix.setPolyToPoly(source, 0, quad, 0, 4)
        canvas.drawBitmap(cover, matrix, paint)
    }
}

/**
 * 详情页里的飞行覆盖层：底图 + 封面两层。
 *
 * 覆盖层挂在 `android.R.id.content` 上，和详情页同一个窗口：
 * Activity 首帧就带着它出现，因此从书架切过来时看到的第一帧和书架完全一致。
 */
internal class ArchiveCoverFlightOverlay(
    private val activity: Activity,
    private val flight: ArchiveCoverFlight,
) {

    private val root = FrameLayout(activity)
    private val backdrop = ImageView(activity)
    private val coverView = CoverFlightView(activity)
    private var animator: android.animation.ValueAnimator? = null
    private var removed = false

    private val density = activity.resources.displayMetrics.density
    private val liftPx = ArchiveFlightMotion.LIFT_DP * density

    /** 动画走完（不是被中途移除）时回调 */
    var onFinished: ((Boolean) -> Unit)? = null

    /** 去程：从书架的斜四边形飞到 [detailCover] 的当前矩形。 */
    fun showForward(detailCover: View) = show(returning = false, detailCover = detailCover)

    /** 回程：从 [detailCover] 的当前矩形飞回书架那本封面曾经的位置。 */
    fun showReturn(detailCover: View) = show(returning = true, detailCover = detailCover)

    private fun show(returning: Boolean, detailCover: View) {
        val parent = activity.window.findViewById<ViewGroup>(android.R.id.content)
        if (parent == null) {
            onFinished?.invoke(false)
            return
        }
        buildLayers(returning)
        parent.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        // 首帧绘制前一定已经完成布局；比 doOnLayout 稳（后者可能拿到第一趟 0 尺寸就再不触发）
        root.doOnPreDraw {
            if (removed) return@doOnPreDraw
            if (detailCover.width <= 0 || detailCover.height <= 0) {
                root.post {
                    if (!removed) {
                        remove()
                        onFinished?.invoke(false)
                    }
                }
                return@doOnPreDraw
            }
            startAnimation(returning, detailCover)
        }
    }

    private fun buildLayers(returning: Boolean) {
        backdrop.scaleType = ImageView.ScaleType.MATRIX
        backdrop.alpha = if (returning) 0f else 1f
        backdrop.setImageBitmap(flight.backdrop)
        root.addView(backdrop, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        coverView.setCover(flight.cover)
        root.addView(coverView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        root.isFocusable = false
        root.isFocusableInTouchMode = false
        // 过渡期间吃掉触摸：去程挡住落在还没露出来的详情页上的误触，回程挡住重复返回
        root.isClickable = true
    }

    private fun startAnimation(returning: Boolean, detailCover: View) {
        if (removed) return
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        val detail = screenRectOf(detailCover)
        // 去程：书架四角 → 详情页封面；回程：详情页封面 → 书架四角
        val startScreen = if (returning) detail else flight.shelfQuad
        val endScreen = if (returning) flight.shelfQuad else detail
        val start = FloatArray(8) { index -> startScreen[index] - origin[index % 2] }
        val end = FloatArray(8) { index -> endScreen[index] - origin[index % 2] }

        // 快照按 1:1 屏幕坐标摆放：两个 Activity 的内容区可能差一条状态栏，
        // 用各自内容区的屏幕原点相减，书架才会停在原来的位置上
        val scale = 1f / flight.backdropScale
        backdrop.imageMatrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                (flight.backdropOriginX - origin[0]).toFloat(),
                (flight.backdropOriginY - origin[1]).toFloat(),
            )
        }

        coverView.quad = ArchiveFlightMotion.quadAt(0, start, end, liftPx, returning)
        val duration = if (returning) ArchiveFlightMotion.RETURN_END else ArchiveFlightMotion.FORWARD_END
        animator = android.animation.ValueAnimator.ofFloat(0f, duration.toFloat()).apply {
            this.duration = duration
            // 时间轴自己带停顿与分段，这里必须线性，不能再叠一层默认的加减速
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { animation ->
                val time = (animation.animatedValue as Float).toLong()
                coverView.quad = ArchiveFlightMotion.quadAt(time, start, end, liftPx, returning)
                backdrop.alpha = ArchiveFlightMotion.backdropAlpha(time, returning)
            }
            doOnEnd {
                if (removed) return@doOnEnd
                // 回程不撤覆盖层：最后一帧留在窗口上随 Activity 一起消失，
                // 否则 finish 之前的那一帧会闪出详情页
                if (!returning) remove()
                onFinished?.invoke(true)
            }
            start()
        }
    }

    /** 视图在屏幕上的四角（TL / TR / BR / BL），用于回程起点/去程终点。 */
    private fun screenRectOf(view: View): FloatArray {
        val origin = IntArray(2)
        view.getLocationOnScreen(origin)
        val left = origin[0].toFloat()
        val top = origin[1].toFloat()
        val right = left + view.width
        val bottom = top + view.height
        return floatArrayOf(left, top, right, top, right, bottom, left, bottom)
    }

    fun remove() {
        if (removed) return
        removed = true
        animator?.cancel()
        animator = null
        (root.parent as? ViewGroup)?.removeView(root)
        backdrop.setImageDrawable(null)
        root.removeAllViews()
    }
}

private inline fun android.animation.ValueAnimator.doOnEnd(crossinline action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = action()
    })
}

/**
 * 书架页面的静态快照。
 *
 * [originX]/[originY] 是内容区左上角的屏幕坐标：详情页里的覆盖层按这个原点
 * 把自己平移过去，两个 Activity 状态栏处理不同也能对齐。
 */
internal class ArchiveBackdrop(
    val bitmap: Bitmap,
    val scale: Float,
    val originX: Int,
    val originY: Int,
)

/**
 * 把内容区画成半分辨率快照；调用前书架要把焦点封面藏起来（[ArchiveShelfView.suppressCoverForSnapshot]）。
 *
 * 抓的是 DecorView 而不是内容区：壁纸主题的背景画在 DecorView 上、内容区是半透明的，
 * 只画内容区会在快照里留下透明洞。按内容区在窗口里的偏移平移后，快照与内容区坐标一致。
 */
internal fun captureArchiveBackdrop(activity: Activity): ArchiveBackdrop? {
    val content = activity.window.findViewById<ViewGroup>(android.R.id.content) ?: return null
    val decor = activity.window.decorView
    val width = content.width
    val height = content.height
    if (width <= 0 || height <= 0 || decor.width <= 0 || decor.height <= 0) return null
    val contentLoc = IntArray(2)
    val decorLoc = IntArray(2)
    content.getLocationOnScreen(contentLoc)
    decor.getLocationOnScreen(decorLoc)
    val offsetX = (contentLoc[0] - decorLoc[0]).toFloat()
    val offsetY = (contentLoc[1] - decorLoc[1]).toFloat()
    val scale = ARCHIVE_BACKDROP_SCALE
    val bitmap = runCatching {
        val shot = Bitmap.createBitmap(
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(shot)
        canvas.scale(scale, scale)
        canvas.translate(-offsetX, -offsetY)
        decor.draw(canvas)
        shot
    }.getOrNull() ?: return null
    return ArchiveBackdrop(bitmap, scale, contentLoc[0], contentLoc[1])
}

/** 书架封面的裁剪结果：飞行位图 + 屏幕四角。 */
internal class ArchiveShelfCoverFlight(
    val cover: Bitmap,
    val sourceCover: Bitmap,
    val quad: FloatArray,
)
