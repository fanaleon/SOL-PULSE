# Pump Viewer

App Android nativa (Kotlin + Jetpack Compose) para seguir tokens de Solana.

- Pegás el contrato (mint) y empieza a seguir el token (precios de DexScreener, sin API key).
- Tarjetas con precio en vivo, variación de 5 min, 1 h y 24 h, volumen y mini gráfico.
- Detalle con gráfico que se puede recorrer con el dedo, máximo y mínimo, y las alertas marcadas.
- Alertas de precio: "sube a" / "baja a", con atajos de ±5% y ±10% y una barra que muestra cuánto falta.
- Widget en la pantalla de inicio, en lista o en una tira de una fila (4x1).
- Notificaciones de alta prioridad que el celu espeja al reloj.
- Servicio en segundo plano (15 s / 30 s / 1 min / 5 min) + WorkManager de respaldo cada 15 min.

## Compilar el APK con GitHub Actions

1. Subí los cambios a la rama `main` (o lanzá el workflow a mano con *Run workflow*).
2. En la pestaña **Actions** corre "Build APK".
3. Los APK quedan en **Releases** (`Pump Viewer build N`): `PumpViewer.apk` para el celu y `PumpViewerTV.apk` para la TV.
4. Bajalo al celu e instalalo (permití "instalar apps desconocidas").

Todas las builds se firman con la misma clave de prueba (`app/debug.keystore`), así que cada APK
nuevo se instala encima del anterior sin perder los tokens ni las alertas.

El mismo build publica aparte un release "Capturas de control" con imágenes de las pantallas y del
widget, dibujadas sin teléfono. Sirven para revisar cómo quedó un cambio de diseño antes de instalar.

Si el build falla, los errores del compilador aparecen arriba de todo en la corrida, como anotaciones.

### Si venías de "SolPulse"

Pump Viewer es una app distinta para Android (`app.pumpviewer` en vez de `app.solpulse`): se instala
al lado de la anterior y no hereda sus tokens ni sus alertas. Desinstalá SolPulse, instalá Pump Viewer
y volvé a cargar los tokens.

## Pump Viewer TV (Android TV)

Módulo `tv/`: la misma app pensada para la tele y el control remoto (`app.pumpviewer.tv`, se instala
al lado de la del celu y no comparten datos).

- Tablero con una tarjeta por token (precio grande, variación, mini gráfico, 5 min / 1 h / market cap) y reloj.
- Se maneja con las flechas y OK; lo enfocado se agranda y se marca con borde cian. Atrás vuelve.
- Detalle con gráfico grande, liquidez, volumen y alertas (atajos ±5% / ±10%). Cuando una alerta
  se cumple, aparece un aviso amarillo arriba de todo.
- Mantiene la pantalla encendida y baja los precios cada 20 s mientras está abierta.
- **Idioma**: inglés y español. El botón "Idioma · …" del tablero alterna Auto → English → Español.
  "Auto" sigue el idioma de la TV (español si la TV está en español, inglés en cualquier otro caso).
  El formulario web que se abre en el celu también sale en el idioma elegido.
  Los textos están en `tv/src/main/res/values/strings.xml` (inglés) y `values-es/strings.xml` (español);
  para sumar otro idioma alcanza con crear otro `values-xx/strings.xml`.
- **Agregar tokens sin escribir con el control**: en "Agregar token" la TV muestra una dirección
  `http://IP:8080`; abrila en el navegador del celu (mismo Wi‑Fi), pegá los mints y tocá "Enviar a la TV".
  El servidor solo corre mientras esa pantalla está abierta. También se puede escribir el mint a mano.

Instalarla en la TV: pasá `PumpViewerTV.apk` con la app *Downloader* (pegando la URL del release),
con "Send Files to TV" o por `adb install PumpViewerTV.apk`.

## El widget

Mantené apretado un lugar vacío de la pantalla de inicio › Widgets › Pump Viewer. Hay dos:

| Widget | Tamaño al agregarlo | Qué muestra |
| --- | --- | --- |
| Pump Viewer: lista | 4x2 | Una fila por token: nombre, mini gráfico, precio y variación de 24 h |
| Pump Viewer: tira 4x1 | 4x1 | Con un token: nombre, mini gráfico, precio y variación. Con varios: dos o tres en columnas |

Los dos se pueden agrandar o achicar: el contenido se acomoda solo al tamaño (por debajo de una
fila y media pasa a tira; por encima, a lista).

## Para que lleguen las alertas al reloj (Xiaomi / Amazfit / Huawei)

1. Aceptá el permiso de notificaciones al abrir la app.
2. Ajustes del celu › Apps › Pump Viewer › Ahorro de batería: **Sin restricciones**, e **Inicio automático** activado.
3. En la app del reloj (Mi Fitness / Zepp / Huawei Health) activá las notificaciones para **Pump Viewer**.
4. En Pump Viewer › Ajustes tocá **Probar una alerta en el reloj** para verificar.

Nota: muchos relojes solo reciben las notificaciones cuando la pantalla del celu está apagada o bloqueada.

## Cómo funciona

- `core/` — módulo compartido con el celu y la TV: modelos, repositorio persistente, cliente DexScreener y formato de números.
- `tv/` — Pump Viewer TV (ver arriba).
- `app/data/` — chequeo de alertas del celu.
- `service/` — servicio en primer plano, worker de respaldo, notificaciones.
- `widget/` — widget de pantalla de inicio (Glance), que elige el diseño según su tamaño.
- `ui/` — pantallas Compose (lista, detalle, agregar token, ajustes). La paleta está en `Theme.kt`.
- `src/test/` — genera las capturas de control.

Cada alerta se dispara una sola vez y queda pausada; la podés volver a activar con el interruptor.
