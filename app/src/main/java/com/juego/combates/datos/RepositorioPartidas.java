/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  RepositorioPartidas.java
 */
package com.juego.combates.datos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Guarda cada combate terminado y calcula las estadísticas de un usuario.
 * Como en RepositorioUsuarios, los parámetros van siempre con "?".
 */
public class RepositorioPartidas {

    private final BaseDatos baseDatos;

    /**
     * @param contexto cualquier contexto (uso el de aplicación por dentro)
     */
    public RepositorioPartidas(Context contexto) {
        this.baseDatos = BaseDatos.get(contexto);
    }

    /**
     * Inserta una partida terminada.
     *
     * @param idUsuario dueño de la partida
     * @param victoria  true si ganó el jugador
     * @param equipo    nombres de sus 3 criaturas (los guardo separados por comas)
     * @param turnos    turnos que duró el combate
     * @return el id de la fila nueva, o -1 si falló
     */
    public long guardar(long idUsuario, boolean victoria, String[] equipo, int turnos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < equipo.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(equipo[i]);
        }

        ContentValues valores = new ContentValues();
        valores.put("usuario_id", idUsuario);
        if (victoria) {
            valores.put("victoria", 1); // SQLite no tiene boolean: 1 = victoria, 0 = derrota
        } else {
            valores.put("victoria", 0);
        }
        valores.put("equipo", sb.toString());
        valores.put("turnos", turnos);
        valores.put("jugada_en", System.currentTimeMillis());

        SQLiteDatabase db = baseDatos.getWritableDatabase();
        return db.insert("partidas", null, valores);
    }

    /**
     * Cuenta las partidas de un usuario con una sola consulta: COUNT para el
     * total y SUM(victoria) para las ganadas (como victoria es 0/1, la suma es
     * el número de victorias). COALESCE porque SUM de cero filas da NULL.
     *
     * @param idUsuario usuario a consultar
     * @return sus estadísticas (todo a 0 si no ha jugado)
     */
    public Estadisticas estadisticas(long idUsuario) {
        SQLiteDatabase db = baseDatos.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(victoria), 0) FROM partidas WHERE usuario_id = ?",
                new String[]{String.valueOf(idUsuario)});
        try {
            Estadisticas e = new Estadisticas();
            if (c.moveToFirst()) {
                int partidas = c.getInt(0);
                int victorias = c.getInt(1);
                e.setPartidas(partidas);
                e.setVictorias(victorias);
                e.setDerrotas(partidas - victorias);
            }
            return e;
        } finally {
            c.close();
        }
    }
}
