/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  RepositorioUsuarios.java
 */
package com.juego.combates.datos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Todo lo que la app hace con la tabla usuarios: registrar, entrar, bloquear
 * tras varios fallos, recuperar la contraseña con la pregunta de seguridad.
 *
 * Reglas que sigo en toda la clase:
 * - Cada consulta lleva sus parámetros con "?" (selectionArgs). Nunca pego el
 *   texto del usuario dentro del SQL, que es como se cuelan las inyecciones.
 * - Usuario y email se guardan y se buscan en minúsculas.
 * - Los métodos que hashean (registrar, autenticar, verificarRespuesta,
 *   cambiarPassword) tardan unos cientos de ms: hay que llamarlos desde un
 *   Thread, no desde el hilo principal.
 * - No se puede saber desde fuera qué cuentas existen: un nombre inventado
 *   tarda lo mismo, recibe los mismos mensajes y se bloquea igual que una
 *   cuenta real (sus contadores van a la tabla intentos_nombre).
 * - Leer los contadores de fallos y volver a escribirlos va siempre dentro de
 *   una transacción, para que dos intentos a la vez no se pisen el contador.
 * - cambiarPassword solo funciona con el token de un solo uso que da
 *   verificarRespuesta al acertar la pregunta: no hay forma de saltársela.
 *
 * Las reglas del bloqueo (cuántos fallos, cuánto dura, los tres relojes) están
 * en ControlBloqueo y el token de recuperación en TokenRecuperacion; aquí solo
 * queda el SQL y el hilo de cada operación.
 */
public class RepositorioUsuarios {

    /**
     * Hash de una contraseña que no es de nadie. Cuando alguien intenta entrar
     * con un usuario que NO existe, verifico la contraseña contra este hash
     * igualmente: así la respuesta tarda lo mismo exista o no la cuenta y, por
     * el tiempo, nadie puede averiguar qué usuarios hay. Lo generé con
     * pruebas/com/juego/combates/datos/PruebaHash.java (imprime uno al final)
     * y lo pego aquí para no recalcularlo en cada arranque.
     */
    private static final String HASH_FICTICIO =
            "pbkdf2_sha256$120000$b1db7cf988d37fa790ce6efe7857158b$00b0db38e0c0f473b743fcd2fa8de0041a7002c27cd7a6053540b4b701eda4ed";

    /** Clave, en la tabla ajustes, de la semilla con la que invento preguntas. */
    private static final String CLAVE_SEMILLA = "semilla_preguntas";
    /** Bytes de la semilla de las preguntas ficticias. */
    private static final int TAM_SEMILLA = 16;

    /** Las cinco columnas que necesita un Usuario. */
    private static final String[] COLUMNAS_USUARIO = {
            "id", "usuario", "email", "pregunta", "creado_en"};

    /** Las seis columnas de EstadoIntentos. */
    private static final String[] COLUMNAS_ESTADO = {
            "intentos_fallidos", "intentos_recuperacion", "bloqueado_en",
            "bloqueado_hasta", "bloqueo_elapsed_hasta", "bloqueo_arranque"};

    // uno solo para toda la app: crear un SecureRandom cada vez es caro y no aporta nada
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final BaseDatos baseDatos;
    private final ControlBloqueo bloqueo;
    private final TokenRecuperacion token;

    /**
     * @param contexto cualquier contexto (uso el de aplicación por dentro)
     */
    public RepositorioUsuarios(Context contexto) {
        this.baseDatos = BaseDatos.get(contexto);
        this.bloqueo = new ControlBloqueo(contexto);
        this.token = new TokenRecuperacion();
    }

    // ---- Tipos de resultado (nada de códigos numéricos mágicos) ----

    /**
     * Qué ha pasado al crear una cuenta. Si es OK lleva el Usuario recién
     * creado, para que la pantalla entre con él sin volver a hashear.
     */
    public static class ResultadoRegistro {

