/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Batalla.java
 */
package com.juego.combates.batalla;

import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Movimiento;
import com.juego.combates.modelo.Tipo;

import java.util.ArrayList;
import java.util.Random;

/**
 * Motor del combate: las reglas del juego (orden de ataque, daño, PP, curaciones
 * e inteligencia del rival). No usa nada de Android para poder compilarlo y
 * probarlo con el javac del PC desde pruebas/ArnesBatalla.java (300 combates
 * con semilla fija comparados con una referencia).
 *
 * Cada acción del jugador devuelve una lista de Eventos con todo lo que ha pasado
 * en el turno. La pantalla los va enseñando uno a uno.
 */
public class Batalla {

    private ArrayList<Criatura> equipoJugador;
    private ArrayList<Criatura> equipoRival;
    private Criatura activaJugador;    // la que está peleando ahora
    private Criatura activaRival;
    private Random rng;
    private Movimiento forcejeo;       // ataque de emergencia cuando no quedan PP

    // ---- Constantes de las reglas ----
    // Las saco aquí con nombre para que los números de la fórmula no queden sueltos por el código.

    /** Nombre del ataque de emergencia. Es público porque la pantalla también lo enseña. */
    public static final String NOMBRE_FORCEJEO = "Forcejeo";

    private static final int POTENCIA_FORCEJEO = 35;       // flojo a propósito: es el último recurso
    private static final int PRECISION_FORCEJEO = 100;     // nunca falla
    private static final int PP_FORCEJEO = 9999;           // ver el comentario del constructor

    private static final double MULTIPLICADOR_STAB = 1.5;  // bonus por usar un movimiento del propio tipo
    private static final int UNO_ENTRE_CRITICO = 12;       // probabilidad de crítico: 1 de cada 12
    private static final double MULTIPLICADOR_CRITICO = 1.5;
    private static final double VARIACION_MINIMA = 0.85;   // el daño va del 85%...
    private static final double RANGO_VARIACION = 0.15;    // ...al 100% (0.85 + 0.15)
    private static final int DIVISOR_CURACION = 2;         // una curación recupera la mitad de la vida máxima

    private static final float UMBRAL_VIDA_BAJA = 0.35f;   // por debajo de esto la IA se plantea curarse
    private static final int PROB_CURARSE = 55;            // % de veces que se cura estando baja de vida
    private static final int PROB_AZAR_IA = 20;            // % de veces que la IA elige al azar

    /**
     * Crea el combate con los dos equipos y deja como activa la primera criatura de cada uno.
     *
     * @param equipoJugador las 3 criaturas del jugador
     * @param equipoRival   las 3 del rival
     * @param rng           generador aleatorio (el mismo para toda la partida)
     */
    public Batalla(ArrayList<Criatura> equipoJugador, ArrayList<Criatura> equipoRival, Random rng) {
        this.equipoJugador = equipoJugador;
        this.equipoRival = equipoRival;
        this.rng = rng;
        this.activaJugador = equipoJugador.get(0);
        this.activaRival = equipoRival.get(0);
        // le pongo 9999 PP por completar el constructor, pero en atacar() nunca le gasto PP:
        // el Forcejeo siempre está disponible
        this.forcejeo = new Movimiento(NOMBRE_FORCEJEO, Tipo.NORMAL, POTENCIA_FORCEJEO, PRECISION_FORCEJEO, PP_FORCEJEO);
    }

    // ---- Getters ----

    public ArrayList<Criatura> getEquipoJugador() {
        return equipoJugador;
    }

    public ArrayList<Criatura> getEquipoRival() {
        return equipoRival;
    }

    public Criatura getActivaJugador() {
        return activaJugador;
    }

    public Criatura getActivaRival() {
        return activaRival;
    }

    // ---- Estado del combate ----

    /**
     * @return true si el jugador no tiene ninguna criatura viva
     */
    public boolean jugadorDerrotado() {
        return sinCriaturasVivas(equipoJugador);
    }

    /**
     * @return true si el rival no tiene ninguna criatura viva
     */
    public boolean rivalDerrotado() {
        return sinCriaturasVivas(equipoRival);
    }

    /**
     * Recorre un equipo buscando alguna criatura con vida.
     *
     * @param equipo lista de criaturas a revisar
     * @return true si todas están debilitadas
     */
    private boolean sinCriaturasVivas(ArrayList<Criatura> equipo) {
        for (int i = 0; i < equipo.size(); i++) {
            if (!equipo.get(i).estaDebilitada()) {
                return false;
            }
        }
        return true;
    }

    // ---- Acciones ----

