/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Movimiento.java
 */
package com.juego.combates.modelo;

/**
 * Un movimiento de una criatura: un ataque o una curación.
 * Los ataques quitan vida al rival. Las curaciones recuperan vida al que las usa.
 * Todos gastan PP (puntos de poder) cada vez que se usan.
 */
public class Movimiento {

    // Los datos de la ficha son final; solo cambian los PP que quedan.
    private final String nombre;
    private final Tipo tipo;
    private final int potencia;      // en las curaciones es 0
    private final int precision;     // porcentaje de acierto, de 0 a 100
    private final int ppMax;         // usos totales
    private int pp;                  // usos que quedan
    private final boolean curacion;  // true si recupera vida en vez de quitarla

    /**
     * Constructor para un ataque.
     *
     * @param nombre    nombre del ataque
     * @param tipo      tipo elemental
     * @param potencia  fuerza del ataque
     * @param precision probabilidad de acertar (0-100)
     * @param ppMax     número de usos
     */
    public Movimiento(String nombre, Tipo tipo, int potencia, int precision, int ppMax) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.potencia = potencia;
        this.precision = precision;
        this.ppMax = ppMax;
        this.pp = ppMax;
        this.curacion = false;
    }

    /**
     * Constructor para una curación (sobrecarga con menos parámetros).
     * La potencia se pone a 0 y la precisión a 100 para que no falle nunca.
     *
     * @param nombre nombre de la curación
     * @param tipo   tipo elemental
     * @param ppMax  número de usos
     */
    public Movimiento(String nombre, Tipo tipo, int ppMax) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.potencia = 0;
        this.precision = 100;
        this.ppMax = ppMax;
        this.pp = ppMax;
        this.curacion = true;
    }

    // ---- Getters y setters ----

    public String getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getPotencia() {
        return potencia;
    }

    public int getPrecision() {
        return precision;
    }

    public int getPpMax() {
        return ppMax;
    }

    public int getPp() {
        return pp;
    }

    public void setPp(int pp) {
        this.pp = pp;
    }

    /**
     * @return true si el movimiento cura en vez de atacar
     */
    public boolean esCuracion() {
        return curacion;
    }

    // ---- Métodos ----

    /**
     * @return true si le quedan usos
     */
    public boolean tienePP() {
        return pp > 0;
    }

    /**
     * Resta un uso. Se llama cada vez que la criatura usa el movimiento.
     */
    public void gastarPP() {
        pp--;
    }
}
