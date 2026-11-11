/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Sesion.java
 */
package com.juego.combates.datos;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Recuerda quién ha entrado para no pedir la contraseña en cada arranque.
 *
 * Solo guardo el id y el nombre de usuario en unas SharedPreferences con
 * MODE_PRIVATE, que ninguna otra app puede leer.
 * No lo cifro porque aquí no hay ningún secreto: ni contraseñas, ni hashes,
 * ni claves de acceso. Si alguien con root leyera el archivo solo vería "id=3,
 * nombre=ana", que es lo mismo que sale en la pantalla del menú. Las
 * contraseñas están en la base de datos y hasheadas (ver Hash.java).
 */
public class Sesion {

    private static final String ARCHIVO = "sesion";
    private static final String CLAVE_ID = "id_usuario";
    private static final String CLAVE_NOMBRE = "nombre_usuario";

    private final SharedPreferences prefs;

    /**
     * @param contexto cualquier contexto
     */
    public Sesion(Context contexto) {
        prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /**
     * Guarda la sesión de un usuario que acaba de entrar.
     *
     * @param usuario el usuario autenticado
     */
    public void guardar(Usuario usuario) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(CLAVE_ID, usuario.getId());
        editor.putString(CLAVE_NOMBRE, usuario.getUsuario());
        editor.apply(); // apply() escribe en segundo plano; commit() bloquearía
    }

    /**
     * Jugar sin cuenta: borro la sesión que hubiera. No guardo ninguna marca de
     * "invitado" porque nadie la lee: ser invitado es simplemente no tener id,
     * así que en el siguiente arranque haySesion() dará false y volverá a
     * salir el login.
     */
    public void entrarComoInvitado() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(CLAVE_ID);
        editor.remove(CLAVE_NOMBRE);
        editor.apply();
    }

    /**
     * @return true si hay un usuario con cuenta dentro
     */
    public boolean haySesion() {
        return prefs.getLong(CLAVE_ID, 0) > 0;
    }

    /**
     * @return el id del usuario, o 0 si no hay sesión
     */
    public long getIdUsuario() {
        return prefs.getLong(CLAVE_ID, 0);
    }

    /**
     * @return el nombre del usuario, o null si no hay sesión
     */
    public String getNombreUsuario() {
        return prefs.getString(CLAVE_NOMBRE, null);
    }

    /**
     * Borra todo (cerrar sesión).
     */
    public void cerrar() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