    /**
     * @return los mensajes de presentación del combate
     */
    public ArrayList<Evento> eventosIntro() {
        ArrayList<Evento> eventos = new ArrayList<Evento>();
        eventos.add(crearEvento("¡Un entrenador rival te desafía!", Evento.NADA));
        eventos.add(crearEvento("El rival envía a " + activaRival.getNombre() + ".", Evento.REFRESCO, Evento.RIVAL));
        eventos.add(crearEvento("¡Adelante, " + activaJugador.getNombre() + "!", Evento.REFRESCO, Evento.JUGADOR));
        return eventos;
    }

    /**
     * Turno completo: el jugador ataca con el movimiento indicado y el rival responde.
     * Elijo los dos movimientos antes de resolver nada para que la IA no haga trampa
     * mirando el resultado de mi ataque.
     *
     * @param indiceMovimiento posición (0-3) del movimiento del jugador
     * @return lista de eventos del turno
     */
    public ArrayList<Evento> ejecutarTurno(int indiceMovimiento) {
        ArrayList<Evento> eventos = new ArrayList<Evento>();
        Movimiento movJugador = movimientoOForcejeo(activaJugador, indiceMovimiento);
        Movimiento movRival = elegirMovimientoIA();

        // el más rápido ataca primero; si empatan, a cara o cruz
        boolean jugadorPrimero;
        if (activaJugador.getVelocidad() > activaRival.getVelocidad()) {
            jugadorPrimero = true;
        } else if (activaJugador.getVelocidad() == activaRival.getVelocidad()) {
            jugadorPrimero = rng.nextBoolean();
        } else {
            jugadorPrimero = false;
        }

        // el segundo solo ataca si los dos siguen vivos tras el primer golpe
        if (jugadorPrimero) {
            atacar(activaJugador, activaRival, movJugador, true, eventos);
            if (!activaRival.estaDebilitada() && !activaJugador.estaDebilitada()) {
                atacar(activaRival, activaJugador, movRival, false, eventos);
            }
        } else {
            atacar(activaRival, activaJugador, movRival, false, eventos);
            if (!activaJugador.estaDebilitada() && !activaRival.estaDebilitada()) {
                atacar(activaJugador, activaRival, movJugador, true, eventos);
            }
        }

        reemplazoRivalSiHaceFalta(eventos);
        return eventos;
    }

    /**
     * Cambio de criatura voluntario. Gasta el turno: el rival ataca a la que entra.
     *
     * @param indiceEquipo posición en el equipo de la criatura que entra
     * @return lista de eventos
     */
    public ArrayList<Evento> cambioVoluntario(int indiceEquipo) {
        ArrayList<Evento> eventos = new ArrayList<Evento>();
        Criatura entrante = equipoJugador.get(indiceEquipo);
        // este mensaje va antes de cambiar la activa para que salga el nombre de la que se va
        eventos.add(crearEvento("¡Vuelve, " + activaJugador.getNombre() + "!", Evento.NADA, Evento.JUGADOR));
        activaJugador = entrante;
        eventos.add(crearEvento("¡Adelante, " + entrante.getNombre() + "!", Evento.REFRESCO, Evento.JUGADOR));

        Movimiento movRival = elegirMovimientoIA();
        atacar(activaRival, activaJugador, movRival, false, eventos);
        reemplazoRivalSiHaceFalta(eventos);
        return eventos;
    }

    /**
     * Cambio obligado tras un KO. Es gratis: el rival no ataca.
     *
     * @param indiceEquipo posición en el equipo de la criatura que entra
     * @return lista de eventos
     */
    public ArrayList<Evento> cambioTrasKO(int indiceEquipo) {
        ArrayList<Evento> eventos = new ArrayList<Evento>();
        activaJugador = equipoJugador.get(indiceEquipo);
        eventos.add(crearEvento("¡Adelante, " + activaJugador.getNombre() + "!", Evento.REFRESCO, Evento.JUGADOR));
        return eventos;
    }

    // ---- Métodos privados ----

    /**
     * Crea un evento del narrador con la foto del estado actual (criaturas activas y su vida).
     *
     * @param texto  mensaje a mostrar
     * @param efecto una de las constantes de efecto de Evento
     * @return el evento creado
     */
    private Evento crearEvento(String texto, int efecto) {
        return new Evento(texto, efecto, activaJugador, activaJugador.getHp(), activaRival, activaRival.getHp());
    }

    /**
     * Igual que el anterior pero indicando de quién habla el mensaje (para las burbujas del chat).
     *
     * @param texto  mensaje a mostrar
     * @param efecto una de las constantes de efecto de Evento
     * @param bando  Evento.NEUTRAL, Evento.JUGADOR o Evento.RIVAL
     * @return el evento creado
     */
    private Evento crearEvento(String texto, int efecto, int bando) {
        return new Evento(texto, efecto, bando, activaJugador, activaJugador.getHp(), activaRival, activaRival.getHp());
    }

