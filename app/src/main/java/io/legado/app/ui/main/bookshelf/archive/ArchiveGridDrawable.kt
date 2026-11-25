package io.legado.app.ui.main.bookshelf.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import io.legado.app.lib.theme.primaryTextColor

/**
 * 档案纸的格子底纹。
 *
 * 和立体书架原来那张网格同一口径：56dp 一格、墨色 12/255、0.5dp 线宽；
 * 底色用 [ArchivePalette.paper]（不透明纸面，半透明主题背景会先合成到白/黑底）。
 *
 * 作为整个窗口的背景铺在每个界面上，书架页、阅读统计、抽屉都是同一张纸；
 * 内容卡片仍然盖在上面，所以只在留白处看得到格子。
 */
class ArchiveGridDrawable(context: Context) : Drawable() {

    private val density = context.resources.displayMetrics.density
    private val step = 56f * density
    private val paperPaint = Paint().apply { color = ArchivePalette.paper(context) }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(context.primaryTextColor, 12)
        strokeWidth = 0.5f * density
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        canvas.drawRect(
            bounds.left.toFloat(), bounds.top.toFloat(),
            bounds.right.toFloat(), bounds.bottom.toFloat(),
            paperPaint
        )
        var x = bounds.left.toFloat()
        while (x <= bounds.right) {
            canvas.drawLine(x, bounds.top.toFloat(), x, bounds.bottom.toFloat(), linePaint)
            x += step
        }
        var y = bounds.top.toFloat()
        while (y <= bounds.bottom) {
            canvas.drawLine(bounds.left.toFloat(), y, bounds.right.toFloat(), y, linePaint)
            y += step
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
