# Pruebas de consola

Las clases de `modelo/`, `batalla/` y `datos/Hash.java` no usan nada de `android.*`, así que se pueden compilar y ejecutar con el JDK del PC (17 o superior) sin emulador ni Android Studio. Aquí viven los dos programas que las prueban. Todos los comandos se lanzan desde la raíz del proyecto en **Git Bash**. En PowerShell hay que cambiar dos cosas: las opciones `-D` van entre comillas simples (`java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp pruebas/salida ArnesBatalla > pruebas/salida/actual.txt`), porque PowerShell parte `-Dfile.encoding` por el punto y Java no encuentra la clase; y la comparación se hace con `git diff --no-index --ignore-cr-at-eol pruebas/referencia_motor.txt pruebas/salida/actual.txt` (o `fc.exe`), porque allí `diff` es un alias de `Compare-Object` y no imprime lo mismo. Las barras `/` de las rutas no dan problema en ningún sitio.

La carpeta `pruebas/salida/` recoge las clases compiladas y los resultados; está en `.gitignore`.

## 1. Arnés del motor (`ArnesBatalla.java`)

Imprime la tabla de tipos, las fichas de las seis especies y juega **300 combates completos con semillas fijas**, volcando cada `Evento` (texto, efecto y vida de las dos criaturas). Como el azar sale de `Random` con semilla, la salida es siempre la misma; la referencia está guardada en `referencia_motor.txt`. Si tras tocar `modelo/` o `batalla/` la salida cambia en una sola línea, el motor ha cambiado de comportamiento (un texto, una fórmula o el orden de las llamadas a `Random`).

Compilar (el arnés junto a las clases del motor):

```
javac -encoding UTF-8 -d pruebas/salida pruebas/ArnesBatalla.java app/src/main/java/com/juego/combates/modelo/*.java app/src/main/java/com/juego/combates/batalla/*.java
```

Ejecutar y guardar la salida:

```
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp pruebas/salida ArnesBatalla > pruebas/salida/actual.txt
```

Comparar con la referencia (si no imprime nada, el motor no ha cambiado):

```
diff --strip-trailing-cr pruebas/referencia_motor.txt pruebas/salida/actual.txt
```

### Sobre la codificación

La referencia está en UTF-8 con saltos de línea CRLF (se generó en Windows). Dos detalles para que la comparación no dé falsos positivos:

- Desde Java 18 `-Dfile.encoding=UTF-8` ya no manda sobre la consola: la salida de `System.out` usa `stdout.encoding`, que en Windows es `Cp1252`. Por eso el comando lleva también `-Dstdout.encoding=UTF-8`; sin él, las tildes y los `¡` salen en Cp1252 y el `diff` marca todas las líneas con acentos.
- En Linux o macOS `println` escribe LF en vez de CRLF. El `--strip-trailing-cr` del `diff` hace que eso no cuente como diferencia.

Si hace falta regenerar la referencia (solo cuando un cambio del motor sea intencionado), basta con ejecutar el mismo comando redirigiendo a `pruebas/referencia_motor.txt`.

## 2. Autocomprobación de PBKDF2 (`com/juego/combates/datos/PruebaHash.java`)

`Hash.java` implementa PBKDF2-HMAC-SHA256 a mano sobre `javax.crypto.Mac`. `PruebaHash` lo compara con vectores de prueba conocidos y comprueba que `verificar()` acepta la contraseña correcta y rechaza la incorrecta. Va en el mismo paquete que `Hash` (`com.juego.combates.datos`) para poder llamarla sin depender de Android.

Compilar y ejecutar:

```
javac -encoding UTF-8 -d pruebas/salida app/src/main/java/com/juego/combates/datos/Hash.java pruebas/com/juego/combates/datos/PruebaHash.java && java -cp pruebas/salida com.juego.combates.datos.PruebaHash
```

La última línea dice `TODO OK` si todo coincide o `FALLO` si alguna comprobación no pasa (no uso `System.exit`, así que el código de salida es siempre 0). Al final imprime también un "hash ficticio" nuevo: es el que `RepositorioUsuarios.HASH_FICTICIO` usa para que un usuario que no existe tarde lo mismo que uno real.
