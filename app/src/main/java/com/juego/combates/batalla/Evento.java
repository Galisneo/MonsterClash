/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Evento.java
 */
package com.juego.combates.batalla;

import com.juego.combates.modelo.Criatura;

/**
 * Un paso del combate: un mensaje de texto y el efecto visual que lo acompaña.
 *
 * Guarda también una "foto" del estado en ese momento (qué criatura hay en cada
 * lado y su vida). Lo necesito porque el motor calcula el turno entero de golpe
 * pero la pantalla enseña los mensajes uno a uno: si la vista mirase el estado
 * real, vería el resultado final desde el primer mensaje.
 *
 * El "bando" dice de quién habla el mensaje (jugador, rival o el narrador), y la
 * pantalla lo usa para colocar la burbuja del chat a un lado o al otro.
 */
public class Evento {

    // Efectos visuales posibles
    public static final int NADA = 0;            // solo texto
    public static final int GOLPE_RIVAL = 1;     // sacudir al rival
    public static final int GOLPE_JUGADOR = 2;   // sacudir al jugador
    public static final int REFRESCO = 3;        // cambio de criatura o curación

    // De quién habla el mensaje
    public static final int NEUTRAL = 0;         // narrador
    public static final int JUGADOR = 1;
    public static final int RIVAL = 2;

    // Todo final: un evento es una foto y no cambia una vez creado.
    private final String texto;
    private final int efecto;
    private final int bando;
    // jugador y rival son referencias vivas a las criaturas reales (su hp sigue cambiando
    // después), por eso guardo aparte los dos int de hp: son la copia del valor en ese momento.
    private final Criatura jugador;
    private final int hpJugador;
    private final Criatura rival;
    private final int hpRival;

    /**
     * Constructor de un evento del narrador (bando NEUTRAL).
     *
     * @param texto     mensaje a mostrar
     * @param efecto    una de las constantes de efecto
     * @param jugador   criatura activa del jugador en ese momento
     * @param hpJugador su vida en ese momento
     * @param rival     criatura activa del rival en ese momento
     * @param hpRival   su vida en ese momento
     */
    public Evento(String texto, int efecto, Criatura jugador, int hpJugador, Criatura rival, int hpRival) {
        this(texto, efecto, NEUTRAL, jugador, hpJugador, rival, hpRival);
    }

    /**
     * Constructor completo, con el bando del que habla el mensaje.
     *
     * @param texto     mensaje a mostrar
     * @param efecto    una de las constantes de efecto
     * @param bando     NEUTRAL, JUGADOR o RIVAL
     * @param jugador   criatura activa del jugador en ese momento
     * @param hpJugador su vida en ese momento
     * @param rival     criatura activa del rival en ese momento
     * @param hpRival   su vida en ese momento
     */
    public Evento(String texto, int efecto, int bando, Criatura jugador, int hpJugador, Criatura rival, int hpRival) {
        this.texto = texto;
        this.efecto = efecto;
        this.bando = bando;
        this.jugador = jugador;
        this.hpJugador = hpJugador;
        this.rival = rival;
        this.hpRival = hpRival;
    }

    // Solo getters: un evento no se modifica una vez creado

    public String getTexto() {
        return texto;
    }

    public int getEfecto() {
        return efecto;
    }

    public int getBando() {
        return bando;
    }

    public Criatura getJugador() {
        return jugador;
    }

    public int getHpJugador() {
        return hpJugador;
    }

    public Criatura getRival() {
        return rival;
    }

    public int getHpRival() {
        return hpRival;
    }
}
