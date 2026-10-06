package app.pumpviewer.data

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import kotlin.math.abs

object Fmt {
    /** Precio con formato legible, incluso para precios muy chicos (memecoins). */
    fun price(p: Double): String = when {
        p <= 0.0 -> "\$0"
        p >= 1000 -> "\$" + String.format(Locale.US, "%,.2f", p)
        p >= 1 -> "\$" + String.format(Locale.US, "%.3f", p)
        else -> "\$" + plain(p, 4)
    }

    /** Número sin notación científica, con N cifras significativas. */
    fun plain(p: Double, digits: Int = 6): String {
        if (p.isNaN() || p.isInfinite() || p == 0.0) return "0"
        return BigDecimal(p).round(MathContext(digits)).stripTrailingZeros().toPlainString()
    }

    fun compact(v: Double): String = when {
        v >= 1e9 -> String.format(Locale.US, "\$%.2fB", v / 1e9)
        v >= 1e6 -> String.format(Locale.US, "\$%.2fM", v / 1e6)
        v >= 1e3 -> String.format(Locale.US, "\$%.1fK", v / 1e3)
        v > 0 -> String.format(Locale.US, "\$%.0f", v)
        else -> "—"
    }

    fun pct(x: Double): String = String.format(Locale.US, "%+.2f%%", x)

    fun pctAbs(x: Double): String = String.format(Locale.US, "%.2f%%", abs(x))

    fun shortMint(m: String): String =
        if (m.length > 12) m.take(5) + "…" + m.takeLast(5) else m
}
