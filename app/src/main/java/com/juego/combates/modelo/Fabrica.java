/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Fabrica.java
 */
package com.juego.combates.modelo;

import java.util.ArrayList;
import java.util.Random;

/**
 * Clase con métodos estáticos para crear las criaturas del juego.
 * Aquí están las 6 especies con sus estadísticas y sus movimientos,
 * así tengo todos los números del juego en un mismo sitio.
 *
 * crear() devuelve siempre un objeto nuevo (con vida y PP llenos).
 */
public class Fabrica {

    /** Criaturas por equipo. */
    public static final int TAMANO_EQUIPO = 3;

    /**
     * Nombres de las especies. La pantalla de selección hace una tarjeta por
     * cada uno, y vistas/DibujoCriatura elige la silueta comparando el nombre
     * de la criatura carácter a carácter con estos mismos textos: si cambio
     * uno aquí hay que cambiarlo también allí (y en el case de crear()).
     */
    private static final String[] ESPECIES = {
            "Fogarto", "Acuarin", "Hojaruz", "Chispin", "Rocardo", "Normalino"
    };

    /**
     * Constructor privado: es una clase de utilidades con métodos estáticos,
     * así que no tiene sentido crear objetos Fabrica.
     */
    private Fabrica() {
    }

    /**
     * @return los nombres de todas las especies
     */
    public static String[] getEspecies() {
        return ESPECIES;
    }

    /**
     * Crea una criatura nueva según el nombre de la especie.
     * Constructor de Criatura: nombre, tipo, nivel, vida, ataque, defensa, velocidad.
     * Constructor de Movimiento (ataque): nombre, tipo, potencia, precisión, PP.
     *
     * @param especie nombre de la especie
     * @return la criatura creada (Normalino si el nombre no existe)
     */
    public static Criatura crear(String especie) {
        Criatura c;

        switch (especie) {
            case "Fogarto":
                // rápido y pega bien, pero poca vida
                c = new Criatura("Fogarto", Tipo.FUEGO, 50, 175, 85, 70, 90);
                c.agregarMovimiento(new Movimiento("Ascuas", Tipo.FUEGO, 40, 100, 25));
                c.agregarMovimiento(new Movimiento("Llamarada", Tipo.FUEGO, 95, 85, 10));
                c.agregarMovimiento(new Movimiento("Mordisco", Tipo.NORMAL, 60, 95, 20));
                c.agregarMovimiento(new Movimiento("Placaje", Tipo.NORMAL, 40, 100, 35));
                break;

            case "Acuarin":
                // equilibrado, con curación
                c = new Criatura("Acuarin", Tipo.AGUA, 50, 185, 78, 80, 75);
                c.agregarMovimiento(new Movimiento("Pistola Agua", Tipo.AGUA, 40, 100, 25));
                c.agregarMovimiento(new Movimiento("Hidrobomba", Tipo.AGUA, 105, 80, 8));
                c.agregarMovimiento(new Movimiento("Placaje", Tipo.NORMAL, 40, 100, 35));
                c.agregarMovimiento(new Movimiento("Recuperación", Tipo.NORMAL, 8));
                break;

            case "Hojaruz":
                // mucha vida y defensa, lento, con curación
                c = new Criatura("Hojaruz", Tipo.PLANTA, 50, 190, 75, 85, 65);
                c.agregarMovimiento(new Movimiento("Hoja Afilada", Tipo.PLANTA, 55, 95, 25));
                c.agregarMovimiento(new Movimiento("Rayo Vegetal", Tipo.PLANTA, 110, 80, 8));
                c.agregarMovimiento(new Movimiento("Placaje", Tipo.NORMAL, 40, 100, 35));
                c.agregarMovimiento(new Movimiento("Síntesis", Tipo.PLANTA, 8));
                break;

            case "Chispin":
                // el más rápido, el que menos vida tiene
                c = new Criatura("Chispin", Tipo.ELECTRICO, 50, 165, 82, 65, 105);
                c.agregarMovimiento(new Movimiento("Impactrueno", Tipo.ELECTRICO, 40, 100, 25));
                c.agregarMovimiento(new Movimiento("Trueno", Tipo.ELECTRICO, 105, 70, 10));
                c.agregarMovimiento(new Movimiento("Ataque Rápido", Tipo.NORMAL, 40, 100, 30));
                c.agregarMovimiento(new Movimiento("Mordisco", Tipo.NORMAL, 60, 95, 20));
                break;

            case "Rocardo":
                // el tanque: mucho ataque y defensa, muy lento
                c = new Criatura("Rocardo", Tipo.ROCA, 50, 200, 90, 100, 45);
                c.agregarMovimiento(new Movimiento("Lanzarrocas", Tipo.ROCA, 50, 90, 20));
                c.agregarMovimiento(new Movimiento("Avalancha", Tipo.ROCA, 80, 90, 10));
                c.agregarMovimiento(new Movimiento("Placaje", Tipo.NORMAL, 40, 100, 35));
                c.agregarMovimiento(new Movimiento("Descanso", Tipo.NORMAL, 6));
                break;

            case "Normalino":
            default:
                // Normalino también es el default: si me equivoco con un nombre no devuelvo null
                c = new Criatura("Normalino", Tipo.NORMAL, 50, 210, 80, 75, 70);
                c.agregarMovimiento(new Movimiento("Placaje", Tipo.NORMAL, 40, 100, 35));
                c.agregarMovimiento(new Movimiento("Golpe Cuerpo", Tipo.NORMAL, 85, 100, 12));
                c.agregarMovimiento(new Movimiento("Mordisco", Tipo.NORMAL, 60, 95, 20));
                c.agregarMovimiento(new Movimiento("Recuperación", Tipo.NORMAL, 8));
                break;
        }

        return c;
    }

    /**
     * Crea un equipo de 3 criaturas al azar sin repetir especie.
     * Uso un array de booleanos para marcar las especies que ya he cogido.
     *
     * @param rng generador de números aleatorios (el mismo para toda la partida)
     * @return lista con las 3 criaturas
     */
    public static ArrayList<Criatura> equipoAleatorio(Random rng) {
        ArrayList<Criatura> equipo = new ArrayList<Criatura>();
        boolean[] usada = new boolean[ESPECIES.length];

        while (equipo.size() < TAMANO_EQUIPO) {
            int indice = rng.nextInt(ESPECIES.length);
            if (!usada[indice]) {
                usada[indice] = true;
                equipo.add(crear(ESPECIES[indice]));
            }
        }

        return equipo;
    }
}
