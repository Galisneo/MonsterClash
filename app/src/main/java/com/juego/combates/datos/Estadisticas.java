/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Estadisticas.java
 */
package com.juego.combates.datos;

/**
 * Resumen de las partidas de un usuario: cuántas ha jugado, ganado y perdido.
 * Lo devuelve RepositorioPartidas.estadisticas() y lo enseña el menú.
 */
public class Estadisticas {

    private int partidas;
    private int victorias;
    private int derrotas;

    /**
     * Todo a cero; RepositorioPartidas lo rellena con los setters según lo que
     * devuelva la consulta.
     */
    public Estadisticas() {
    }

    /**
     * @param partidas  total jugadas
     * @param victorias las ganadas
     * @param derrotas  las perdidas
     */
    public Estadisticas(int partidas, int victorias, int derrotas) {
        this.partidas = partidas;
        this.victorias = victorias;
        this.derrotas = derrotas;
    }

    public int getPartidas() {
        return partidas;
    }

    public void setPartidas(int partidas) {
        this.partidas = partidas;
    }

    public int getVictorias() {
        return victorias;
    }

    public void setVictorias(int victorias) {
        this.victorias = victorias;
    }

    public int getDerrotas() {
        return derrotas;
    }

    public void setDerrotas(int derrotas) {
        this.derrotas = derrotas;
    }
}
