/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  BaseDatos.java
 */
package com.juego.combates.datos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Abre y crea la base de datos SQLite "monsterclash.db" (cuatro tablas:
 * usuarios, partidas, intentos_nombre y ajustes). Solo el SDK:
 * android.database.sqlite, sin Room ni librerías.
 *
 * Es un singleton con el contexto de aplicación: si cada Activity abriera su
 * propio helper tendría varias conexiones a la vez al mismo archivo y podría
 * saltar "database is locked". El archivo vive en la carpeta privada de la app
 * (/data/data/com.juego.combates/databases/), donde ninguna otra app puede leer.
 */
public class BaseDatos extends SQLiteOpenHelper {

    private static final String NOMBRE = "monsterclash.db";
    /**
     * Versión 2: añado a usuarios los tres campos del bloqueo resistente a
     * cambios de hora, la tabla intentos_nombre (bloqueo de nombres que no
     * existen) y la tabla ajustes (semilla de la pregunta ficticia).
     */
    private static final int VERSION = 2;

    /** Columnas de contadores y bloqueo, iguales en usuarios y en intentos_nombre. */
    private static final String COLUMNAS_BLOQUEO =
            "intentos_fallidos INTEGER NOT NULL DEFAULT 0, "
            + "bloqueado_hasta INTEGER NOT NULL DEFAULT 0, "
            + "intentos_recuperacion INTEGER NOT NULL DEFAULT 0, "
            + "bloqueado_en INTEGER NOT NULL DEFAULT 0, "
            + "bloqueo_elapsed_hasta INTEGER NOT NULL DEFAULT 0, "
            + "bloqueo_arranque INTEGER NOT NULL DEFAULT -1";

    private static BaseDatos instancia;

    private BaseDatos(Context contexto) {
        super(contexto, NOMBRE, null, VERSION);
    }

    /**
     * @param contexto cualquier contexto; me quedo con el de aplicación para no
     *                 retener una Activity en memoria
     * @return la única instancia del helper
     */
    public static synchronized BaseDatos get(Context contexto) {
        if (instancia == null) {
            instancia = new BaseDatos(contexto.getApplicationContext());
        }
        return instancia;
    }

    /**
     * SQLite no comprueba las claves ajenas si no se lo pides: aquí lo activo
     * para que al borrar un usuario se borren sus partidas (ON DELETE CASCADE).
     *
     * @param db la base de datos recién abierta, antes de onCreate/onUpgrade
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Se ejecuta solo la primera vez que se abre la base de datos en el móvil.
     *
     * @param db la base de datos recién creada
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // las contraseñas y las respuestas van hasheadas (ver Hash.java), nunca en claro
        db.execSQL("CREATE TABLE usuarios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario TEXT NOT NULL UNIQUE, "
                + "email TEXT UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "pregunta INTEGER NOT NULL, "
                + "respuesta_hash TEXT NOT NULL, "
                + "creado_en INTEGER NOT NULL, "
                + COLUMNAS_BLOQUEO + ")");

        db.execSQL("CREATE TABLE partidas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, "
                + "victoria INTEGER NOT NULL, "
                + "equipo TEXT NOT NULL, "
                + "turnos INTEGER, "
                + "jugada_en INTEGER NOT NULL)");

        // las estadísticas se consultan siempre por usuario, así que le pongo índice
        db.execSQL("CREATE INDEX idx_partidas_usuario ON partidas(usuario_id)");

        crearTablasVersion2(db);
    }

    /**
     * Tablas que llegaron con la versión 2. Las creo desde un método aparte
     * para llamarlo tanto desde onCreate (móvil nuevo) como desde onUpgrade
     * (móvil que ya tenía la versión 1).
     *
     * @param db la base de datos abierta
     */
    private void crearTablasVersion2(SQLiteDatabase db) {
        // contadores de los nombres que NO existen: así un nombre inventado se
        // bloquea igual que una cuenta real y no se puede distinguir uno de otro
        db.execSQL("CREATE TABLE intentos_nombre ("
                + "nombre TEXT PRIMARY KEY, "
                + COLUMNAS_BLOQUEO + ")");

        // pares clave/valor de la propia app (por ahora solo la semilla de las
        // preguntas ficticias de RecuperarActivity)
        db.execSQL("CREATE TABLE ajustes ("
                + "clave TEXT PRIMARY KEY, "
                + "valor TEXT NOT NULL)");
    }

    /**
     * Migra una base de datos vieja sin perder las cuentas. Cada "if" sube una
     * versión, así que si algún día hay una 3 solo tengo que añadir otro.
     *
     * @param db           la base de datos abierta
     * @param versionVieja la versión que tenía el móvil
     * @param versionNueva la VERSION actual
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int versionVieja, int versionNueva) {
        if (versionVieja < 2) {
            // SQLite solo deja añadir una columna por ALTER TABLE
            db.execSQL("ALTER TABLE usuarios ADD COLUMN bloqueado_en INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN bloqueo_elapsed_hasta INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN bloqueo_arranque INTEGER NOT NULL DEFAULT -1");
            crearTablasVersion2(db);
        }
    }
}