    /**
     * Devuelve el movimiento de esa posición si existe y tiene PP; si no, Forcejeo.
     *
     * @param c      criatura que va a atacar
     * @param indice posición del movimiento en su lista
     * @return el movimiento elegido o el Forcejeo compartido
     */
    private Movimiento movimientoOForcejeo(Criatura c, int indice) {
        if (indice >= 0 && indice < c.getMovimientos().size()) {
            Movimiento m = c.getMovimientos().get(indice);
            if (m.tienePP()) {
                return m;
            }
        }
        return forcejeo;
    }

    /**
     * Calcula el STAB: si el movimiento es del mismo tipo que la criatura pega un 50% más.
     *
     * @param mov   movimiento que se va a usar
     * @param quien criatura que lo usa
     * @return MULTIPLICADOR_STAB si coinciden los tipos, 1.0 si no
     */
    private double factorStab(Movimiento mov, Criatura quien) {
        if (mov.getTipo() == quien.getTipo()) {
            return MULTIPLICADOR_STAB;
        } else {
            return 1.0;
        }
    }

    /**
     * Resuelve un ataque y añade los mensajes a la lista.
     *
     * @param atacante  quien ataca
     * @param defensor  quien recibe
     * @param mov       movimiento usado
     * @param esJugador true si ataca el jugador (para los textos y la sacudida)
     * @param eventos   lista donde añado los eventos
     */
    private void atacar(Criatura atacante, Criatura defensor, Movimiento mov, boolean esJugador, ArrayList<Evento> eventos) {
        // al rival le pongo "enemigo" para distinguirlo si los dos usan la misma especie
        String nombreAtacante;
        String nombreDefensor;
        int bando;          // de quién hablan los mensajes de este ataque
        int bandoDefensor;
        if (esJugador) {
            nombreAtacante = atacante.getNombre();
            nombreDefensor = defensor.getNombre() + " enemigo";
            bando = Evento.JUGADOR;
            bandoDefensor = Evento.RIVAL;
        } else {
            nombreAtacante = atacante.getNombre() + " enemigo";
            nombreDefensor = defensor.getNombre();
            bando = Evento.RIVAL;
            bandoDefensor = Evento.JUGADOR;
        }

        // comparo referencias con == porque el Forcejeo es un único objeto compartido por los dos bandos
        if (mov == forcejeo) {
            eventos.add(crearEvento("¡A " + nombreAtacante + " no le quedan PP! ¡Usa Forcejeo!", Evento.NADA, bando));
        } else {
            mov.gastarPP(); // se gasta aunque luego falle
            eventos.add(crearEvento("¡" + nombreAtacante + " usa " + mov.getNombre() + "!", Evento.NADA, bando));
        }

        // curación: recupera la mitad de la vida máxima y termina
        if (mov.esCuracion()) {
            int curado = atacante.curar(atacante.getHpMax() / DIVISOR_CURACION);
            if (curado > 0) {
                eventos.add(crearEvento(nombreAtacante + " recupera " + curado + " PS.", Evento.REFRESCO, bando));
            } else {
                eventos.add(crearEvento("Pero no tiene efecto...", Evento.NADA, bando));
            }
            return;
        }

        // tirada de precisión: nextInt(100) da de 0 a 99, con precisión 100 nunca falla
        int tirada = rng.nextInt(100);
        if (tirada >= mov.getPrecision()) {
            eventos.add(crearEvento("¡Pero el ataque falló!", Evento.NADA, bando));
            return;
        }

        // ---- cálculo del daño ----
        double efectividad = mov.getTipo().multiplicadorContra(defensor.getTipo());

        // STAB: mismo tipo que la criatura = 50% más
        double stab = factorStab(mov, atacante);

        // crítico 1 de cada 12
        boolean critico = false;
        if (rng.nextInt(UNO_ENTRE_CRITICO) == 0) {
            critico = true;
        }

        // variación aleatoria del 85% al 100%
        double variacion = VARIACION_MINIMA + rng.nextDouble() * RANGO_VARIACION;

        // fórmula clásica de Pokémon; trabajo en double y redondeo al final.
        // Escribo 2.0 y 5.0 con decimales para que la división sea en double y no entera.
        // factorNivel: a nivel 50 sale 22; es lo que hace que el daño crezca con el nivel
        double factorNivel = 2.0 * atacante.getNivel() / 5.0 + 2.0;
        // base: potencia del movimiento escalada por mi ataque frente a su defensa,
        // dividido entre 50 para dejarlo en una escala razonable y +2 para que nunca sea 0
        double base = (factorNivel * mov.getPotencia() * atacante.getAtaque() / defensor.getDefensa()) / 50.0 + 2.0;

        double multCritico;
        if (critico) {
            multCritico = MULTIPLICADOR_CRITICO;
        } else {
            multCritico = 1.0;
        }

        int dano = (int) (base * stab * efectividad * variacion * multCritico);
        if (dano < 1) {
            dano = 1; // mínimo 1 para que no salga "pierde 0 PS"
        }

        defensor.recibirDano(dano);
        if (esJugador) {
            eventos.add(crearEvento(nombreDefensor + " pierde " + dano + " PS.", Evento.GOLPE_RIVAL, bando));
        } else {
            eventos.add(crearEvento(nombreDefensor + " pierde " + dano + " PS.", Evento.GOLPE_JUGADOR, bando));
        }

        // los avisos van después del daño
        if (critico) {
            eventos.add(crearEvento("¡Golpe crítico!", Evento.NADA, bando));
        }
        if (efectividad > 1.0) {
            eventos.add(crearEvento("¡Es muy eficaz!", Evento.NADA, bando));
        } else if (efectividad < 1.0) {
            eventos.add(crearEvento("No es muy eficaz...", Evento.NADA, bando));
        }

        if (defensor.estaDebilitada()) {
            eventos.add(crearEvento("¡" + nombreDefensor + " se debilitó!", Evento.REFRESCO, bandoDefensor));
        }
    }