        public enum Estado {
            OK, USUARIO_EXISTE, EMAIL_EXISTE
        }

        private final Estado estado;
        private final Usuario usuario;

        private ResultadoRegistro(Estado estado, Usuario usuario) {
            this.estado = estado;
            this.usuario = usuario;
        }

        static ResultadoRegistro ok(Usuario usuario) {
            return new ResultadoRegistro(Estado.OK, usuario);
        }

        static ResultadoRegistro usuarioExiste() {
            return new ResultadoRegistro(Estado.USUARIO_EXISTE, null);
        }

        static ResultadoRegistro emailExiste() {
            return new ResultadoRegistro(Estado.EMAIL_EXISTE, null);
        }

        public Estado getEstado() {
            return estado;
        }

        public Usuario getUsuario() {
            return usuario;
        }
    }

    /**
     * Qué ha pasado al intentar entrar. Si es OK lleva el Usuario; si es
     * BLOQUEADO lleva los minutos que faltan.
     */
    public static class ResultadoLogin {

        public enum Estado {
            OK, CREDENCIALES_INCORRECTAS, BLOQUEADO
        }

        private final Estado estado;
        private final Usuario usuario;
        private final int minutosRestantes;

        private ResultadoLogin(Estado estado, Usuario usuario, int minutosRestantes) {
            this.estado = estado;
            this.usuario = usuario;
            this.minutosRestantes = minutosRestantes;
        }

        static ResultadoLogin ok(Usuario usuario) {
            return new ResultadoLogin(Estado.OK, usuario, 0);
        }

        static ResultadoLogin incorrecto() {
            return new ResultadoLogin(Estado.CREDENCIALES_INCORRECTAS, null, 0);
        }

        static ResultadoLogin bloqueado(int minutos) {
            return new ResultadoLogin(Estado.BLOQUEADO, null, minutos);
        }

        public Estado getEstado() {
            return estado;
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public int getMinutosRestantes() {
            return minutosRestantes;
        }
    }

    /**
     * Qué ha pasado al responder la pregunta de seguridad. Si es OK lleva el
     * token que hay que darle a cambiarPassword; si es BLOQUEADO, los minutos.
     */
    public static class ResultadoRecuperacion {

        public enum Estado {
            OK, INCORRECTA, BLOQUEADO
        }

        private final Estado estado;
        private final int minutosRestantes;
        private final String token;

        private ResultadoRecuperacion(Estado estado, int minutosRestantes, String token) {
            this.estado = estado;
            this.minutosRestantes = minutosRestantes;
            this.token = token;
        }

        static ResultadoRecuperacion ok(String token) {
            return new ResultadoRecuperacion(Estado.OK, 0, token);
        }

        static ResultadoRecuperacion incorrecta() {
            return new ResultadoRecuperacion(Estado.INCORRECTA, 0, null);
        }

        static ResultadoRecuperacion bloqueado(int minutos) {
            return new ResultadoRecuperacion(Estado.BLOQUEADO, minutos, null);
        }

        public Estado getEstado() {
            return estado;
        }

        public int getMinutosRestantes() {
            return minutosRestantes;
        }

        /**
         * @return el token de un solo uso para cambiarPassword, o null si no es OK
         */
        public String getToken() {
            return token;
        }
    }

    /**
     * Qué ha pasado al cambiar la contraseña con el token del paso 2.
     * TOKEN_INVALIDO: no había token, no era el de ese usuario, ya se había
     * gastado o han pasado más de TokenRecuperacion.TOKEN_MS desde que acertó
     * la pregunta.
     * NO_GUARDADO: la fila del usuario no se actualizó (no debería pasar con
     * un token válido, salvo que la cuenta haya desaparecido entre medias).
     */
    public enum ResultadoCambio {
        OK, TOKEN_INVALIDO, NO_GUARDADO
    }

    // ---- Registro ----

