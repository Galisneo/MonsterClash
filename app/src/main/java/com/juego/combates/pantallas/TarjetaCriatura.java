/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  TarjetaCriatura.java
 */
package com.juego.combates.pantallas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import com.juego.combates.R;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.vistas.Paleta;
import com.juego.combates.vistas.VistaCriatura;

/**
 * La cabecera que comparten la tarjeta de selección (item_criatura.xml) y la
 * del diálogo de equipo (item_equipo_dialogo.xml): el dibujo de la criatura,
 * el círculo de fondo del color de su tipo, el nombre y el chip del tipo. Los
 * dos layouts usan los mismos ids para esas cuatro vistas, así que un solo
 * método vale para ambos; cada pantalla añade después lo suyo (las barras de
 * estadísticas en Selección, la barra de vida y el estado en Combate).
 *
 * Todo es estático: la clase no se instancia, por eso el constructor es privado.
 */
public final class TarjetaCriatura {

    /**
     * Constructor privado: solo hay métodos estáticos.
     */
    private TarjetaCriatura() {
    }

    /**
     * Rellena dibujo, fondo, nombre y chip del tipo de una tarjeta ya inflada.
     * Busco las vistas con item.findViewById porque los ids se repiten en
     * todas las tarjetas de la misma pantalla.
     *
     * @param item vista inflada de item_criatura.xml o item_equipo_dialogo.xml
     * @param c    criatura a mostrar
     */
    public static void rellenarCabecera(View item, Criatura c) {
        Context contexto = item.getContext();
        int color = c.getTipo().getColor();

        VistaCriatura vistaCriatura = item.findViewById(R.id.vista_criatura);
        vistaCriatura.setCriatura(c);

        // círculo de fondo del color del tipo
        View fondo = item.findViewById(R.id.fondo_criatura);
        fondo.setBackgroundTintList(ColorStateList.valueOf(color));

        TextView nombre = item.findViewById(R.id.nombre);
        nombre.setText(c.getNombre());

        // chip del tipo: emoji + nombre, con el fondo del color del tipo y el
        // texto oscuro o claro según lo que se lea mejor encima
        TextView tipo = item.findViewById(R.id.tipo);
        tipo.setText(contexto.getString(R.string.etiqueta_tipo, c.getTipo().getIcono(), c.getTipo().getNombre()));
        tipo.setBackgroundTintList(ColorStateList.valueOf(color));
        tipo.setTextColor(Paleta.textoSobre(color));
    }
}
