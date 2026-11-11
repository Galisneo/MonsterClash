/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  ChatCombate.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.juego.combates.R;
import com.juego.combates.batalla.Evento;

/**
 * El historial del combate enseñado como un chat: cada mensaje es una burbuja
 * dentro de un LinearLayout vertical que va dentro de un ScrollView. A la
 * izquierda las de mi criatura, a la derecha las del rival y centradas, en
 * cursiva y más discretas, las del narrador.
 *
 * CombateActivity me pasa las dos vistas del layout y me llama con cada
 * mensaje; yo creo la burbuja, la animo y bajo el scroll.
 */
public class ChatCombate {

    /** Burbujas que guardo como mucho; la más antigua se quita al pasar de aquí. */
    private static final int MAX_MENSAJES = 40;

    /** Opacidad de las burbujas anteriores, para que la última destaque. */
    private static final float ALFA_ANTERIORES = 0.7f;
    /** Duración del fundido de entrada de una burbuja. */
    private static final long DURACION_ENTRADA_MS = 220;

    private final Context contexto;
    private final LinearLayout listaMensajes;
    private final ScrollView scroll;

    // medidas de dimens.xml ya pasadas a píxeles, para no leerlas en cada burbuja
    private final int paddingLateral;
    private final int paddingVertical;
    private final int margenLateral;
    private final int margenVertical;
    private final int anchoMaximo;
    private final int subidaEntrada;
    private final float tamanoTexto;
    private final float tamanoTextoNarrador;

    /**
     * @param contexto      para crear los TextView y leer recursos
     * @param listaMensajes el LinearLayout vertical donde van las burbujas
     * @param scroll        el ScrollView que lo contiene, para bajarlo al final
     */
    public ChatCombate(Context contexto, LinearLayout listaMensajes, ScrollView scroll) {
        this.contexto = contexto;
        this.listaMensajes = listaMensajes;
        this.scroll = scroll;
        this.paddingLateral = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_padding_lateral);
        this.paddingVertical = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_padding_vertical);
        this.margenLateral = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_margen_lateral);
        this.margenVertical = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_margen_vertical);
        this.anchoMaximo = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_ancho_maximo);
        this.subidaEntrada = contexto.getResources().getDimensionPixelSize(R.dimen.chat_burbuja_subida);
        // getDimension ya devuelve los sp pasados a píxeles, por eso luego uso COMPLEX_UNIT_PX
        this.tamanoTexto = contexto.getResources().getDimension(R.dimen.texto_normal);
        this.tamanoTextoNarrador = contexto.getResources().getDimension(R.dimen.texto_ayuda);
    }

    /**
     * Añade una burbuja al chat y baja el scroll. Las burbujas anteriores se
     * atenúan un poco para que la última destaque. El scroll va en un post()
     * porque si no el ScrollView aún no se ha enterado de la burbuja nueva.
     *
     * @param texto mensaje a añadir
     * @param bando Evento.JUGADOR (izquierda), Evento.RIVAL (derecha) o NEUTRAL (centro)
     */
    public void agregar(String texto, int bando) {
        // atenúo las anteriores
        for (int i = 0; i < listaMensajes.getChildCount(); i++) {
            listaMensajes.getChildAt(i).setAlpha(ALFA_ANTERIORES);
        }

        TextView burbuja = new TextView(contexto);
        burbuja.setText(texto);
        burbuja.setTextColor(contexto.getColor(R.color.texto_claro));
        burbuja.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanoTexto);
        burbuja.setPadding(paddingLateral, paddingVertical, paddingLateral, paddingVertical);
        burbuja.setMaxWidth(anchoMaximo);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(margenLateral, margenVertical, margenLateral, margenVertical);

        if (bando == Evento.JUGADOR) {
            lp.gravity = Gravity.START;
            burbuja.setBackgroundResource(R.drawable.burbuja_jugador);
        } else if (bando == Evento.RIVAL) {
            lp.gravity = Gravity.END;
            burbuja.setBackgroundResource(R.drawable.burbuja_rival);
        } else {
            // narrador: centrado, en cursiva y más discreto
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            burbuja.setBackgroundResource(R.drawable.burbuja_neutral);
            burbuja.setTypeface(null, Typeface.ITALIC);
            burbuja.setTextColor(contexto.getColor(R.color.texto_tenue));
            burbuja.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanoTextoNarrador);
        }

        listaMensajes.addView(burbuja, lp);
        if (listaMensajes.getChildCount() > MAX_MENSAJES) {
            listaMensajes.removeViewAt(0); // quito la más antigua
        }

        // entra con un fundido subiendo un poco
        burbuja.setAlpha(0f);
        burbuja.setTranslationY(subidaEntrada);
        burbuja.animate().alpha(1f).translationY(0f).setDuration(DURACION_ENTRADA_MS).start();

        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * Deja el chat en blanco (al empezar otra batalla).
     */
    public void limpiar() {
        listaMensajes.removeAllViews();
    }
}