    /**
     * Crea la cuenta. La contraseña y la respuesta se guardan hasheadas.
     *
     * @param usuario        nombre (se pasa a minúsculas)
     * @param email          email o vacío/null si no quiere darlo
     * @param password       contraseña en claro (ya validada por la pantalla)
     * @param indicePregunta posición de la pregunta en el string-array
     * @param respuesta      respuesta en claro (se normaliza antes de hashear)
     * @return OK con el Usuario creado, USUARIO_EXISTE o EMAIL_EXISTE
     */
    public ResultadoRegistro registrar(String usuario, String email, String password,
                                       int indicePregunta, String respuesta) {
        String usuarioNormal = normalizar(usuario);
        String emailNormal = normalizarEmail(email);
        // los dos hashes los calculo ANTES de abrir la transacción: tardan unos
        // cientos de ms y no quiero tener la base de datos cogida mientras
        String passwordHash = Hash.hashear(password);
        String respuestaHash = Hash.hashear(Hash.normalizarRespuesta(respuesta));
        SQLiteDatabase db = baseDatos.getWritableDatabase();

        // comprobar que no existe e insertar van juntos en una transacción: así
        // nadie puede colarse entre la comprobación y el INSERT
        db.beginTransaction();
        try {
            ResultadoRegistro resultado = registrarEnTransaccion(db, usuarioNormal, emailNormal,
                    passwordHash, indicePregunta, respuestaHash);
            db.setTransactionSuccessful();
            return resultado;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * El cuerpo de registrar, ya dentro de la transacción.
     *
     * @param db             base de datos con la transacción abierta
     * @param usuarioNormal  nombre ya en minúsculas
     * @param emailNormal    email en minúsculas, o null si no lo dio
     * @param passwordHash   hash ya calculado de la contraseña
     * @param indicePregunta posición de la pregunta en el string-array
     * @param respuestaHash  hash ya calculado de la respuesta normalizada
     * @return OK con el Usuario creado, USUARIO_EXISTE o EMAIL_EXISTE
     */
    private ResultadoRegistro registrarEnTransaccion(SQLiteDatabase db, String usuarioNormal,
                                                     String emailNormal, String passwordHash,
                                                     int indicePregunta, String respuestaHash) {
        // compruebo antes de insertar para poder decir QUÉ está repetido
        if (existe(db, "usuario", usuarioNormal)) {
            return ResultadoRegistro.usuarioExiste();
        }
        if (emailNormal != null && existe(db, "email", emailNormal)) {
            return ResultadoRegistro.emailExiste();
        }

        long creadoEn = System.currentTimeMillis();
        ContentValues valores = new ContentValues();
        valores.put("usuario", usuarioNormal);
        valores.put("email", emailNormal); // null si no lo dio; así el UNIQUE no choca entre vacíos
        valores.put("password_hash", passwordHash);
        valores.put("pregunta", indicePregunta);
        valores.put("respuesta_hash", respuestaHash);
        valores.put("creado_en", creadoEn);

        long id;
        try {
            id = db.insertOrThrow("usuarios", null, valores);
        } catch (SQLiteConstraintException e) {
            // con la transacción no debería pasar, pero el UNIQUE es la última
            // red. Vuelvo a consultar para decir cuál de los dos está repetido.
            if (emailNormal != null && existe(db, "email", emailNormal)) {
                return ResultadoRegistro.emailExiste();
            }
            return ResultadoRegistro.usuarioExiste();
        }

        // si alguien había fallado con este nombre cuando aún no existía, esos
        // contadores ya no pintan nada: la cuenta nueva empieza limpia
        db.delete("intentos_nombre", "nombre = ?", new String[]{usuarioNormal});

        // devuelvo el Usuario con lo que tengo en mano, sin volver a consultar ni hashear
        return ResultadoRegistro.ok(new Usuario(id, usuarioNormal, emailNormal, indicePregunta, creadoEn));
    }

    // ---- Login ----

    /**
     * Comprueba usuario y contraseña. Tras ControlBloqueo.MAX_INTENTOS fallos
     * seguidos bloquea BLOQUEO_MS; un login correcto deja los dos contadores a
     * cero (también el de la pregunta de seguridad: si sabe la contraseña, es
     * él). El contador NO se pone a cero al bloquear: si alguien se saltara el
     * bloqueo solo ganaría un intento, no cinco.
     *
     * @param usuario  nombre (se pasa a minúsculas)
     * @param password contraseña en claro
     * @return OK con el Usuario, CREDENCIALES_INCORRECTAS o BLOQUEADO(minutos)
     */
    public ResultadoLogin autenticar(String usuario, String password) {
        String usuarioNormal = normalizar(usuario);
        SQLiteDatabase db = baseDatos.getWritableDatabase();

        // leer los contadores y volver a escribirlos va en una transacción: si
        // dos intentos entraran a la vez, sin ella uno podría pisar el fallo del
        // otro (los dos leen 3, los dos escriben 4). El hash queda dentro y
        // retiene la base de datos unos cientos de ms; aquí no molesta porque
        // solo hay una conexión (BaseDatos es singleton) y nada más escribe
        // mientras alguien está entrando.
        db.beginTransaction();
        try {
            ResultadoLogin resultado = autenticarEnTransaccion(db, usuarioNormal, password);
            db.setTransactionSuccessful();
            return resultado;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * El cuerpo de autenticar, ya dentro de la transacción.
     *
     * @param db            base de datos con la transacción abierta
     * @param usuarioNormal nombre ya en minúsculas
     * @param password      contraseña en claro
     * @return OK con el Usuario, CREDENCIALES_INCORRECTAS o BLOQUEADO(minutos)
     */
    private ResultadoLogin autenticarEnTransaccion(SQLiteDatabase db, String usuarioNormal, String password) {
        // pido las columnas del Usuario, el hash y las del estado, todo en una consulta
        String[] columnas = juntar(juntar(COLUMNAS_USUARIO, new String[]{"password_hash"}), COLUMNAS_ESTADO);
        Cursor c = db.query("usuarios", columnas,
                "usuario = ?", new String[]{usuarioNormal}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return autenticarNombreDesconocido(db, usuarioNormal, password);
            }

            long id = c.getLong(c.getColumnIndexOrThrow("id"));
            EstadoIntentos estado = leerEstado(c);
            int minutos = bloqueo.minutosDeBloqueo(estado);
            if (minutos > 0) {
                guardarEstadoUsuario(db, id, estado); // por si minutosDeBloqueo lo ha rearmado
                return ResultadoLogin.bloqueado(minutos);
            }

            String passwordHash = c.getString(c.getColumnIndexOrThrow("password_hash"));
            if (Hash.verificar(password, passwordHash)) {
                // entrar bien limpia todo: los dos contadores y el bloqueo
                estado.setIntentosLogin(0);
                estado.setIntentosRecuperacion(0);
                estado.quitarBloqueo();
                guardarEstadoUsuario(db, id, estado);
                return ResultadoLogin.ok(leerUsuario(c));
            }

            minutos = bloqueo.apuntarFallo(estado, true);
            guardarEstadoUsuario(db, id, estado);
            if (minutos > 0) {
                return ResultadoLogin.bloqueado(minutos);
            }
            return ResultadoLogin.incorrecto();
        } finally {
            c.close();
        }
    }

    /**
     * Mismo camino que autenticar pero para un nombre que no está en usuarios:
     * mismos pasos, mismo hash (el ficticio), mismos mensajes y mismo bloqueo,
     * con los contadores en intentos_nombre.
     *
     * @param db       base de datos con la transacción abierta
     * @param nombre   nombre ya en minúsculas que no existe
     * @param password contraseña en claro
     * @return CREDENCIALES_INCORRECTAS o BLOQUEADO(minutos); nunca OK
     */
    private ResultadoLogin autenticarNombreDesconocido(SQLiteDatabase db, String nombre, String password) {
        EstadoIntentos estado = leerEstadoNombre(db, nombre);
        int minutos = bloqueo.minutosDeBloqueo(estado);
        if (minutos > 0) {
            guardarEstadoNombre(db, nombre, estado);
            return ResultadoLogin.bloqueado(minutos);
        }

        // hasheo igual para tardar lo mismo que con una cuenta real
        Hash.verificar(password, HASH_FICTICIO);

        minutos = bloqueo.apuntarFallo(estado, true);
        guardarEstadoNombre(db, nombre, estado);
        if (minutos > 0) {
            return ResultadoLogin.bloqueado(minutos);
        }
        return ResultadoLogin.incorrecto();
    }

    // ---- Recuperación de contraseña ----

    /**
     * Devuelve la pregunta de seguridad de un usuario. Si el usuario NO existe
     * devuelvo igualmente una pregunta, inventada pero siempre la misma para
     * ese nombre: así la pantalla de recuperar no delata qué cuentas hay (el
     * paso 2 fallará después como si la respuesta fuera incorrecta).
     *
     * @param usuario         nombre (se pasa a minúsculas)
     * @param numeroPreguntas cuántas preguntas hay en el string-array
     * @return el índice de la pregunta (real o inventada)
     */
    public int obtenerPregunta(String usuario, int numeroPreguntas) {
        String usuarioNormal = normalizar(usuario);
        // pido la base de datos para escribir aunque esto parezca solo una
        // lectura: si el nombre no existe, preguntaFicticia puede tener que
        // generar y guardar la semilla en la tabla ajustes la primera vez
        SQLiteDatabase db = baseDatos.getWritableDatabase();
        Cursor c = db.query("usuarios", new String[]{"pregunta"},
                "usuario = ?", new String[]{usuarioNormal}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow("pregunta"));
            }
        } finally {
            c.close();
        }
        return preguntaFicticia(db, usuarioNormal, numeroPreguntas);
    }

    /**
     * Comprueba la respuesta de seguridad. Lleva su propio contador
     * (intentos_recuperacion) pero comparte el mismo bloqueo de 15 minutos que
     * el login, así que quien esté bloqueado por uno lo está por los dos.
     *
     * @param usuario   nombre (se pasa a minúsculas)
     * @param respuesta respuesta en claro (se normaliza aquí)
     * @return OK (con el token para cambiarPassword), INCORRECTA o BLOQUEADO(minutos)
     */
    public ResultadoRecuperacion verificarRespuesta(String usuario, String respuesta) {
        String usuarioNormal = normalizar(usuario);
        String respuestaNormal = Hash.normalizarRespuesta(respuesta);
        SQLiteDatabase db = baseDatos.getWritableDatabase();

        // misma transacción que en autenticar, por la misma razón
        db.beginTransaction();
        try {
            ResultadoRecuperacion resultado = verificarRespuestaEnTransaccion(db, usuarioNormal, respuestaNormal);
            db.setTransactionSuccessful();
            return resultado;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * El cuerpo de verificarRespuesta, ya dentro de la transacción.
     *
     * @param db              base de datos con la transacción abierta
     * @param usuarioNormal   nombre ya en minúsculas
     * @param respuestaNormal respuesta ya normalizada
     * @return OK (con el token), INCORRECTA o BLOQUEADO(minutos)
     */
    private ResultadoRecuperacion verificarRespuestaEnTransaccion(SQLiteDatabase db, String usuarioNormal,
                                                                  String respuestaNormal) {
        String[] columnas = juntar(new String[]{"id", "respuesta_hash"}, COLUMNAS_ESTADO);
        Cursor c = db.query("usuarios", columnas,
                "usuario = ?", new String[]{usuarioNormal}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return verificarRespuestaNombreDesconocido(db, usuarioNormal, respuestaNormal);
            }

            long id = c.getLong(c.getColumnIndexOrThrow("id"));
            EstadoIntentos estado = leerEstado(c);
            int minutos = bloqueo.minutosDeBloqueo(estado);
            if (minutos > 0) {
                guardarEstadoUsuario(db, id, estado);
                return ResultadoRecuperacion.bloqueado(minutos);
            }

            String respuestaHash = c.getString(c.getColumnIndexOrThrow("respuesta_hash"));
            if (Hash.verificar(respuestaNormal, respuestaHash)) {
                // acertar limpia el contador de la pregunta y los datos de un
                // bloqueo ya vencido (si estuviera vigente no habría llegado aquí)
                estado.setIntentosRecuperacion(0);
                estado.quitarBloqueo();
                guardarEstadoUsuario(db, id, estado);
                // le doy el token que le va a pedir cambiarPassword
                return ResultadoRecuperacion.ok(token.crear(usuarioNormal));
            }

            minutos = bloqueo.apuntarFallo(estado, false);
            guardarEstadoUsuario(db, id, estado);
            if (minutos > 0) {
                return ResultadoRecuperacion.bloqueado(minutos);
            }
            return ResultadoRecuperacion.incorrecta();
        } finally {
            c.close();
        }
    }

    /**
     * Igual que verificarRespuesta pero para un nombre que no existe: nunca
     * acierta, pero tarda, cuenta y bloquea igual que una cuenta real.
     *
     * @param db              base de datos con la transacción abierta
     * @param nombre          nombre ya en minúsculas que no existe
     * @param respuestaNormal respuesta ya normalizada
     * @return INCORRECTA o BLOQUEADO(minutos); nunca OK
     */
    private ResultadoRecuperacion verificarRespuestaNombreDesconocido(SQLiteDatabase db, String nombre,
                                                                      String respuestaNormal) {
        EstadoIntentos estado = leerEstadoNombre(db, nombre);
        int minutos = bloqueo.minutosDeBloqueo(estado);
        if (minutos > 0) {
            guardarEstadoNombre(db, nombre, estado);
            return ResultadoRecuperacion.bloqueado(minutos);
        }

        Hash.verificar(respuestaNormal, HASH_FICTICIO);

        minutos = bloqueo.apuntarFallo(estado, false);
        guardarEstadoNombre(db, nombre, estado);
        if (minutos > 0) {
            return ResultadoRecuperacion.bloqueado(minutos);
        }
        return ResultadoRecuperacion.incorrecta();
    }

    /**
     * Cambia la contraseña y deja la cuenta limpia: contadores a cero y sin
     * bloqueo. Solo lo hace si me traen el token que dio verificarRespuesta al
     * acertar la pregunta: así nadie puede llamar a este método saltándose el
     * paso 2. El token se gasta al comprobarlo, valga o no.
     *
     * @param usuario       nombre (se pasa a minúsculas)
     * @param tokenRecibido el que devolvió verificarRespuesta con OK
     * @param nuevaPassword contraseña nueva en claro (ya validada)
     * @return OK, TOKEN_INVALIDO o NO_GUARDADO
     */
    public ResultadoCambio cambiarPassword(String usuario, String tokenRecibido, String nuevaPassword) {
        String usuarioNormal = normalizar(usuario);
        if (!token.consumir(usuarioNormal, tokenRecibido)) {
            return ResultadoCambio.TOKEN_INVALIDO;
        }
        SQLiteDatabase db = baseDatos.getWritableDatabase();
        ContentValues valores = valoresDeEstado(new EstadoIntentos()); // todo a cero
        valores.put("password_hash", Hash.hashear(nuevaPassword));
        int filas = db.update("usuarios", valores, "usuario = ?", new String[]{usuarioNormal});
        if (filas > 0) {
            return ResultadoCambio.OK;
        }
        return ResultadoCambio.NO_GUARDADO;
    }

    // ---- Pregunta ficticia ----

    /**
     * Inventa una pregunta para un nombre que no existe. Tiene que salir
     * siempre la misma para el mismo nombre (si cambiara entre intentos se
     * notaría que es falsa) y no poder adivinarse desde fuera (si fuera una
     * fórmula fija del nombre, ver otra pregunta distinta delataría que la
     * cuenta es real). Por eso hago SHA-256 de una semilla aleatoria del móvil
     * más el nombre. No hace falta que sea lento: aquí no se comprueba nada.
     *
     * @param db              base de datos (para leer o crear la semilla)
     * @param nombre          nombre ya en minúsculas que no existe
     * @param numeroPreguntas cuántas preguntas hay en el string-array
     * @return un índice entre 0 y numeroPreguntas - 1
     */
    private int preguntaFicticia(SQLiteDatabase db, String nombre, int numeroPreguntas) {
        if (numeroPreguntas < 1) {
            return 0;
        }
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            resumen.update(obtenerSemilla(db));
            byte[] bytes = resumen.digest(nombre.getBytes(StandardCharsets.UTF_8));
            // cuatro bytes a entero positivo y módulo: suficiente para elegir entre 6
            int valor = ((bytes[0] & 0x7F) << 24) | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
            return valor % numeroPreguntas;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está en todos los Android; si faltara, mejor una pregunta fija que fallar
            return 0;
        }
    }

    /**
     * Lee la semilla de la tabla ajustes; la primera vez la genera con
     * SecureRandom y la guarda. Vive solo en la base de datos privada de la app.
     *
     * @param db base de datos abierta para escribir
     * @return los TAM_SEMILLA bytes de la semilla
     */
    private byte[] obtenerSemilla(SQLiteDatabase db) {
        Cursor c = db.query("ajustes", new String[]{"valor"},
                "clave = ?", new String[]{CLAVE_SEMILLA}, null, null, null);
        try {
            if (c.moveToFirst()) {
                byte[] guardada = Hash.deHex(c.getString(c.getColumnIndexOrThrow("valor")));
                if (guardada != null) {
                    return guardada;
                }
            }
        } finally {
            c.close();
        }
        byte[] semilla = new byte[TAM_SEMILLA];
        ALEATORIO.nextBytes(semilla);
        ContentValues valores = new ContentValues();
        valores.put("clave", CLAVE_SEMILLA);
        valores.put("valor", Hash.aHex(semilla));
        db.replace("ajustes", null, valores);
        return semilla;
    }

    // ---- Ayudantes privados ----

    /**
     * Lee un Usuario de un cursor que tiene COLUMNAS_USUARIO. Busco cada
     * columna por su nombre, así da igual en qué posición venga.
     *
     * @param c cursor ya colocado en la fila
     * @return el usuario de esa fila
     */
    private Usuario leerUsuario(Cursor c) {
        int columnaEmail = c.getColumnIndexOrThrow("email");
        String email = null;
        if (!c.isNull(columnaEmail)) {
            email = c.getString(columnaEmail);
        }
        return new Usuario(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("usuario")),
                email,
                c.getInt(c.getColumnIndexOrThrow("pregunta")),
                c.getLong(c.getColumnIndexOrThrow("creado_en")));
    }

