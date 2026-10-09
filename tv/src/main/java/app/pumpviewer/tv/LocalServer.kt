package app.pumpviewer.tv

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

/**
 * Mini servidor web para cargar tokens sin escribir con el control remoto:
 * mientras la pantalla "Agregar" está abierta, el celu entra a http://IP-DE-LA-TV:8080,
 * pega los mints y los manda. Solo escucha en la red local y solo mientras esa pantalla está abierta.
 */
class LocalServer(private val onMints: (String) -> Unit) {
    @Volatile
    private var running = false
    private var server: ServerSocket? = null

    fun start() {
        if (running) return
        running = true
        thread(isDaemon = true, name = "pump-local-server") {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress(PORT))
                server = ss
                while (running) {
                    val client = try {
                        ss.accept()
                    } catch (e: Exception) {
                        break
                    }
                    try {
                        handle(client)
                    } catch (e: Exception) {
                        // una conexión rara no tiene que tumbar el servidor
                    } finally {
                        try {
                            client.close()
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                // puerto ocupado u otro problema: la pantalla igual permite escribir el mint a mano
            }
        }
    }

    fun stop() {
        running = false
        try {
            server?.close()
        } catch (e: Exception) {
        }
        server = null
    }

    private fun handle(client: Socket) {
        client.soTimeout = 5_000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.ISO_8859_1))
        val requestLine = reader.readLine() ?: return
        var contentLength = 0
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
            }
        }

        var message: String? = null
        if (requestLine.startsWith("POST") && contentLength in 1..20_000) {
            val buf = CharArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = reader.read(buf, read, contentLength - read)
                if (n < 0) break
                read += n
            }
            val body = String(buf, 0, read)
            val mints = body.split('&')
                .firstOrNull { it.startsWith("mints=") }
                ?.substringAfter('=')
                ?.let { URLDecoder.decode(it, "UTF-8") }
                .orEmpty()
            if (mints.isNotBlank()) {
                onMints(mints)
                message = "Listo, se los mandé a la TV. Mirá la pantalla."
            } else {
                message = "No llegó ninguna dirección. Probá de nuevo."
            }
        }

        val html = page(message)
        val bytes = html.toByteArray(Charsets.UTF_8)
        val out = client.getOutputStream()
        out.write(
            (
                "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"
                ).toByteArray(Charsets.ISO_8859_1)
        )
        out.write(bytes)
        out.flush()
    }

    private fun page(message: String?): String = """
        <!doctype html>
        <html lang="es"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Pump Viewer TV</title>
        <style>
        body{margin:0;padding:24px;background:#0a0c1b;color:#f7f8ff;font-family:system-ui,sans-serif}
        h1{font-size:24px;margin:0 0 6px}
        p{color:#aeb6dc;line-height:1.4}
        textarea{width:100%;box-sizing:border-box;min-height:140px;padding:12px;border-radius:12px;
        border:1px solid #313a68;background:#151930;color:#f7f8ff;font-size:16px}
        button{margin-top:14px;width:100%;padding:16px;border:0;border-radius:14px;
        background:#7c4dff;color:#fff;font-size:18px;font-weight:700}
        .ok{background:#12372b;color:#2df0a6;padding:12px;border-radius:12px;margin-bottom:16px}
        </style></head><body>
        <h1>Pump Viewer TV</h1>
        <p>Pegá una o varias direcciones de token (mint de Solana), una por línea.</p>
        ${if (message != null) "<div class=\"ok\">$message</div>" else ""}
        <form method="post" action="/">
        <textarea name="mints" placeholder="Dirección del token..."></textarea>
        <button type="submit">Enviar a la TV</button>
        </form></body></html>
    """.trimIndent()

    companion object {
        const val PORT = 8080

        /** IP de la TV en la red local (la que hay que escribir en el celu), o null si no hay red. */
        fun localIp(): String? = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
