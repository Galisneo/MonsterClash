/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  DetalleGolpe.java
 */
package com.juego.combates.vistas;

import com.juego.combates.modelo.Tipo;

/**
 * Datos visuales de un ataque interpretados desde la lista de eventos.
 *
 * No pertenece al motor: solo ayuda a VistaBatalla a saber qué tipo de golpe,
 * daño, crítico o fallo tiene que enseñar.
 */
public class DetalleGolpe {

    private Tipo tipo;
    private int dano;
    private boolean critico;
    private double eficacia;
    private boolean ko;
    private boolean fallo;
    private boolean curacion;
    private boolean atacaJugador;

    /**
     * Arranca con tipo NORMAL y eficacia 1.0 para que un detalle vacío no rompa nada.
     */
    public DetalleGolpe() {
        tipo = Tipo.NORMAL;
        eficacia = 1.0;
    }

    // ---- Getters y setters ----
    // Los booleanos van con verbo (esCritico, haFallado...) como el resto del
    // proyecto (estaDebilitada, tienePP, haySesion), no con getX.

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public int getDano() {
        return dano;
    }

    public void setDano(int dano) {
        this.dano = dano;
    }

    public boolean esCritico() {
        return critico;
    }

    public void setCritico(boolean critico) {
        this.critico = critico;
    }

    public double getEficacia() {
        return eficacia;
    }

    public void setEficacia(double eficacia) {
        this.eficacia = eficacia;
    }

    public boolean esKo() {
        return ko;
    }

    public void setKo(boolean ko) {
        this.ko = ko;
    }

    public boolean haFallado() {
        return fallo;
    }

    public void setFallo(boolean fallo) {
        this.fallo = fallo;
    }

    public boolean esCuracion() {
        return curacion;
    }

    public void setCuracion(boolean curacion) {
        this.curacion = curacion;
    }

    public boolean atacaJugador() {
        return atacaJugador;
    }

    public void setAtacaJugador(boolean atacaJugador) {
        this.atacaJugador = atacaJugador;
    }

    /**
     * @return texto corto para Log.d
     */
    public String resumen() {
        return "tipo=" + tipo.getNombre()
                + " dano=" + dano
                + " critico=" + critico
                + " eficacia=" + eficacia
                + " ko=" + ko
                + " fallo=" + fallo
                + " curacion=" + curacion
                + " atacaJugador=" + atacaJugador;
    }
}
