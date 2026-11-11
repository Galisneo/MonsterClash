/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  EnvioConTeclado.java
 */
package com.juego.combates.pantallas;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

/**
 * El botón "hecho" del teclado equivale a pulsar el botón principal del
 * formulario. Este listener se pone en los EditText del login y de recuperar
 * contraseña: cuando llega IME_ACTION_DONE ejecuta la acción que le pasaron
 * (enviar el formulario o el botón de acción del paso) y se queda el evento;
 * el resto de acciones del teclado (siguiente, etc.) las deja pasar.
 */
public class EnvioConTeclado implements TextView.OnEditorActionListener {

    private final Runnable accion;

    /**
     * @param accion lo que hay que ejecutar cuando el usuario pulsa "hecho"
     */
    public EnvioConTeclado(Runnable accion) {
        this.accion = accion;
    }

    /**
     * @param v        el EditText donde se pulsó el botón del teclado
     * @param actionId acción del teclado (EditorInfo.IME_ACTION_*)
     * @param event    tecla física, si la hubo (no la uso)
     * @return true si era "hecho" y ya lo he atendido, false para que siga el comportamiento normal
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            accion.run();
            return true;
        }
        return false;
    }
}
