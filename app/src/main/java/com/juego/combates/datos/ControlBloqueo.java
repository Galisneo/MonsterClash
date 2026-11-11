/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  ControlBloqueo.java
 */
package com.juego.combates.datos;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Las reglas del bloqueo por intentos: cuántos fallos se aguantan, cuánto dura
 * el bloqueo y, sobre todo, cómo saber si sigue vigente sin fiarme solo de la
 * hora del móvil. Trabaja sobre un EstadoIntentos (los contadores leídos de la
 * tabla) y no toca la base de datos: eso lo hace RepositorioUsuarios.
 *
 * Va aparte del repositorio para que las cuentas reales y los nombres que no
 * existen usen exactamente las mismas reglas y para que el repositorio se
 * quede solo con el SQL.
 *
 * Del bloqueo se guardan tres relojes distintos, porque el de pared se puede
 * cambiar desde Ajustes en dos toques:
 * - bloqueadoEn / bloqueadoHasta: reloj de pared (System.currentTimeMillis).
 * - bloqueoElapsedHasta: SystemClock.elapsedRealtime, que solo avanza y nadie
 *   puede tocar, pero que vuelve a cero al reiniciar el móvil.
 * - bloqueoArranque: el número de arranques del móvil (BOOT_COUNT) cuando se
 *   bloqueó, para saber si elapsedRealtime sigue valiendo.
 *
 * Es package-private: solo la usa el paquete datos.
 */
class ControlBloqueo {

    /** Fallos seguidos que aguanto antes de bloquear. */
    static final int MAX_INTENTOS = 5;
    /** Milisegundos que tiene un minuto, para pasar de ms a minutos sin números sueltos. */
    static final long MS_POR_MINUTO = 60000L;
    /** Cuánto dura el bloqueo: 15 minutos. */
    static final long BLOQUEO_MS = 15L * MS_POR_MINUTO;

    private final Context contexto;

    /**
     * @param contexto cualquier contexto; me quedo con el de aplicación para
     *                 poder leer BOOT_COUNT sin retener una Activity
     */
    ControlBloqueo(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    /**
     * Dice si el bloqueo sigue vigente y cuántos minutos le quedan.
     *
     * - Si el móvil no se ha reiniciado desde que bloqueé (mismo BOOT_COUNT),
     *   mando con SystemClock.elapsedRealtime. Da igual lo que hagan con la
     *   hora: ni acorta ni alarga el bloqueo.
     * - Si se ha reiniciado (elapsedRealtime vuelve a cero) solo me queda el
     *   reloj de pared. Si veo que ha ido hacia ATRÁS respecto al momento del
     *   bloqueo es que alguien lo ha movido: rearmo 15 minutos desde ahora, ni
     *   gratis ni eterno.
     * Lo que queda va acotado a BLOQUEO_MS para que nunca salga "espera 3 días".
     *
     * @param estado los contadores leídos de la tabla (puede rearmarse aquí,
     *               por eso quien me llama tiene que volver a guardarlo)
     * @return minutos que faltan, o 0 si no hay bloqueo vigente
     */
    int minutosDeBloqueo(EstadoIntentos estado) {
        if (!estado.tieneBloqueo()) {
            return 0;
        }
        long arranque = contarArranques();
        boolean mismoArranque = arranque >= 0 && arranque == estado.getBloqueoArranque();
        if (mismoArranque) {
            long restante = estado.getBloqueoElapsedHasta() - SystemClock.elapsedRealtime();
            if (restante <= 0) {
                return 0;
            }
            return aMinutos(restante);
        }

        long ahora = System.currentTimeMillis();
        if (ahora >= estado.getBloqueadoHasta()) {
            return 0;
        }
        if (ahora < estado.getBloqueadoEn()) {
            armarBloqueo(estado);
            return aMinutos(BLOQUEO_MS);
        }
        return aMinutos(estado.getBloqueadoHasta() - ahora);
    }

    /**
     * Suma un fallo y, si llega al tope, arma el bloqueo. El contador se queda
     * como está (a cero solo lo pone un acierto): así, si alguien se saltara el
     * bloqueo, el siguiente fallo volvería a bloquear en vez de regalarle cinco.
     *
     * @param estado  los contadores a modificar
     * @param deLogin true si el fallo es de contraseña, false si es de la pregunta
     * @return los minutos de bloqueo si acabo de bloquear, o 0
     */
    int apuntarFallo(EstadoIntentos estado, boolean deLogin) {
        int intentos;
        if (deLogin) {
            intentos = estado.getIntentosLogin() + 1;
            estado.setIntentosLogin(intentos);
        } else {
            intentos = estado.getIntentosRecuperacion() + 1;
            estado.setIntentosRecuperacion(intentos);
        }
        if (intentos >= MAX_INTENTOS) {
            armarBloqueo(estado);
            return aMinutos(BLOQUEO_MS);
        }
        return 0;
    }

    /**
     * Apunta los tres relojes de un bloqueo de BLOQUEO_MS a partir de ahora.
     *
     * @param estado los contadores donde apunto el bloqueo
     */
    private void armarBloqueo(EstadoIntentos estado) {
        long ahora = System.currentTimeMillis();
        estado.setBloqueadoEn(ahora);
        estado.setBloqueadoHasta(ahora + BLOQUEO_MS);
        estado.setBloqueoElapsedHasta(SystemClock.elapsedRealtime() + BLOQUEO_MS);
        estado.setBloqueoArranque(contarArranques());
    }

    /**
     * @return cuántas veces ha arrancado el móvil (BOOT_COUNT, API 24), o -1 si
     *         este dispositivo no lo tiene
     */
    private long contarArranques() {
        try {
            return Settings.Global.getInt(contexto.getContentResolver(), Settings.Global.BOOT_COUNT);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    /**
     * Pasa milisegundos a minutos redondeando hacia arriba (un segundo de
     * bloqueo ya cuenta como "espera 1 minuto").
     *
     * @param ms milisegundos que faltan
     * @return los minutos, entre 1 y los de BLOQUEO_MS
     */
    private int aMinutos(long ms) {
        int minutos = (int) ((ms + MS_POR_MINUTO - 1) / MS_POR_MINUTO);
        int tope = (int) (BLOQUEO_MS / MS_POR_MINUTO);
        if (minutos < 1) {
            minutos = 1;
        }
        if (minutos > tope) {
            minutos = tope;
        }
        return minutos;
    }
}
