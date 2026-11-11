/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Transiciones.java
 */
package com.juego.combates.pantallas;

import android.app.Activity;

import com.juego.combates.R;

/**
 * Las dos transiciones entre pantallas de la app, con las animaciones de
 * res/anim: "adelante" (la nueva entra por la derecha) y "atrás" (entra por la
 * izquierda). Se llaman justo después de startActivity() o antes de finish().
 *
 * overridePendingTransition está marcado como obsoleto desde la API 34 en
 * favor de overrideActivityTransition, pero esa solo existe desde la 34 y mi
 * minSdk es 24: con la antigua funciona igual en todos los móviles que
 * soporto, así que silencio el aviso una sola vez aquí, para toda la clase,
 * en vez de repetirlo en cada Activity.
 */
@SuppressWarnings("deprecation")
public final class Transiciones {

    /**
     * Constructor privado: solo hay métodos estáticos.
     */
    private Transiciones() {
    }

    /**
     * Ir hacia delante: la pantalla nueva entra desde la derecha y la actual
     * sale hacia la izquierda.
     *
     * @param actividad la pantalla que acaba de llamar a startActivity
     */
    public static void adelante(Activity actividad) {
        actividad.overridePendingTransition(R.anim.entrar_desde_derecha, R.anim.salir_a_izquierda);
    }

    /**
     * Volver: la pantalla de destino entra desde la izquierda y la actual sale
     * hacia la derecha.
     *
     * @param actividad la pantalla que vuelve (menú, login...)
     */
    public static void atras(Activity actividad) {
        actividad.overridePendingTransition(R.anim.entrar_desde_izquierda, R.anim.salir_a_derecha);
    }
}
