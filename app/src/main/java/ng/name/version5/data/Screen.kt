package ng.name.version5.data

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

object Screen {
    @Suppress("DEPRECATION")
    fun rotation(ctx: Context): Int =
        (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

    /** Full physical display size in the CURRENT orientation (includes system bars). */
    @Suppress("DEPRECATION")
    fun realSize(ctx: Context): Point = Point().also {
        (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(it)
    }

    fun naturalSize(ctx: Context): Point {
        val s = realSize(ctx); val r = rotation(ctx)
        return if (r == 1 || r == 3) Point(s.y, s.x) else s
    }

    fun xdpi(ctx: Context): Float {
        val d = ctx.resources.displayMetrics
        return if (d.xdpi > 50f) d.xdpi else d.densityDpi.toFloat()
    }
}