    /**
     * Lee un EstadoIntentos de un cursor que tiene COLUMNAS_ESTADO, buscando
     * cada columna por su nombre.
     *
     * @param c cursor ya colocado en la fila
     * @return los contadores y el bloqueo de esa fila
     */
    private EstadoIntentos leerEstado(Cursor c) {
        return new EstadoIntentos(
                c.getInt(c.getColumnIndexOrThrow("intentos_fallidos")),
                c.getInt(c.getColumnIndexOrThrow("intentos_recuperacion")),
                c.getLong(c.getColumnIndexOrThrow("bloqueado_en")),
                c.getLong(c.getColumnIndexOrThrow("bloqueado_hasta")),
                c.getLong(c.getColumnIndexOrThrow("bloqueo_elapsed_hasta")),
                c.getLong(c.getColumnIndexOrThrow("bloqueo_arranque")));
    }

    /**
     * @param db     base de datos
     * @param nombre nombre ya en minúsculas que no existe
     * @return el estado guardado para ese nombre, o uno limpio si nunca ha
     *         fallado nadie con él
     */
    private EstadoIntentos leerEstadoNombre(SQLiteDatabase db, String nombre) {
        Cursor c = db.query("intentos_nombre", COLUMNAS_ESTADO,
                "nombre = ?", new String[]{nombre}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return leerEstado(c);
            }
            return new EstadoIntentos();
        } finally {
            c.close();
        }
    }

