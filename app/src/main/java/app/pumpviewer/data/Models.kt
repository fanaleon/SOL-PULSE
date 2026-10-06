package app.pumpviewer.data

data class Token(
    val mint: String,
    val symbol: String,
    val name: String,
    val imageUrl: String? = null,
    val priceUsd: Double = 0.0,
    val change5m: Double = 0.0,
    val change1h: Double = 0.0,
    val change24h: Double = 0.0,
    val liquidityUsd: Double = 0.0,
    val volume24h: Double = 0.0,
    val marketCap: Double = 0.0,
    val pairUrl: String? = null,
    val updatedAt: Long = 0L,
    val history: List<Double> = emptyList()
)

enum class AlertType { ABOVE, BELOW }

data class PriceAlert(
    val id: String,
    val mint: String,
    val type: AlertType,
    val target: Double,
    val enabled: Boolean = true
)
