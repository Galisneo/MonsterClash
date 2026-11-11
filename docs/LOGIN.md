# Login offline de Monster Clash

Memoria corta de cómo he hecho las cuentas de usuario. Todo pasa dentro del móvil: no hay servidor, ni API, ni permiso de Internet. Las cuentas viven en una base de datos SQLite en la carpeta privada de la app y el código está en el paquete `com.juego.combates.datos`.

## Tablas

Base de datos `monsterclash.db`, versión 2 (`BaseDatos.java`, un `SQLiteOpenHelper` singleton; `onUpgrade` migra la versión 1 con `ALTER TABLE` sin perder cuentas):

```
usuarios
  id                    INTEGER PRIMARY KEY AUTOINCREMENT
  usuario               TEXT NOT NULL UNIQUE      -- siempre en minúsculas
  email                 TEXT UNIQUE               -- NULL si no lo dio
  password_hash         TEXT NOT NULL             -- ver "Contraseñas"
  pregunta              INTEGER NOT NULL          -- índice en preguntas_seguridad (arrays.xml)
  respuesta_hash        TEXT NOT NULL             -- la respuesta, normalizada y hasheada
  creado_en             INTEGER NOT NULL          -- ms desde 1970
  intentos_fallidos     INTEGER NOT NULL DEFAULT 0
  intentos_recuperacion INTEGER NOT NULL DEFAULT 0
  bloqueado_en          INTEGER NOT NULL DEFAULT 0   -- reloj de pared al bloquear
  bloqueado_hasta       INTEGER NOT NULL DEFAULT 0   -- reloj de pared al que acaba
  bloqueo_elapsed_hasta INTEGER NOT NULL DEFAULT 0   -- SystemClock.elapsedRealtime al que acaba
  bloqueo_arranque      INTEGER NOT NULL DEFAULT -1  -- BOOT_COUNT del móvil al bloquear

intentos_nombre         -- los mismos contadores, pero para nombres que NO existen
  nombre                TEXT PRIMARY KEY
  (las seis columnas de contadores y bloqueo de arriba)

ajustes                 -- pares clave/valor de la app (hoy solo semilla_preguntas)
  clave                 TEXT PRIMARY KEY
  valor                 TEXT NOT NULL

partidas
  id          INTEGER PRIMARY KEY AUTOINCREMENT
  usuario_id  INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE
  victoria    INTEGER NOT NULL      -- 1 / 0
  equipo      TEXT NOT NULL         -- "Fogarto,Acuarin,Hojaruz"
  turnos      INTEGER
  jugada_en   INTEGER NOT NULL
  índice: idx_partidas_usuario (usuario_id)
```

Las claves ajenas las activo en `onConfigure` con `setForeignKeyConstraintsEnabled(true)`, porque SQLite por defecto no las comprueba. Todas las consultas van con parámetros `?` (`selectionArgs`), nunca concatenando texto del usuario en el SQL.

## Contraseñas

Nunca guardo la contraseña. Guardo un hash **PBKDF2-HMAC-SHA256** con:

- **salt** de 16 bytes aleatorios (`SecureRandom`), distinto por usuario: dos personas con la misma contraseña tienen hashes distintos y no sirven las tablas precalculadas;
- **120000 iteraciones**, para que probar contraseñas a lo bruto sea lento a propósito (100-400 ms por intento en un móvil);
- salida de 32 bytes.

Lo guardo todo en una sola cadena, para que si algún día subo las iteraciones los hashes viejos sigan valiendo:

```
pbkdf2_sha256$120000$<salt en hex>$<hash en hex>
```

El PBKDF2 lo he implementado a mano en `Hash.java` encima de `javax.crypto.Mac("HmacSHA256")` porque con `minSdk 24` no está garantizado `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")`. La clase no importa nada de `android.*`, así que la compilo con el `javac` del PC y `pruebas/com/juego/combates/datos/PruebaHash.java` (comandos en `pruebas/README.md`) comprueba mi implementación contra los vectores de prueba conocidos (los de "password"/"salt" y los del RFC 7914) y contra el PBKDF2 del JDK. Al verificar comparo con `MessageDigest.isEqual`, que tarda lo mismo acierte o no.

## Bloqueo por intentos

Las reglas del bloqueo están en `ControlBloqueo.java` (cuántos fallos, cuánto dura y cómo se mide el tiempo); `RepositorioUsuarios` solo las aplica y guarda el resultado en SQLite. `ControlBloqueo.apuntarFallo` suma uno a `intentos_fallidos` con cada contraseña mala y, al llegar a **5 fallos seguidos**, arma un bloqueo de 15 minutos; `RepositorioUsuarios.autenticar()` guarda ese estado y devuelve `BLOQUEADO(minutos)`; la pantalla dice "Demasiados intentos. Espera N minutos." (o "1 minuto": el texto es un `<plurals>`). Solo un login correcto deja el contador a cero (y de paso también `intentos_recuperacion`: si sabe la contraseña, es él): al bloquear **no** lo reinicio, así que si alguien consiguiera saltarse el bloqueo solo ganaría un intento (el siguiente fallo vuelve a bloquear otros 15 minutos), no cinco.