    /**
     * Escribe los contadores de una cuenta real.
     *
     * @param db     base de datos
     * @param id     clave primaria del usuario
     * @param estado contadores a guardar
     */
    private void guardarEstadoUsuario(SQLiteDatabase db, long id, EstadoIntentos estado) {
        db.update("usuarios", valoresDeEstado(estado), "id = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Inserta o sustituye la fila del nombre (INSERT OR REPLACE por la clave primaria).
     *
     * @param db     base de datos
     * @param nombre nombre ya en minúsculas que no existe
     * @param estado contadores a guardar
     */
    private void guardarEstadoNombre(SQLiteDatabase db, String nombre, EstadoIntentos estado) {
        ContentValues valores = valoresDeEstado(estado);
        valores.put("nombre", nombre);
        db.replace("intentos_nombre", null, valores);
    }

    /**
     * @param estado contadores a convertir
     * @return los seis campos de EstadoIntentos con sus nombres de columna
     */
    private ContentValues valoresDeEstado(EstadoIntentos estado) {
        ContentValues valores = new ContentValues();
        valores.put("intentos_fallidos", estado.getIntentosLogin());
        valores.put("intentos_recuperacion", estado.getIntentosRecuperacion());
        valores.put("bloqueado_en", estado.getBloqueadoEn());
        valores.put("bloqueado_hasta", estado.getBloqueadoHasta());
        valores.put("bloqueo_elapsed_hasta", estado.getBloqueoElapsedHasta());
        valores.put("bloqueo_arranque", estado.getBloqueoArranque());
        return valores;
    }

    /**
     * @param db      base de datos
     * @param columna "usuario" o "email" (nombres fijos míos, no del usuario)
     * @param valor   el valor a buscar (este sí va con "?")
     * @return true si ya hay una fila con ese valor en esa columna
     */
    private boolean existe(SQLiteDatabase db, String columna, String valor) {
        Cursor c = db.query("usuarios", new String[]{"id"},
                columna + " = ?", new String[]{valor}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /**
     * @param primeras primer grupo de columnas
     * @param segundas segundo grupo de columnas
     * @return un array con los dos pegados, para montar la lista de columnas
     */
    private String[] juntar(String[] primeras, String[] segundas) {
        String[] todas = new String[primeras.length + segundas.length];
        for (int i = 0; i < primeras.length; i++) {
            todas[i] = primeras[i];
        }
        for (int i = 0; i < segundas.length; i++) {
            todas[primeras.length + i] = segundas[i];
        }
        return todas;
    }

    /**
     * @param usuario nombre tal cual llega (puede ser null)
     * @return el nombre sin espacios de los lados y en minúsculas ("" si venía null)
     */
    private String normalizar(String usuario) {
        if (usuario == null) {
            return "";
        }
        return usuario.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param email email tal cual llega (puede ser null)
     * @return el email en minúsculas, o null si venía vacío (para que el UNIQUE
     *         de la tabla no choque entre dos usuarios sin email)
     */
    private String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        if (e.length() == 0) {
            return null;
        }
        return e;
    }
}
