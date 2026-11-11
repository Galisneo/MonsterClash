# Monster Clash — Análisis completo y plan de pulido

> **Nota:** este documento describe el estado del proyecto el 6 de septiembre de 2026, **antes** del pulido. El plan de la sección 6 se ejecutó el 7 de septiembre: la basura de la tabla de la sección 4 ya no existe, las clases grandes se partieron (`Escenario`, `Particulas`, `TextosFlotantes`, `PanelCriatura`, `LectorDeEventos`, `ChatCombate`, `EstiloBotonMovimiento`, `Animaciones`, `Transiciones`, `ControlBloqueo`, `TokenRecuperacion`) y las pruebas viven en `pruebas/`. Lo conservo como memoria del proceso; el estado actual está en el README.

## 1. Resumen ejecutivo

**Nota: 7,8 / 10.** Es un trabajo de 2º DAM claramente por encima de la media en fondo (motor sin Android con arnés de regresión, login offline con PBKDF2 y bloqueo por intentos que resiste el cambio de hora, vistas con pools preasignados y cero `new` en `onDraw`) y correcto en estilo declarado (cabeceras, Javadoc, comentarios en primera persona en español, clases anónimas, if/else, bucles con índice, `Log.d` con TAG). Lo que le baja la nota no es diseño ni corrección: es **arrastre histórico** y **tamaño**. Quedan esqueletos de tres versiones anteriores (sobrecargas "para no romper firmas", `tipoPorColor`, `setColor/getColor`, `getFase/setFase`, `obtener(long)`, un `main` de prueba dentro del APK con `System.exit`, un comentario que cita un `tintar()` que ya no existe, un Javadoc que promete un `main` del motor que no está en el repo), tres clases que nadie puede seguir línea a línea (`VistaBatalla` 1718, `CombateActivity` 1129, `RepositorioUsuarios` 893), y una raíz con basura de proceso (APK con el nombre viejo, `gradle-daemon-jvm.properties` que exige JDK 25, un prompt de 18 KB para una IA, 14 MB de capturas y vídeo con nombres `after_*`). Además el proyecto **declara** "medidas en dimens.xml y colores en colors.xml" pero los layouts repiten 44 `textSize` y 21 colores literales, y hay 78 `left/right` frente a 4 `start/end`.

Con el plan de abajo (nueve fases, cada una verificable por separado) el proyecto queda en un 9+ sin cambiar el comportamiento del motor, sin romper el esquema de paquetes y sin dejar de parecer escrito por un alumno aplicado.

No hay restos del login REST descartado en el código Java (las cinco lecturas lo confirman con grep: ni "api", ni "token" salvo el de recuperación local, ni "red" salvo "red de seguridad"). Los restos de esa etapa están en la raíz y en `docs/`, no en `app/src`.

## 2. Mapa del proyecto

```
LoginActivity (LAUNCHER) ──► MenuActivity ──► SeleccionActivity ──► CombateActivity
      │                          │                                        │
      ▼                          ▼                                        ▼
RecuperarActivity          RepositorioPartidas                    Batalla (motor)
      │                          │                                  │       ▲
      ▼                          ▼                                  ▼       │
RepositorioUsuarios ──► BaseDatos (SQLite v2)               List<Evento>  Fabrica/Criatura
      │                                                             │      /Movimiento/Tipo
   Hash · Validador · Sesion · EstadoIntentos                       ▼
                                                     DetalleGolpe ──► VistaBatalla ──► DibujoCriatura · Paleta
```