**El bloqueo no depende solo de la hora del móvil.** El único atacante realista de un login offline es quien tiene el móvil en la mano, y ese puede adelantar el reloj en Ajustes en dos toques. Por eso al bloquear apunto tres cosas (`EstadoIntentos.java`): el reloj de pared (`bloqueado_en`, `bloqueado_hasta`), `SystemClock.elapsedRealtime()` (`bloqueo_elapsed_hasta`, un contador que solo avanza y nadie puede tocar, pero que vuelve a cero al reiniciar) y el número de arranques del móvil (`bloqueo_arranque`, `Settings.Global.BOOT_COUNT`). Al comprobar (`ControlBloqueo.minutosDeBloqueo`):

- si el móvil **no se ha reiniciado** desde el bloqueo (mismo `BOOT_COUNT`), manda `elapsedRealtime`: cambiar la hora ni acorta ni alarga el bloqueo (esto también arregla el caso contrario: si el reloj iba adelantado al bloquear y luego se corrige, la cuenta no se queda bloqueada horas);
- si **se ha reiniciado**, solo me queda el reloj de pared; si veo que ha ido hacia atrás respecto a `bloqueado_en`, es que lo han movido y rearmo 15 minutos desde ese momento;
- los minutos que enseño van acotados a 15, nunca "espera 3 días".

**Límite que asumo**: reiniciar el móvil **y** adelantar la hora sí se salta el bloqueo (sin reloj de confianza no hay forma de evitarlo offline), pero como el contador no se reinicia solo se gana un intento por cada reinicio + cambio de hora.

**Nadie puede averiguar qué usuarios hay.** Si el nombre no existe hago exactamente lo mismo que si existiera: verifico la contraseña contra un hash ficticio precalculado (tarda lo mismo), devuelvo el mismo mensaje ("Usuario o contraseña incorrectos.") y llevo los mismos contadores y el mismo bloqueo, guardados por nombre en la tabla `intentos_nombre` (si luego alguien registra ese nombre, borro su fila). Así un nombre inventado también dice "Demasiados intentos" al quinto fallo, igual que una cuenta real.

## Recuperar la contraseña

Como no hay servidor no puedo mandar un email, así que uso una **pregunta de seguridad** elegida al crear la cuenta (6 preguntas en `res/values/arrays.xml`; en la BD guardo solo el índice). `RecuperarActivity` va en tres pasos: usuario → responder la pregunta → contraseña nueva dos veces.

El paso 1 **nunca dice "ese usuario no existe"**: sería la forma más barata de enumerar cuentas y dejaría en nada el hash ficticio del login. Si el nombre no existe, `obtenerPregunta` devuelve una pregunta inventada y la pantalla pasa al paso 2 igual; allí `verificarRespuesta` compara contra el hash ficticio, responde siempre "Respuesta incorrecta." y bloquea a los 5 fallos como con una cuenta real (contadores en `intentos_nombre`). La pregunta inventada tiene que ser **siempre la misma para el mismo nombre** (si cambiara entre intentos se notaría) y **no adivinable** (si fuera una fórmula fija del nombre, ver otra pregunta delataría que la cuenta es real): la saco con SHA-256 de una semilla aleatoria generada en el móvil la primera vez (tabla `ajustes`) más el nombre.

La respuesta se **normaliza** antes de hashearla (`Hash.normalizarRespuesta`: sin espacios de los lados, minúsculas, un solo espacio entre palabras, sin tildes) para que "Tobías" y "tobias" valgan igual, y se guarda hasheada igual que la contraseña. Los fallos tienen su propio contador (`intentos_recuperacion`) con el **mismo bloqueo de 15 minutos tras 5 fallos**. Acertar la pregunta pone `intentos_recuperacion` a cero y llama a `quitarBloqueo()` para borrar los datos de un bloqueo ya vencido (si estuviera vigente no se llega a comprobar la respuesta: `minutosDeBloqueo` devuelve antes); cambiar la contraseña pone los dos contadores a cero y quita el bloqueo; un login correcto también deja los dos a cero.

**El paso 3 exige un token del paso 2.** Al acertar la pregunta, `verificarRespuesta` devuelve `OK` con un token opaco: 16 bytes de `SecureRandom` en hexadecimal que `TokenRecuperacion.java` (un campo del repositorio) guarda **en memoria** (no en la base de datos) junto al usuario normalizado, y que caduca a los **10 minutos** medidos con `elapsedRealtime`, como el bloqueo. Sus dos métodos, `crear` y `consumir`, son `synchronized` porque el paso 2 y el paso 3 llegan desde Threads distintos. `cambiarPassword(usuario, token, nuevaPassword)` lo exige, lo compara en tiempo constante con `MessageDigest.isEqual` y lo gasta al comprobarlo, valga o no (un solo uso; solo hay uno a la vez, acertar otra pregunta invalida el anterior). Sin el token, bastaría llamar a `cambiarPassword` para saltarse la pregunta. `RecuperarActivity` es quien lleva el token del paso 2 al 3; si el repositorio lo rechaza (`TOKEN_INVALIDO`), la pantalla vuelve al paso 2 con "Ha pasado demasiado tiempo. Responde otra vez a la pregunta.". Como el token vive en memoria, cerrar la app también obliga a responder otra vez.

