package io.legado.app.ui.main.bookshelf.archive

import android.content.Context
import android.graphics.Color
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.ThemeStore
import kotlin.math.roundToInt

/**
 * 档案式书架的配色工具。
 */
object ArchivePalette {

    /**
     * 纸面/面板颜色必须是**不透明**的。
     *
     * 主题里可以把背景色设成半透明（配合背景图壁纸很常见），直接用这个颜色铺纸面或抽屉面板，
     * 壁纸会透过来、上下层文字叠在一起。这里把半透明背景色合成到「浅色主题白底 /
     * 深色主题黑底」上，得到一个不透明的实心色。
     */
    fun paper(context: Context): Int {
        val background = runCatching { ThemeStore.backgroundColor(context) }
            .getOrDefault(Color.WHITE)
        if (Color.alpha(background) == 255) return background
        val base = if (AppConfig.isNightTheme) Color.BLACK else Color.WHITE
        return blendOver(background, base)
    }

    /** src 叠到不透明的 dst 上，返回不透明结果 */
    private fun blendOver(src: Int, dst: Int): Int {
        val a = Color.alpha(src) / 255f
        return Color.rgb(
            (Color.red(src) * a + Color.red(dst) * (1f - a)).roundToInt(),
            (Color.green(src) * a + Color.green(dst) * (1f - a)).roundToInt(),
            (Color.blue(src) * a + Color.blue(dst) * (1f - a)).roundToInt()
        )
    }
}
