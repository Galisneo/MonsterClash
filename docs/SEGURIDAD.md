# Seguridad aplicada

Monster Clash funciona sin servidor, sin API, sin WebView, sin cookies y sin permiso de Internet. Por ese motivo HSTS y las cookies `Secure`/`HttpOnly` no tienen un punto de aplicación dentro de esta versión: no existe una sesión HTTP que proteger.

Sí quedan aplicadas las medidas equivalentes que corresponden a una app Android local:

- No se permite tráfico HTTP sin cifrar si en el futuro se añade una conexión (`usesCleartextTraffic="false"` y `res/xml/red_segura.xml`).
- La entrada de usuarios usa una lista blanca ASCII de 3–20 caracteres (`a-z`, `A-Z`, `0-9` y `_`). El email se normaliza y valida, y las respuestas se normalizan antes de almacenarse.
- Las consultas SQLite usan parámetros; no se concatena texto introducido por el usuario.
- Login y recuperación tienen contadores separados, bloqueo tras 5 fallos, bloqueo resistente a cambios de hora y nombres ficticios para evitar enumeración de cuentas.
- El cambio de contraseña exige un token de un solo uso en memoria y con caducidad.
- Las contraseñas y respuestas usan PBKDF2-HMAC-SHA256 con salt aleatorio. Los hashes con parámetros corruptos o excesivos se rechazan para evitar esperas abusivas.
- Las Activities internas no están exportadas y las copias/transferencias de datos están desactivadas.

## Música

El menú usa una pista propia y cada combate rota entre tres pistas chiptune originales generadas matemáticamente en `MusicaJuego.java`. Buscan un ambiente de estrategia retro de los años 90, con progresiones, cambios de tonalidad y frases largas, sin copiar melodías de juegos comerciales. La generación se hace en un hilo aparte, con mezcla a volumen moderado, armónicos suaves y fundidos en los extremos del bucle para evitar saturación y clics. Al volver al menú, la pista del combate no puede apagar la del menú por una carrera entre Activities. Los botones tienen además efectos de selección y confirmación de menos de 130 ms. No se descargan archivos ni se incluyen samples de terceros.
