/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Criatura.java
 */
package com.juego.combates.modelo;

import java.util.ArrayList;

/**
 * Una criatura que combate. Tiene sus estadísticas, la vida que le queda
 * y una lista de hasta 4 movimientos.
 *
 * Cada objeto es un ejemplar de un combate concreto: si los dos bandos sacan
 * la misma especie son dos objetos distintos, cada uno con su vida y sus PP.
 */
public class Criatura {

    // Las estadísticas son final: se fijan al crear la criatura y no cambian en el combate.
    private final String nombre;
    private final Tipo tipo;
    private final int nivel;
    private final int hpMax;
    private final int ataque;
    private final int defensa;
    private final int velocidad;
    private int hp;                              // vida actual (esta sí cambia)
    private ArrayList<Movimiento> movimientos;

    /**
     * Crea la criatura con la vida llena y sin movimientos (se añaden después con agregarMovimiento).
     *
     * @param nombre    nombre de la especie
     * @param tipo      tipo elemental
     * @param nivel     nivel (todas van a 50)
     * @param hpMax     vida máxima
     * @param ataque    estadística de ataque
     * @param defensa   estadística de defensa
     * @param velocidad estadística de velocidad (decide quién ataca primero)
     */
    public Criatura(String nombre, Tipo tipo, int nivel, int hpMax, int ataque, int defensa, int velocidad) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.nivel = nivel;
        this.hpMax = hpMax;
        this.ataque = ataque;
        this.defensa = defensa;
        this.velocidad = velocidad;
        this.hp = hpMax; // empieza con la vida llena
        this.movimientos = new ArrayList<Movimiento>();
    }

    // ---- Getters y setters ----

    public String getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getNivel() {
        return nivel;
    }

    public int getHpMax() {
        return hpMax;
    }

    public int getAtaque() {
        return ataque;
    }

    public int getDefensa() {
        return defensa;
    }

    public int getVelocidad() {
        return velocidad;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public ArrayList<Movimiento> getMovimientos() {
        return movimientos;
    }

    // ---- Métodos ----

    /**
     * Añade un movimiento a la lista.
     *
     * @param m movimiento a añadir
     */
    public void agregarMovimiento(Movimiento m) {
        movimientos.add(m);
    }

    /**
     * @return true si se ha quedado sin vida
     */
    public boolean estaDebilitada() {
        return hp <= 0;
    }

    /**
     * Le quita vida. No la dejo bajar de 0 para que la barra de vida no se dibuje mal.
     *
     * @param dano puntos de vida a quitar
     */
    public void recibirDano(int dano) {
        hp = hp - dano;
        if (hp < 0) {
            hp = 0;
        }
    }

    /**
     * Le suma vida sin pasarse del máximo.
     *
     * @param cantidad puntos de vida a sumar
     * @return los puntos que ha recuperado de verdad (0 si ya estaba llena)
     */
    public int curar(int cantidad) {
        int antes = hp;
        hp = hp + cantidad;
        if (hp > hpMax) {
            hp = hpMax;
        }
        return hp - antes;
    }

    /**
     * Vida que le queda entre 0 y 1. Hago el cast a float antes de dividir
     * porque si no sería división entera y daría siempre 0.
     *
     * @return fracción de vida (1 = llena)
     */
    public float fraccionVida() {
        if (hpMax <= 0) {
            return 0f;
        }
        return (float) hp / (float) hpMax;
    }

    /**
     * @return true si algún movimiento tiene PP; si no, tendrá que usar Forcejeo
     */
    public boolean tieneAlgunPP() {
        for (int i = 0; i < movimientos.size(); i++) {
            if (movimientos.get(i).tienePP()) {
                return true;
            }
        }
        return false;
    }
}
