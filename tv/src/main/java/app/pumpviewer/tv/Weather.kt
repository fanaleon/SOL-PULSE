package app.pumpviewer.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Una ciudad para el clima. */
data class Place(
    val name: String,
    val region: String,
    val lat: Double,
    val lon: Double
)

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

data class WeatherInfo(
    val temp: Double,
    val sky: Sky,
    val isDay: Boolean,
    val windKmh: Double,
    val max: Double,
    val min: Double
)

/** Guarda la ciudad elegida (aparte de los tokens). */
object PlaceStore {
    private const val PREFS = "tv_settings"

    // Por defecto Añelo; se cambia desde el clima del tablero.
    private val DEFAULT = Place("Añelo", "Neuquén, Argentina", -38.3533, -68.7867)

    fun load(context: Context): Place {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString("place_name", null) ?: return DEFAULT
        val lat = p.getString("place_lat", null)?.toDoubleOrNull() ?: return DEFAULT
        val lon = p.getString("place_lon", null)?.toDoubleOrNull() ?: return DEFAULT
        return Place(name, p.getString("place_region", "") ?: "", lat, lon)
    }

    fun save(context: Context, place: Place) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("place_name", place.name)
            .putString("place_region", place.region)
            .putString("place_lat", place.lat.toString())
            .putString("place_lon", place.lon.toString())
            .apply()
    }
}

/** Open-Meteo: clima y buscador de ciudades, gratis y sin clave. */
object WeatherApi {
    suspend fun current(place: Place): WeatherInfo {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.lat}&longitude=${place.lon}" +
            "&current=temperature_2m,weather_code,is_day,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
        val root = getJson(url)
        val cur = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        return WeatherInfo(
            temp = cur.getDouble("temperature_2m"),
            sky = skyOf(cur.optInt("weather_code", 3)),
            isDay = cur.optInt("is_day", 1) == 1,
            windKmh = cur.optDouble("wind_speed_10m", 0.0),
            max = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, Double.NaN) ?: Double.NaN,
            min = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, Double.NaN) ?: Double.NaN
        )
    }

    suspend fun search(query: String, lang: String): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val root = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=5&language=$lang&format=json")
        val arr = root.optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            Place(
                name = r.getString("name"),
                region = listOf(r.optString("admin1", ""), r.optString("country", ""))
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                lat = r.getDouble("latitude"),
                lon = r.getDouble("longitude")
            )
        }
    }

    /** Códigos WMO de Open-Meteo a un tipo de cielo. */
    private fun skyOf(code: Int): Sky = when (code) {
        0, 1 -> Sky.CLEAR
        2 -> Sky.PARTLY
        3 -> Sky.CLOUDY
        45, 48 -> Sky.FOG
        in 51..57 -> Sky.DRIZZLE
        in 61..67, in 80..82 -> Sky.RAIN
        in 71..77, 85, 86 -> Sky.SNOW
        in 95..99 -> Sky.STORM
        else -> Sky.CLOUDY
    }

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }
}