    /**
     * Si la criatura del rival ha caído y le quedan más, saca la que mejor
     * le viene por tipo contra la mía.
     *
     * @param eventos lista donde añado el mensaje de entrada de la nueva criatura
     */
    private void reemplazoRivalSiHaceFalta(ArrayList<Evento> eventos) {
        if (!activaRival.estaDebilitada() || rivalDerrotado()) {
            return;
        }

        // búsqueda del máximo; empiezo en -1 para que la primera viva siempre entre
        Criatura mejor = null;
        double mejorPuntaje = -1;
        for (int i = 0; i < equipoRival.size(); i++) {
            Criatura c = equipoRival.get(i);
            if (c.estaDebilitada()) {
                continue;
            }
            // lo que le pega ella a mi criatura / lo que le pego yo a ella
            double puntaje = c.getTipo().multiplicadorContra(activaJugador.getTipo())
                    / activaJugador.getTipo().multiplicadorContra(c.getTipo());
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = c;
            }
        }

        if (mejor != null) {
            activaRival = mejor;
            eventos.add(crearEvento("¡El rival envía a " + mejor.getNombre() + "!", Evento.REFRESCO, Evento.RIVAL));
        }
    }

    /**
     * IA del rival, por prioridades:
     * 1) con menos del 35% de vida, el 55% de las veces se cura si puede;
     * 2) el 20% de las veces elige al azar;
     * 3) si no, el ataque con más daño esperado.
     *
     * @return el movimiento que usará el rival este turno (Forcejeo si no le quedan PP)
     */
    private Movimiento elegirMovimientoIA() {
        // solo los que tienen PP
        ArrayList<Movimiento> disponibles = new ArrayList<Movimiento>();
        for (int i = 0; i < activaRival.getMovimientos().size(); i++) {
            Movimiento m = activaRival.getMovimientos().get(i);
            if (m.tienePP()) {
                disponibles.add(m);
            }
        }
        if (disponibles.isEmpty()) {
            return forcejeo;
        }

        // 1) curarse si está baja de vida
        if (activaRival.fraccionVida() < UMBRAL_VIDA_BAJA) {
            if (rng.nextInt(100) < PROB_CURARSE) {
                for (int i = 0; i < disponibles.size(); i++) {
                    if (disponibles.get(i).esCuracion()) {
                        return disponibles.get(i);
                    }
                }
            }
        }

        // 2) un poco de aleatoriedad
        if (rng.nextInt(100) < PROB_AZAR_IA) {
            return disponibles.get(rng.nextInt(disponibles.size()));
        }

        // 3) el mejor ataque (empiezo con el primero para no devolver null)
        Movimiento mejor = disponibles.get(0);
        double mejorEsperado = -1;
        for (int i = 0; i < disponibles.size(); i++) {
            Movimiento m = disponibles.get(i);
            if (m.esCuracion()) {
                continue;
            }
            double stab = factorStab(m, activaRival);
            // daño esperado aproximado: potencia x stab x efectividad x precisión
            double esperado = m.getPotencia() * stab
                    * m.getTipo().multiplicadorContra(activaJugador.getTipo())
                    * (m.getPrecision() / 100.0);
            if (esperado > mejorEsperado) {
                mejorEsperado = esperado;
                mejor = m;
            }
        }
        return mejor;
    }
}
