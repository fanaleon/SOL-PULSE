package app.solpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Cliente de DexScreener (gratis, sin API key). Elige el par con más liquidez de cada token. */
object DexApi {
    private const val BASE = "https://api.dexscreener.com/tokens/v1/solana/"

    /** Devuelve un mapa mint -> Token. Lanza excepción si falla la red. */
    suspend fun fetch(mints: List<String>): Map<String, Token> = withContext(Dispatchers.IO) {
        val result = HashMap<String, Token>()
        for (chunk in mints.distinct().chunked(30)) {
            val conn = URL(BASE + chunk.joinToString(",")).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
                val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                val arr: JSONArray =
                    if (body.startsWith("[")) JSONArray(body)
                    else JSONObject(body).optJSONArray("pairs") ?: JSONArray()

                val best = HashMap<String, JSONObject>()
                for (i in 0 until arr.length()) {
                    val pair = arr.optJSONObject(i) ?: continue
                    val addr = pair.optJSONObject("baseToken")?.optString("address").orEmpty()
                    if (addr.isEmpty()) continue
                    val liq = pair.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0
                    val cur = best[addr]
                    val curLiq = cur?.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0
                    if (cur == null || liq > curLiq) best[addr] = pair
                }
                for ((addr, pair) in best) result[addr] = parse(addr, pair)
            } finally {
                conn.disconnect()
            }
        }
        result
    }

    private fun parse(mint: String, p: JSONObject): Token {
        val base = p.optJSONObject("baseToken")
        val chg = p.optJSONObject("priceChange")
        val img = p.optJSONObject("info")?.optString("imageUrl").orEmpty()
        val url = p.optString("url")
        return Token(
            mint = mint,
            symbol = base?.optString("symbol").orEmpty().ifBlank { "?" },
            name = base?.optString("name").orEmpty().takeIf { it != "null" }.orEmpty(),
            imageUrl = img.takeIf { it.startsWith("http") },
            priceUsd = p.optString("priceUsd").toDoubleOrNull() ?: 0.0,
            change5m = chg?.optDouble("m5", 0.0) ?: 0.0,
            change1h = chg?.optDouble("h1", 0.0) ?: 0.0,
            change24h = chg?.optDouble("h24", 0.0) ?: 0.0,
            liquidityUsd = p.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0,
            volume24h = p.optJSONObject("volume")?.optDouble("h24", 0.0) ?: 0.0,
            marketCap = p.optDouble("marketCap", p.optDouble("fdv", 0.0)),
            pairUrl = url.takeIf { it.startsWith("http") }
        )
    }
}
