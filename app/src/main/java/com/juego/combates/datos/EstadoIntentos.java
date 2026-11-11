/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  EstadoIntentos.java
 */
package com.juego.combates.datos;

/**
 * Los contadores de fallos y los datos del bloqueo de una cuenta. Lo saco a
 * una clase porque lo uso igual para las cuentas reales (tabla usuarios) y
 * para los nombres que no existen (tabla intentos_nombre): así el bloqueo se
 * comporta exactamente igual exista o no la cuenta.
 *
 * Del bloqueo guardo tres relojes distintos, porque el de pared se puede
 * cambiar desde Ajustes:
 * - bloqueadoEn / bloqueadoHasta: reloj de pared (System.currentTimeMillis).
 * - bloqueoElapsedHasta: SystemClock.elapsedRealtime, que solo avanza y se
 *   pone a cero al reiniciar el móvil.
 * - bloqueoArranque: el número de arranques del móvil (BOOT_COUNT) cuando se
 *   bloqueó, para saber si elapsedRealtime sigue valiendo.
 * Esta clase solo guarda los datos; quién decide si el bloqueo sigue vigente
 * y cuándo se arma es ControlBloqueo (minutosDeBloqueo y apuntarFallo), y
 * quién los lee y escribe en SQLite es RepositorioUsuarios.
 */
public class EstadoIntentos {

    private int intentosLogin;
    private int intentosRecuperacion;
    private long bloqueadoEn;          // 0 = sin bloqueo
    private long bloqueadoHasta;       // 0 = sin bloqueo
    private long bloqueoElapsedHasta;  // 0 = sin bloqueo
    private long bloqueoArranque;      // -1 = no lo sé

    /** Estado limpio: sin fallos y sin bloqueo. */
    public EstadoIntentos() {
        this.intentosLogin = 0;
        this.intentosRecuperacion = 0;
        this.bloqueadoEn = 0;
        this.bloqueadoHasta = 0;
        this.bloqueoElapsedHasta = 0;
        this.bloqueoArranque = -1;
    }

    /**
     * @param intentosLogin        fallos seguidos al entrar
     * @param intentosRecuperacion fallos seguidos con la pregunta de seguridad
     * @param bloqueadoEn          reloj de pared al bloquear (0 si no hay bloqueo)
     * @param bloqueadoHasta       reloj de pared en que acaba el bloqueo
     * @param bloqueoElapsedHasta  elapsedRealtime en que acaba el bloqueo
     * @param bloqueoArranque      BOOT_COUNT del móvil al bloquear, o -1
     */
    public EstadoIntentos(int intentosLogin, int intentosRecuperacion, long bloqueadoEn,
                          long bloqueadoHasta, long bloqueoElapsedHasta, long bloqueoArranque) {
        this.intentosLogin = intentosLogin;
        this.intentosRecuperacion = intentosRecuperacion;
        this.bloqueadoEn = bloqueadoEn;
        this.bloqueadoHasta = bloqueadoHasta;
        this.bloqueoElapsedHasta = bloqueoElapsedHasta;
        this.bloqueoArranque = bloqueoArranque;
    }

    /**
     * @return true si hay un bloqueo apuntado (vigente o no, eso lo decide ControlBloqueo)
     */
    public boolean tieneBloqueo() {
        return bloqueadoHasta > 0;
    }

    /** Borra los datos del bloqueo (los contadores no los toco). */
    public void quitarBloqueo() {
        this.bloqueadoEn = 0;
        this.bloqueadoHasta = 0;
        this.bloqueoElapsedHasta = 0;
        this.bloqueoArranque = -1;
    }

    public int getIntentosLogin() {
        return intentosLogin;
    }

    public void setIntentosLogin(int intentosLogin) {
        this.intentosLogin = intentosLogin;
    }

    public int getIntentosRecuperacion() {
        return intentosRecuperacion;
    }

    public void setIntentosRecuperacion(int intentosRecuperacion) {
        this.intentosRecuperacion = intentosRecuperacion;
    }

    public long getBloqueadoEn() {
        return bloqueadoEn;
    }

    public void setBloqueadoEn(long bloqueadoEn) {
        this.bloqueadoEn = bloqueadoEn;
    }

    public long getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(long bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public long getBloqueoElapsedHasta() {
        return bloqueoElapsedHasta;
    }

    public void setBloqueoElapsedHasta(long bloqueoElapsedHasta) {
        this.bloqueoElapsedHasta = bloqueoElapsedHasta;
    }

    public long getBloqueoArranque() {
        return bloqueoArranque;
    }

    public void setBloqueoArranque(long bloqueoArranque) {
        this.bloqueoArranque = bloqueoArranque;
    }
}
