# SolPulse

App Android nativa (Kotlin + Jetpack Compose) para seguir tokens de Solana.

- Pegás el contrato (mint) y empieza a seguir el token (precios de DexScreener, sin API key).
- Alertas de precio: "sube a" / "baja a", con atajos de ±5% y ±10%.
- Widget en la pantalla de inicio (hasta 5 tokens con precio y variación 24h).
- Notificaciones de alta prioridad que el celu espeja al reloj.
- Servicio en segundo plano (15 s / 30 s / 1 min / 5 min) + WorkManager de respaldo cada 15 min.

## Compilar el APK con GitHub Actions

1. Creá un repo nuevo en GitHub (por ejemplo `solpulse`) y subí todo el contenido de esta carpeta,
   incluida la carpeta oculta `.github`.
2. En la pestaña **Actions** corre solo "Build APK" (o lo lanzás a mano con *Run workflow*).
3. Al terminar, el APK queda en **Releases** (`SolPulse build N`) y como artefacto del workflow.
4. Bajalo al celu e instalalo (permití "instalar apps desconocidas").

Si el build falla, abrí el paso "Build debug APK", copiá el error y pasámelo.

## Para que lleguen las alertas al reloj (Xiaomi / Amazfit / Huawei)

1. Aceptá el permiso de notificaciones al abrir la app.
2. Ajustes del celu › Apps › SolPulse › Ahorro de batería: **Sin restricciones**, e **Inicio automático** activado.
3. En la app del reloj (Mi Fitness / Zepp / Huawei Health) activá las notificaciones para **SolPulse**.
4. En SolPulse › Ajustes tocá **Probar notificación** para verificar.

Nota: muchos relojes solo reciben las notificaciones cuando la pantalla del celu está apagada o bloqueada.

## Cómo funciona

- `data/` — modelos, repositorio persistente, cliente DexScreener y chequeo de alertas.
- `service/` — servicio en primer plano, worker de respaldo, notificaciones.
- `widget/` — widget de pantalla de inicio (Glance).
- `ui/` — pantallas Compose (lista, detalle, agregar token, ajustes).

Cada alerta se dispara una sola vez y queda pausada; la podés volver a activar con el interruptor.