- **modelo/ + batalla/** (1009 líneas, cero `android.*`): `Tipo` (tabla 6×6 de efectividades), `Movimiento`, `Criatura`, `Fabrica` (6 especies), `Batalla` (fórmula Pokémon con STAB 1,5, crítico 1/12, variación 0,85–1,0; IA que se cura <35 % con 55 %, 20 % azar, si no mayor daño esperado) y `Evento` (mensaje + efecto + bando + HP fotografiado). Un arnés externo compila estos dos paquetes con `javac` y hace `cmp` de 300 combates con semilla fija. **Restricción doble sobre los literales**: además del arnés, `CombateActivity` y `VistaBatalla` interpretan los textos de los eventos con `contains("recupera")`, `startsWith("¡Es muy eficaz")`, `contains("Forcejeo")`, etc. Ni un literal ni una llamada a `rng` pueden moverse.
- **datos/** (1878 líneas): cuatro capas limpias (acceso SQLite, repositorios, criptografía/reglas sin Android, POJOs+sesión). `Hash.java` compila con javac de PC y su autocomprobación pasa; `HASH_FICTICIO` verifica de verdad. `RepositorioUsuarios` concentra cuatro responsabilidades (repositorio, bloqueo con tres relojes, token de recuperación, tipos de resultado).
- **vistas/** (3112 líneas): `Paleta` (colores derivados de `Tipo`), `DibujoCriatura` (una criatura por especie con Canvas), `VistaCriatura`, `DetalleGolpe`, `VistaConfeti` y `VistaBatalla`, que mezcla cinco cosas: ciclo de vida y bucle a 33 ms, fondo (cielo por hora, astro, estrellas, nubes, colinas), criaturas (entrada, KO, embestida, esquiva, hit-stop), efectos (pool de 96 partículas y 12 textos) y paneles de vida.
- **pantallas/** (2781 líneas): cinco `Activity` del framework (sin AndroidX), patrón `Thread + Handler` para la BD, clases anónimas. `CombateActivity` hace flujo de turnos, interpretación de textos → `DetalleGolpe`, estilizado de botones, chat de burbujas, diálogos y persistencia.
- **res/**: 9 layouts, 20 drawables, 4 anims, 1 animator, `strings.xml` (98 + 1 plurals), `colors.xml` (43), `dimens.xml` (16), `themes.xml`, `arrays.xml` (preguntas de seguridad, índice guardado en SQLite), `xml/reglas_extraccion`. Todo se usa salvo `dlg_equipo_item` y `margen`.
- **Configuración**: AGP 9.4.0, Gradle 9.6 wrapper, compileSdk 36, minSdk 24, targetSdk 34 justificado, Java 17, `dependencies {}` vacío a propósito. Git solo tiene el estado de agosto: todo `datos/`, el rediseño y `docs/` están sin confirmar.

## 3. Lo que está bien (y no se toca)

- La separación `Tipo → Movimiento → Criatura → Fabrica → Batalla → Evento` y la idea de la "foto" del HP en `Evento` para que el golpe caiga sobre la criatura correcta.
- La capa de datos: consultas parametrizadas, transacciones, camino espejo para nombres inexistentes contra un hash ficticio real, token de un solo uso en memoria, `reglas_extraccion.xml` que excluye la BD del backup. `LOGIN.md` coincide con el código en todo lo contrastado.
- Las vistas: shaders cacheados y recolocados con `setLocalMatrix`, pools en arrays, bucle ambiental que se para al ocultar la vista, comentarios ejemplares sobre `postInvalidateDelayed` y el alfa del shader.
- Las Activities: `Sesion`, `CLEAR_TOP`, cerrojo `ocupado`, hit-stop, todo explicado en primera persona y sin métodos muertos.
- Recursos: cada XML con cabecera en español, manifest razonado, README completo con árbol fiel.

## 4. Basura detectada

| Ruta | Qué | Acción |
|---|---|---|
| `CombatesMonstruo-debug.apk` (raíz) | APK de agosto con el nombre antiguo, anterior al login | Borrar |
| `build/` (raíz) | Un `problems-report.html` de agosto | Borrar |
| `gradle/gradle-daemon-jvm.properties` | Generado por el IDE; exige JDK 25 y URLs foojay, contradice "JDK 17+" del README; con `--offline` obligaría a tener ese JDK | Borrar (el PC ya ejecuta Gradle con el Java 25 del PATH) |
| `docs/PROMPT_CODEX.md` | Prompt de 18 KB para una IA con rutas de scratchpad de este PC; cita `tintar()` y `old.txt` que no existen | Sacar del repo (copiar fuera si se quiere conservar) y borrar |
| `docs/capturas/after_batalla.mp4` | 11,3 MB sin trackear; multiplicaría ×25 el repo | Borrar (si se quiere enseñar, asset de una release) |
| `docs/capturas/after_combate.png`, `after_combate_color.png`, `after_impacto_2.png` | Variantes del mismo momento del rediseño | Borrar; conservar `after_combate_final` → `combate.png`, `after_impacto_1` → `combate_impacto.png`, `after_combate_1080x1920` → `combate_1080x1920.png`, `after_login` → `login.png`, `after_seleccion` → `seleccion.png`, `after_seleccion_elegida` → `seleccion_equipo.png` |
| `.idea/` | Ignorado por git, pero lleva el serial del móvil y `jbr-25` | No borrar; excluir si se entrega como ZIP |
| `gradle.properties:2` `android.useAndroidX=true` | Plantilla del IDE; el proyecto presume de no usar AndroidX | Quitar y verificar compilación |
| `strings.xml:58` `dlg_equipo_item` | Sin uso (lint + grep) | Borrar |
| `dimens.xml:7` `margen` | Sin uso, pero el 16dp aparece literal en `activity_seleccion` y `activity_combate` | **Usar**, no borrar (ver §5, contradicción resuelta) |
| `drawable/slot_lleno.xml` | Idéntico a `circulo_suave.xml` | Borrar; `SeleccionActivity:292` usa `circulo_suave` |
| `colors.xml` `menu_grad_abajo`, `pantalla_grad_abajo`, `seleccion_borde`, `insignia`, `barra_fondo` | Mismo valor que `fondo_oscuro`, `boton_verde`, `boton_apagado` | Definir como alias `@color/...` |
| `activity_recuperar.xml` `tools:text="No hay ninguna cuenta con ese usuario"` | Contradice el diseño (nunca revela si existe) | Sustituir por un texto neutro |
| `activity_menu.xml` ids `telon_menu`, `fila_usuario`, `fila_criaturas`; `activity_login.xml` `titulo_login` | Nadie los lee desde Java | Quitar |
| `DibujoCriatura.java` `oscurecer`, `aclarar`, dos sobrecargas `dibujar(..., color, golpeada)` | Sin llamadores; Javadoc "para no romper firmas" | Borrar |
| `DibujoCriatura.java` `dibujarHojaSuelta` → `dibujarHojaInterna` | Envoltorio puro | Fusionar: `dibujarHoja` pública |
| `Paleta.java` `tipoPorColor` | Solo lo usaban las sobrecargas muertas | Borrar |
| `VistaCriatura.java` `setColor`, `getColor`, atributo `color = 0xFF9AA0A6` | Sin uso; el color ni influye en el dibujo; duplica `Tipo.NORMAL` | Borrar |
| `VistaBatalla.java` `mostrarEvento(Evento)` | "Sobrecarga antigua" sin llamadores | Borrar |
| `VistaBatalla.java` `if (indice < 0) return;` en `crearTextoDano`/`crearTextoCuracion` | `textoLibre()` nunca devuelve −1 | Quitar el `if`, comentar "reutilizo el 0" |
| `VistaBatalla.java` `matrizGris` atributo | Solo se usa en `inicializar()` | Variable local |
| `VistaConfeti.java` `getFase`/`setFase` | "Para pruebas visuales" que no existen | Borrar |
| `RepositorioUsuarios.java` `obtener(long)` + `COLUMNAS_USUARIO.split(", ")` | Sin llamadores; el `split` es un atajo | Borrar `obtener`; convertir `COLUMNAS_USUARIO` en `String[]` y usarla en `autenticarEnTransaccion` |
| `Hash.java` bloque `main`/`comprobarVector`/`informar` | Código de prueba dentro del APK con `System.exit(1)` | Mover a `pruebas/com/juego/combates/datos/PruebaHash.java` |
| `Batalla.java:17` "probarlo con un main" | No hay tal main en el repo | Reescribir (y hacerlo verdad: el arnés entra en `pruebas/`) |
| `Batalla.java:42` "9999 PP para que no se agote nunca" | Los PP del Forcejeo nunca se gastan (`mov == forcejeo` no llama a `gastarPP`) | Reescribir |
| `CombateActivity.java:178` "Sustituye al antiguo tintar()" | `tintar()` ya no existe | Reescribir |
| `Validador.java` Javadoc "todas las reglas en un solo sitio" | El email se valida en `LoginActivity` | Hacerlo verdad con `esEmailValido` |
| `README.md` §"Limitaciones conocidas", línea 97 (BaseDatos "tablas usuarios y partidas"), líneas 51/96 (`main` de Hash) | Informe de rediseño y datos obsoletos | Reescribir |

## 5. Problemas de organización y estilo, y contradicciones resueltas

**Clases enormes.** `VistaBatalla` (1718), `CombateActivity` (1129) y `RepositorioUsuarios` (893) mezclan responsabilidades separables sin tocar ninguna firma pública ni el comportamiento. Es el único sitio donde propongo crear clases nuevas, todas en español y dentro del paquete que ya les corresponde.

**Lo declarado no se cumple.** El proyecto dice "colores en colors.xml, medidas en dimens.xml": `dimens.xml` define `texto_normal=14sp` y los layouts escriben `14sp` ocho veces; hay 21 colores literales en drawables/layouts, ocho hex en Java que ya existen en `colors.xml` o `Tipo`, diez emojis en `iconoMensaje` fuera de `strings.xml`, y `left/right` en vez de `start/end` con `supportsRtl=true`.

**Números mágicos.** En el motor (1.5, 12, 0.85, 0.35f, 55, 20), en `VistaBatalla` (0.30f cinco veces, posición de las criaturas copiada en tres sitios) y en datos (`59999L`, `16`). Un profesor entiende `HORIZONTE` pero no por qué `0.30f` aparece en la línea 451 y en la 932.

**Javadoc a medias.** La mitad de los privados de `VistaBatalla`, `DibujoCriatura`, `Batalla` y `RepositorioUsuarios` no llevan `@param/@return`; en `RecuperarActivity` falta la mitad de los Javadoc.

**Acoplamiento por texto duplicado.** Los literales de `Batalla` viven en `CombateActivity` (dos veces: `leerDetalleAtaque` e `iconoMensaje`) y en `VistaBatalla.mostrarEvento`. No se puede quitar (el motor no cambia), pero sí concentrar en **un** archivo.

**Contradicciones entre lecturas y cómo las resuelvo:**

1. *`R.dimen.margen`*: pantallas dice borrar, recursos dice usar. **Usar**: el valor 16dp está literal en dos layouts; borrarlo iría contra la regla de medidas en `dimens.xml`.
2. *`PROMPT_CODEX.md`*: motor propone corregir la ruta del arnés o copiarlo a `docs/arnes/`; recursos dice sacarlo del repo. **Ambas**: el documento se saca (es un prompt para una IA, no documentación de alumno) y el arnés se copia a `pruebas/ArnesBatalla.java` con un `README` de dos líneas; la salida de referencia no se versiona (2,5 MB), se genera al empezar cada fase y se compara al terminar. Así el Javadoc de `Batalla` puede decir con verdad "se prueba con el javac del PC desde `pruebas/`".
3. *Sobrecarga de 9 parámetros de `DibujoCriatura.dibujar`*: se conserva y `VistaCriatura.onDraw` la usa (hoy pasa `false, false, 0f` a mano); las otras dos se borran.
4. *`dibujarEstrellaParticula` vs `dibujarChispa`*: difieren 0,22 frente a 0,20 en el radio interior. Se fusionan en `DibujoCriatura.dibujarEstrella` con 0,20; la diferencia de 0,02·r en una partícula de pocos píxeles no se aprecia y evita mantener dos estrellas. Se comprueba con captura.
5. *Color ámbar `0xFFF0A500`* (barra de vida media): pantallas dice `colors.xml`, vistas dice `Paleta`. **Ambas**: `colors.xml` `barra_vida_media` para las Activities y `Paleta.luz(ELECTRICO)` no, porque coincide con `Tipo.ELECTRICO` por casualidad; en `VistaBatalla` se lee de `colors.xml` en `inicializar()`.
6. *`LectorDeEventos`*: pantallas lo pone en `vistas/` junto a `DetalleGolpe`; vistas propone constantes en `VistaBatalla`. **Una sola clase `vistas/LectorDeEventos`** que usan `CombateActivity` y `VistaBatalla`; ahí viven los literales, con un comentario que remite a las líneas de `Batalla`. Añado `public static final String NOMBRE_FORCEJEO = "Forcejeo"` en `Batalla` (no cambia la salida del arnés).
7. *`main` de Hash*: datos ofrece moverlo o dejarlo sin `System.exit`. **Mover** a `pruebas/`, mismo paquete, para que `derivar/aHex/deHex` sigan package-private; actualizar Javadoc de Hash, LOGIN.md, README y el comentario de `HASH_FICTICIO`.
8. *Cosas que NO hago aunque una lectura las proponga*: la clase base `PantallaFormulario` (toca un login que funciona, beneficio pequeño; me quedo con `Animaciones` y `Transiciones`), `LadoCombate` (rehace `mostrarEvento` tras la partición; riesgo sin ganancia clara), mover `ResultadoLogin`/`ResultadoRegistro`/… a archivos propios (los anidados son legibles y cambiarlos obliga a tocar imports de pantallas sin ganancia), y convertir `seleccion_tope` en plurals (se llama siempre con la constante 3: `tools:ignore` con comentario).

## 6. Plan de pulido por fases

Cada fase se ejecuta y se verifica sola. Verificaciones: **C** = `./gradlew assembleDebug --offline`; **A** = arnés (`javac` de `pruebas/ArnesBatalla.java` + `modelo/*` + `batalla/*`, `java` sin `-Dfile.encoding`, `cmp` contra la referencia generada en la Fase 0); **L** = `./gradlew lintDebug --offline` (objetivo 0 avisos al acabar la Fase 4); **M** = instalar en el móvil y recorrer login → menú → selección → combate completo → fin.

- **Fase 0 — Red de seguridad** (riesgo bajo): crear `pruebas/` con el arnés y generar la referencia; compilar y anotar el tamaño del APK; sacar capturas de referencia del menú (telón sin batalla), un impacto y el panel. Verifica: C, A.
- **Fase 1 — Raíz, docs y configuración** (bajo, una tarea media): borrar APK, `build/`, `gradle-daemon-jvm.properties`, `PROMPT_CODEX.md`, vídeo y capturas redundantes; renombrar capturas; `.gitignore`/`.gitattributes`; quitar `useAndroidX`; cabeceras en `settings.gradle`, `gradle.properties`, `app/build.gradle`; README. Verifica: C (en especial tras quitar `useAndroidX` y el daemon-jvm).
- **Fase 2 — Basura en código y recursos** (bajo): todo el bloque de la tabla §4 que es borrar/quitar. Verifica: C, L, A, M rápido.
- **Fase 3 — Motor** (bajo, una tarea media): constantes con nombre, `factorStab`, `final`, renombrar `esCuracion`→`curacion`, Javadoc, `NOMBRE_FORCEJEO`, comentarios; al final, la fórmula con variables intermedias en un commit aparte que se revierte si `cmp` difiere. Verifica: A tras cada tarea.
- **Fase 4 — Recursos XML** (bajo): `start/end`, `dimens`, colores a `colors.xml`, estilos `CampoTexto`/`BotonPrimario`/`BotonContorno`, supresiones lint justificadas, orden de atributos y reindentado, prefijos `combate_*`. Verifica: L a 0, M mirando las cinco pantallas.
- **Fase 5 — Constantes, colores y pequeños pulidos en Java** (bajo): sin partir clases. Posiciones y `HORIZONTE` en `VistaBatalla`, `Paleta.TEXTO_OSCURO`, emojis a strings, `Validador.esEmailValido`, columnas por nombre, `onClick` homogéneo, `Sesion.cerrar`, `SecureRandom` único, etc. Verifica: C, A, M.
- **Fase 6 — Partir datos** (medio): `ControlBloqueo`, `TokenRecuperacion`, `PruebaHash`. Verifica: C, `PruebaHash` TODO OK, M probando registro, login, 5 fallos → bloqueo, recuperación completa.
- **Fase 7 — Partir pantallas** (medio): `LectorDeEventos`, `EstiloBotonMovimiento`, `ChatCombate`, `Animaciones`, `Transiciones`. Verifica: C, A, M con un combate hasta el final y el diálogo de fin.
- **Fase 8 — Partir VistaBatalla** (medio-alto): `Escenario`, `Particulas`, `TextosFlotantes`, `PanelCriatura`; cuidado con el `strokeCap ROUND` que `DibujoCriatura.dibujar` deja en el Paint compartido. Verifica: C, M, comparar con las capturas de la Fase 0 (telón del menú, impacto, panel).
- **Fase 9 — Cierre** (bajo): README (árbol con `pruebas/` y `docs/`), sección Capturas, commit de todo lo que está sin trackear y release nueva.

## 7. Lo que NO se toca y por qué

- **Comportamiento del motor**: ni un literal de texto, ni el orden de las llamadas a `rng`, ni las fórmulas, ni los colores de `Tipo` (el arnés imprime `getColor()`). Solo comentarios, constantes con el mismo valor y tipo, `final`, nombres privados, un método privado `factorStab` que devuelve los mismos `double`, y una constante pública nueva. Todo pasa por `cmp`.
- **Esquema de paquetes y pantallas**: las clases nuevas van dentro de `datos/`, `vistas/` y `pantallas/`; no aparece ningún paquete nuevo salvo la carpeta `pruebas/` fuera de `app/`, que no se compila en el APK. Las cinco Activities, sus layouts e ids que Java lee se conservan.
- **Estilo de estudiante**: nada de lambdas, ternarios, streams ni `var`; las clases auxiliares llevan cabecera, Javadoc con `@param/@return`, atributos privados con getters/setters y comentarios en primera persona. Los "trucos" que se quitan (`split(", ")`, máscara de bits, índices de columna) se sustituyen por código más largo y más claro, no por otro truco.
- **Esquema de la BD**: `monsterclash.db` v2 y sus cuatro tablas no cambian; nadie pierde su cuenta al instalar la versión pulida.
- **Cero dependencias**: `PruebaHash` y `ArnesBatalla` no usan JUnit; se compilan con `javac`.
- **`.idea/` y `local.properties`**: son locales e ignorados; no forman parte de la entrega.