**Contadores sin carreras.** Leer los contadores de fallos y volver a escribirlos (en `usuarios` y en `intentos_nombre`, tanto al entrar como al responder la pregunta) va dentro de `beginTransaction()` / `setTransactionSuccessful()` / `endTransaction()`, para que dos intentos a la vez no se pisen (los dos leen 3, los dos escriben 4). El registro también: comprobar que el nombre y el email no existen e insertar van en la misma transacción, y si aun así saltara el `UNIQUE`, vuelvo a consultar para decir si lo repetido es el usuario o el email.

**Límite que asumo**: quien conozca la respuesta de seguridad puede cambiar la contraseña. Es lo que hay sin servidor ni email; por eso la respuesta va hasheada (para que ni con root se lea) y se limita a 5 intentos.

## Qué protege y qué no

- Sin root, ninguna otra app puede leer la base de datos ni las `SharedPreferences` (`MODE_PRIVATE`), porque están en la carpeta privada de la app.
- Con root se podría abrir `monsterclash.db`, pero se verían **hashes**, no contraseñas. Con 120000 iteraciones y salt, sacar una contraseña de 8+ caracteres con letra y número no es cosa de un rato.
- `android:allowBackup="false"` en el manifest (junto con `android:fullBackupContent="false"` para Android 11 o menos): la base de datos no sale en las copias de seguridad de Google ni con `adb backup`. Pero con `targetSdk 34` eso **no basta**: en Android 12+ `allowBackup="false"` no impide la transferencia directa móvil-a-móvil al estrenar teléfono. Por eso añado `android:dataExtractionRules="@xml/reglas_extraccion"`, que excluye la base de datos y las `SharedPreferences` tanto de `<cloud-backup>` como de `<device-transfer>`. Las cuentas son de ese móvil y no viajan al siguiente.
- La sesión (`Sesion.java`) guarda solo el **id y el nombre** de usuario en `SharedPreferences`. No la cifro porque no hay ningún secreto en ella: es lo mismo que se ve en el menú.
- **El registro sí revela si un usuario existe**: "Ese usuario ya existe" y "Ese email ya está registrado" son, por definición, la confirmación de que hay una cuenta con ese nombre o ese email. No lo puedo evitar sin dejar de avisar a quien quiere crear una cuenta con un nombre pillado, así que lo asumo: enumerar cuentas desde la pestaña "Crear cuenta" es posible, aunque a mano y de una en una. El login y la recuperación, en cambio, no lo revelan ni por el mensaje, ni por el tiempo, ni por el bloqueo.
- No protege contra alguien que tenga el móvil desbloqueado en la mano: ese ya está dentro de la app con la sesión abierta. Para eso está "Cerrar sesión".
- No hay sincronización entre móviles: las cuentas son de ese dispositivo.

## Cómo probarlo

1. Compilar `Hash.java` junto a `PruebaHash.java` en el PC y ejecutar la autocomprobación (tiene que acabar en `TODO OK`; si algo falla, la última línea es `FALLO`):
   ```
   javac -encoding UTF-8 -d pruebas/salida app/src/main/java/com/juego/combates/datos/Hash.java pruebas/com/juego/combates/datos/PruebaHash.java
   java -cp pruebas/salida com.juego.combates.datos.PruebaHash
   ```
2. `./gradlew assembleDebug` e instalar el APK.
3. En el móvil: crear una cuenta (pestaña "Crear cuenta", elegir pregunta y respuesta; entra directamente sin volver a hashear), cerrar sesión desde el menú, entrar con ella; probar 5 contraseñas malas y ver el bloqueo; adelantar la hora en Ajustes y comprobar que sigue bloqueada; "¿Has olvidado tu contraseña?" → usuario → respuesta → contraseña nueva → volver a entrar (y repetirlo dejando pasar más de 10 minutos entre la respuesta y la contraseña nueva: tiene que devolver al paso 2). Probar también con un usuario inventado: el login y la recuperación tienen que responder igual que con uno real (misma pregunta cada vez, "Respuesta incorrecta." y bloqueo al quinto fallo). Jugar un combate con sesión y comprobar que las estadísticas del menú (🏆/💀) suben.
4. Mirar la base de datos con `adb shell run-as com.juego.combates sqlite3 databases/monsterclash.db "select usuario, password_hash from usuarios;"` (en un móvil sin root solo se puede con el APK de debug): se ve que la contraseña no está en claro.
