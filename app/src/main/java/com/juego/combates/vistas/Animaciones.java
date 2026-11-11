/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Animaciones.java
 */
package com.juego.combates.vistas;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Las dos animaciones pequeñas que repiten todas las pantallas: el fundido de
 * entrada de botones y tarjetas (aparecer) y el flotar arriba y abajo de las
 * criaturas de adorno (flotar). Así las cuatro Activities comparten el mismo
 * código y cada pantalla solo pasa sus valores (altura, duración y retardo).
 *
 * Todo es estático: la clase no se instancia, por eso el constructor es privado.
 */
public final class Animaciones {

    /**
     * Constructor privado: solo hay métodos estáticos y no quiero que nadie
     * haga new Animaciones() por error.
     */
    private Animaciones() {
    }

    /**
     * Fundido de entrada: la vista empieza transparente y un poco más abajo, y
     * sube hasta su sitio mientras se vuelve opaca.
     *
     * @param v        vista a animar
     * @param dpSubida desde cuántos dp más abajo sube
     * @param retardo  ms de espera antes de empezar (para encadenar varias)
     * @param duracion ms que dura el fundido
     */
    public static void aparecer(View v, float dpSubida, long retardo, long duracion) {
        float densidad = v.getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(dpSubida * densidad);
        v.animate().alpha(1f).translationY(0f).setStartDelay(retardo).setDuration(duracion).start();
    }

    /**
     * Flotar sin parar: sube dpAltura, vuelve a su sitio y repite
     * (ObjectAnimator infinito sobre translationY). Devuelvo el animador para
     * que la pantalla pueda pausarlo, reanudarlo y cancelarlo en onDestroy.
     *
     * @param v        vista a animar
     * @param dpAltura cuántos dp sube en cada ciclo (positivo)
     * @param duracion ms de un ciclo completo (subir y bajar)
     * @param retardo  ms antes de empezar, para desfasar varias criaturas
     * @return el animador, ya en marcha
     */
    public static ObjectAnimator flotar(View v, float dpAltura, long duracion, long retardo) {
        float densidad = v.getResources().getDisplayMetrics().density;
        // translationY negativa = hacia arriba
        float alturaPx = -dpAltura * densidad;
        ObjectAnimator animador = ObjectAnimator.ofFloat(v, "translationY", 0f, alturaPx, 0f);
        animador.setDuration(duracion);
        animador.setStartDelay(retardo);
        animador.setRepeatCount(ValueAnimator.INFINITE);
        animador.setInterpolator(new AccelerateDecelerateInterpolator());
        animador.start();
        return animador;
    }
}
