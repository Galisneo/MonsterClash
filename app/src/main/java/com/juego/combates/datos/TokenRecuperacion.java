/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  TokenRecuperacion.java
 */
package com.juego.combates.datos;

import android.os.SystemClock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * El permiso de un solo uso que demuestra que se ha acertado la pregunta de
 * seguridad. verificarRespuesta lo crea al acertar y cambiarPassword lo exige:
 * así nadie puede cambiar la contraseña saltándose el paso 2.
 *
 * Vive solo en memoria y no en la base de datos: si la app se cierra, el
 * permiso se pierde y hay que responder otra vez, que es justo lo que quiero.
 * Solo hay uno a la vez (crear otro invalida el anterior), se gasta al
 * comprobarlo valga o no, y caduca a los TOKEN_MS medidos con elapsedRealtime
 * y no con la hora del móvil, por lo mismo que el bloqueo (ver ControlBloqueo).
 *
 * Los dos métodos son synchronized porque verificarRespuesta y cambiarPassword
 * se llaman desde Threads distintos.
 *
 * Es package-private: solo la usa RepositorioUsuarios.
 */
class TokenRecuperacion {

    /** Cuánto vale el token desde que se acierta la pregunta: 10 minutos. */
    static final long TOKEN_MS = 10L * 60L * 1000L;
    /** Bytes aleatorios del token (en hexadecimal salen 32 caracteres). */
    static final int TAM_TOKEN = 16;

    // uno solo para toda la app: crear un SecureRandom cada vez es caro y no aporta nada
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private String valor;          // 32 caracteres hex, o null si no hay token
    private String usuario;        // usuario normalizado al que pertenece
    private long caducaElapsed;    // SystemClock.elapsedRealtime en que caduca

    /** Empieza sin token: hay que ganárselo acertando la pregunta. */
    TokenRecuperacion() {
        this.valor = null;
        this.usuario = null;
        this.caducaElapsed = 0;
    }

    /**
     * Crea un token nuevo para ese usuario (y tira el que hubiera).
     *
     * @param usuarioNormal usuario (ya en minúsculas) al que pertenece
     * @return el token en hex (32 caracteres)
     */
    synchronized String crear(String usuarioNormal) {
        byte[] bytes = new byte[TAM_TOKEN];
        ALEATORIO.nextBytes(bytes);
        valor = Hash.aHex(bytes);
        usuario = usuarioNormal;
        caducaElapsed = SystemClock.elapsedRealtime() + TOKEN_MS;
        return valor;
    }

    /**
     * Comprueba el token y lo gasta. Lo borro ANTES de comparar: sea válido o
     * no, ya no sirve para un segundo intento. La comparación es en tiempo
     * constante (MessageDigest.isEqual), igual que la de los hashes.
     *
     * @param usuarioNormal usuario (ya en minúsculas) que quiere cambiar la contraseña
     * @param token         el que trae la pantalla
     * @return true si el token era el de ese usuario y no había caducado
     */
    synchronized boolean consumir(String usuarioNormal, String token) {
        String valorGuardado = valor;
        String usuarioGuardado = usuario;
        long caduca = caducaElapsed;
        valor = null;
        usuario = null;
        caducaElapsed = 0;

        if (valorGuardado == null || token == null) {
            return false;
        }
        if (SystemClock.elapsedRealtime() > caduca) {
            return false;
        }
        boolean mismoUsuario = usuarioGuardado.equals(usuarioNormal);
        boolean mismoToken = MessageDigest.isEqual(
                valorGuardado.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
        return mismoUsuario && mismoToken;
    }
}
