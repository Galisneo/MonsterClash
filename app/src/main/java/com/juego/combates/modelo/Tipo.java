/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Tipo.java
 */
package com.juego.combates.modelo;

/**
 * Enumerado con los 6 tipos elementales del juego.
 * Cada tipo tiene un nombre para mostrar, un color para dibujar la criatura
 * y un icono (emoji) para las etiquetas de la interfaz.
 * También tiene la tabla de efectividades (qué tipo es fuerte contra cuál).
 *
 * Uso un enum porque los tipos son fijos y así no puedo escribir uno que no existe.
 */
public enum Tipo {

    // El color va en formato 0xAARRGGBB (AA = transparencia, FF es opaco)
    NORMAL("Normal", 0xFF9AA0A6, "⚪"),
    FUEGO("Fuego", 0xFFE25822, "🔥"),
    AGUA("Agua", 0xFF3B82F6, "💧"),
    PLANTA("Planta", 0xFF34A853, "🌿"),
    ELECTRICO("Eléctrico", 0xFFF0A500, "⚡"),
    ROCA("Roca", 0xFF8D6E63, "⛰️");

    // final porque los datos de un tipo no cambian nunca
    private final String nombre;
    private final int color;
    private final String icono;

    /**
     * Tabla de efectividades. Se lee TABLA[atacante][defensor].
     * 2.0 = muy eficaz, 1.0 = normal, 0.5 = poco eficaz.
     * El orden de filas y columnas es el de indice(): NORMAL, FUEGO, AGUA, PLANTA, ELECTRICO, ROCA.
     */
    private static final double[][] TABLA = {
            //           NOR  FUE  AGU  PLA  ELE  ROC
            /* NORMAL */ {1.0, 1.0, 1.0, 1.0, 1.0, 0.5},
            /* FUEGO  */ {1.0, 0.5, 0.5, 2.0, 1.0, 0.5},
            /* AGUA   */ {1.0, 2.0, 0.5, 0.5, 1.0, 2.0},
            /* PLANTA */ {1.0, 0.5, 2.0, 0.5, 1.0, 2.0},
            /* ELECTR */ {1.0, 1.0, 2.0, 0.5, 0.5, 1.0},
            /* ROCA   */ {1.0, 2.0, 1.0, 1.0, 1.0, 0.5}
    };

    Tipo(String nombre, int color, String icono) {
        this.nombre = nombre;
        this.color = color;
        this.icono = icono;
    }

    // ---- Getters ----

    /**
     * @return el nombre del tipo tal y como se enseña en pantalla (con tilde si la lleva)
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return el color del tipo en formato 0xAARRGGBB, listo para pintar la criatura
     */
    public int getColor() {
        return color;
    }

    /**
     * @return el emoji del tipo, para los botones y las etiquetas
     */
    public String getIcono() {
        return icono;
    }

    /**
     * Devuelve la posición de este tipo en la tabla.
     * Lo hago con un switch para no depender del orden en que declaro las constantes.
     *
     * @return número de fila/columna en TABLA (de 0 a 5)
     */
    private int indice() {
        switch (this) {
            case NORMAL:
                return 0;
            case FUEGO:
                return 1;
            case AGUA:
                return 2;
            case PLANTA:
                return 3;
            case ELECTRICO:
                return 4;
            case ROCA:
                return 5;
            default:
                return 0;
        }
    }

    /**
     * Calcula el multiplicador de daño de este tipo atacando al tipo defensor.
     *
     * @param defensor tipo de la criatura que recibe el ataque
     * @return 2.0, 1.0 o 0.5 según la tabla
     */
    public double multiplicadorContra(Tipo defensor) {
        return TABLA[this.indice()][defensor.indice()];
    }
}
