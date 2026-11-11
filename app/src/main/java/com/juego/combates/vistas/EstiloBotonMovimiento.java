/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  EstiloBotonMovimiento.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.Button;

import com.juego.combates.R;
import com.juego.combates.modelo.Tipo;

/**
 * Pinta los cuatro botones de movimiento del combate con la paleta de su tipo
 * (luz arriba, color de botón abajo, borde de sombra y onda al pulsar) y les
 * pone el texto en dos líneas: nombre en negrita y tipo/PP más pequeño.
 *
 * El fondo lo creo desde código con GradientDrawable y RippleDrawable en vez
 * de un drawable XML por tipo: son seis paletas y cambian con el movimiento
 * que toque en cada turno. Las medidas salen de dimens.xml y los colores fijos
 * de colors.xml; los de cada tipo, de Paleta.
 */
public class EstiloBotonMovimiento {

    /** La línea de tipo y PP va a este tamaño respecto al nombre. */
    private static final float ESCALA_DETALLE = 0.86f;

    private final Context contexto;
    private final int radioPx;   // esquinas redondeadas
    private final int bordePx;   // grosor del borde
    private final int colorRipple;

    /**
     * @param contexto para leer dimens.xml y colors.xml
     */
    public EstiloBotonMovimiento(Context contexto) {
        this.contexto = contexto;
        this.radioPx = contexto.getResources().getDimensionPixelSize(R.dimen.radio_boton_mov);
        this.bordePx = contexto.getResources().getDimensionPixelSize(R.dimen.borde_boton_mov);
        this.colorRipple = contexto.getColor(R.color.ripple_boton);
    }

    /**
     * Pinta un botón de movimiento con la paleta de su tipo (luz arriba, color
     * de botón abajo, borde de sombra).
     *
     * @param b    botón a pintar
     * @param tipo tipo del movimiento
     */
    public void estilizarBoton(Button b, Tipo tipo) {
        int base = Paleta.colorBoton(tipo);
        int texto = Paleta.textoSobre(base);
        int luz = Paleta.luz(tipo);
        if (tipo == Tipo.NORMAL) {
            // la luz de Normal es demasiado clara sobre el azul pizarra del botón
            luz = contexto.getColor(R.color.boton_normal_luz);
        }
        estilizarBotonConColores(b, luz, base, Paleta.sombra(tipo), texto);
    }

    /**
     * Botón de un movimiento sin PP: grises de colors.xml para que se vea apagado pero legible.
     *
     * @param b botón a pintar
     */
    public void estilizarBotonApagado(Button b) {
        estilizarBotonConColores(b,
                contexto.getColor(R.color.boton_apagado_luz),
                contexto.getColor(R.color.boton_gris),
                contexto.getColor(R.color.boton_apagado_borde),
                contexto.getColor(R.color.texto_tenue));
    }

    /**
     * Crea el fondo del botón desde código para poder usar una paleta distinta
     * por tipo sin añadir drawables repetidos.
     *
     * @param b      botón a pintar
     * @param arriba color de la parte superior
     * @param abajo  color principal
     * @param borde  color del borde
     * @param texto  color del texto
     */
    private void estilizarBotonConColores(Button b, int arriba, int abajo, int borde, int texto) {
        int[] colores = new int[]{arriba, abajo};
        GradientDrawable fondo = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colores);
        fondo.setCornerRadius(radioPx);
        fondo.setStroke(bordePx, borde);

        // la máscara marca hasta dónde llega la onda: el mismo rectángulo redondeado
        GradientDrawable mascara = new GradientDrawable();
        mascara.setColor(Color.WHITE);
        mascara.setCornerRadius(radioPx);

        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(colorRipple), fondo, mascara);
        // Quito el tint del XML para que no apague el gradiente del tipo.
        b.setBackgroundTintList(null);
        b.setBackground(ripple);
        b.setTextColor(texto);
        b.setIncludeFontPadding(false);
    }

    /**
     * Da jerarquía al texto del movimiento: nombre fuerte y PP más pequeño. El
     * color principal lo leo del propio botón, que ya lo tiene puesto por
     * estilizarBoton o estilizarBotonApagado; la segunda línea va en el mismo
     * tono pero al 80 %.
     *
     * @param b       botón que recibe el texto, ya estilizado
     * @param titulo  nombre con icono
     * @param detalle tipo y PP
     */
    public void ponerTextoMovimiento(Button b, String titulo, String detalle) {
        int colorPrincipal = b.getCurrentTextColor();
        SpannableStringBuilder texto = new SpannableStringBuilder();
        texto.append(titulo);
        texto.append("\n");
        int inicioDetalle = texto.length();
        texto.append(detalle);
        texto.setSpan(new StyleSpan(Typeface.BOLD), 0, titulo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new RelativeSizeSpan(ESCALA_DETALLE), inicioDetalle, texto.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new ForegroundColorSpan(colorSecundario(colorPrincipal)), inicioDetalle, texto.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setText(texto);
    }

    /**
     * @param colorPrincipal color del texto del botón (oscuro o claro según Paleta.textoSobre)
     * @return el mismo tono al 80 % para la línea de tipo y PP
     */
    private int colorSecundario(int colorPrincipal) {
        // Paleta.textoSobre solo devuelve TEXTO_OSCURO o TEXTO_CLARO, así que
        // basta con mirar si es el oscuro; el resto (incluido el gris del botón
        // apagado) lleva la variante clara
        if (colorPrincipal == Paleta.TEXTO_OSCURO) {
            return contexto.getColor(R.color.texto_secundario_oscuro);
        }
        return contexto.getColor(R.color.texto_secundario_claro);
    }
}
