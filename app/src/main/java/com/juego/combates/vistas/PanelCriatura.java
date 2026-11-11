/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  PanelCriatura.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.juego.combates.R;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Tipo;

import java.util.ArrayList;

/**
 * Panel oscuro con nombre, nivel, barra de vida y puntos del equipo de una
 * criatura. En VistaBatalla hay dos: el del rival (sin números) y el del
 * jugador (con los PS en texto).
 *
 * Guardo en caché los textos formateados (nivel y PS) porque getString con
 * formato crea objetos y onDraw corre a 30 fps: solo los recalculo cuando
 * cambia la criatura o el entero de la vida.
 */
public class PanelCriatura {

    /** Proporciones del panel respecto a su ancho y a su alto. */
    private static final float PROPORCION_ALTO = 0.42f;
    private static final float PROPORCION_RADIO = 0.22f;
    /**
     * Umbrales de la barra de vida: verde por encima del 50 %, rojo por debajo
     * del 20 %. Son públicos porque la barra del diálogo de equipo
     * (CombateActivity.colorVida) usa los mismos cortes.
     */
    public static final float UMBRAL_VIDA_ALTA = 0.5f;
    public static final float UMBRAL_VIDA_BAJA = 0.2f;

    private Context contexto;
    private boolean mostrarNumeros;   // true en el panel del jugador (enseña "N / M PS")
    private Paint paint;
    private Paint paintTexto;
    private RectF rect;

    // colores leídos de colors.xml una vez
    private int colorSombra;
    private int colorFondo;
    private int colorNombre;
    private int colorNivel;
    private int colorBarraFondo;
    private int colorBarraFantasma;
    private int colorPs;
    private int colorPuntoCaido;
    private int colorVidaAlta;
    private int colorVidaMedia;
    private int colorVidaBaja;

    // caché de textos formateados
    private String textoNivel = "";
    private String textoPs = "";
    private int ultimoHpEntero = -1;

    /**
     * Prepara pinceles y lee los colores del panel.
     *
     * @param contexto       contexto para colors.xml y strings.xml
     * @param mostrarNumeros true para enseñar los PS en texto (panel del jugador)
     */
    public PanelCriatura(Context contexto, boolean mostrarNumeros) {
        this.contexto = contexto;
        this.mostrarNumeros = mostrarNumeros;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();

        colorSombra = contexto.getColor(R.color.panel_combate_sombra);
        colorFondo = contexto.getColor(R.color.panel_combate_fondo);
        colorNombre = contexto.getColor(R.color.texto_blanco);
        colorNivel = contexto.getColor(R.color.texto_tenue);
        colorBarraFondo = contexto.getColor(R.color.barra_fondo);
        colorBarraFantasma = contexto.getColor(R.color.barra_fantasma);
        colorPs = contexto.getColor(R.color.texto_claro);
        colorPuntoCaido = contexto.getColor(R.color.slot_borde);
        colorVidaAlta = contexto.getColor(R.color.boton_verde);
        colorVidaMedia = contexto.getColor(R.color.barra_vida_media);
        colorVidaBaja = contexto.getColor(R.color.boton_rojo);
    }

    /**
     * Cambia la criatura del panel: recalcula el texto del nivel y obliga a
     * volver a formatear los PS en el siguiente fotograma.
     *
     * @param criatura criatura nueva (null deja los textos como están)
     */
    public void setCriatura(Criatura criatura) {
        if (criatura != null) {
            textoNivel = contexto.getString(R.string.combate_panel_nivel, criatura.getNivel());
        }
        ultimoHpEntero = -1;
    }

    /**
     * Dibuja el panel completo en la posición indicada.
     *
     * @param canvas     dónde dibujar
     * @param criatura   criatura que se está enseñando (la "foto" del evento)
     * @param hpMostrado vida interpolada que persigue a la real
     * @param hpFantasma vida blanca que se queda un instante detrás al bajar
     * @param equipo     equipo completo, para los puntos de criaturas restantes
     * @param x          esquina izquierda del panel
     * @param y          esquina superior del panel
     * @param ancho      ancho del panel (el alto sale de la proporción)
     */
    public void dibujarPanel(Canvas canvas, Criatura criatura, float hpMostrado, float hpFantasma, ArrayList<Criatura> equipo, float x, float y, float ancho) {
        float alto = ancho * PROPORCION_ALTO;
        float radioEsquina = alto * PROPORCION_RADIO;
        Tipo tipo = criatura.getTipo();

        // sombra desplazada, fondo y borde del color de luz del tipo
        paint.setColor(colorSombra);
        rect.set(x + alto * 0.04f, y + alto * 0.06f, x + ancho + alto * 0.04f, y + alto + alto * 0.06f);
        canvas.drawRoundRect(rect, radioEsquina, radioEsquina, paint);
        paint.setColor(colorFondo);
        rect.set(x, y, x + ancho, y + alto);
        canvas.drawRoundRect(rect, radioEsquina, radioEsquina, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(alto * 0.018f);
        paint.setColor(Paleta.luz(tipo));
        canvas.drawRoundRect(rect, radioEsquina, radioEsquina, paint);
        paint.setStyle(Paint.Style.FILL);

        // franja izquierda del color base del tipo: recorto el mismo rectángulo redondeado
        canvas.save();
        canvas.clipRect(x, y, x + ancho * 0.06f, y + alto);
        paint.setColor(Paleta.base(tipo));
        canvas.drawRoundRect(rect, radioEsquina, radioEsquina, paint);
        canvas.restore();

        float izquierda = x + ancho * 0.10f;
        float derecha = x + ancho * 0.94f;

        // nombre y nivel; measureText no crea objetos
        paintTexto.setColor(colorNombre);
        paintTexto.setTextSize(alto * 0.27f);
        paintTexto.setFakeBoldText(true);
        canvas.drawText(criatura.getNombre(), izquierda, y + alto * 0.32f, paintTexto);
        paintTexto.setFakeBoldText(false);
        paintTexto.setTextSize(alto * 0.2f);
        paintTexto.setColor(colorNivel);
        float anchoNivel = paintTexto.measureText(textoNivel);
        canvas.drawText(textoNivel, derecha - anchoNivel, y + alto * 0.32f, paintTexto);

        // barra de vida según la vida interpolada, con el fantasma blanco detrás
        float barraX = izquierda;
        float barraY = y + alto * 0.45f;
        float barraAncho = derecha - izquierda;
        float barraAlto = alto * 0.15f;
        paint.setColor(colorBarraFondo);
        rect.set(barraX, barraY, barraX + barraAncho, barraY + barraAlto);
        canvas.drawRoundRect(rect, barraAlto / 2, barraAlto / 2, paint);

        float fraccionFantasma = fraccion(hpFantasma, criatura.getHpMax());
        if (fraccionFantasma > 0f) {
            paint.setColor(colorBarraFantasma);
            rect.set(barraX, barraY, barraX + barraAncho * fraccionFantasma, barraY + barraAlto);
            canvas.drawRoundRect(rect, barraAlto / 2, barraAlto / 2, paint);
        }

        float fraccionVida = fraccion(hpMostrado, criatura.getHpMax());
        if (fraccionVida > 0f) {
            paint.setColor(colorVida(fraccionVida));
            rect.set(barraX, barraY, barraX + barraAncho * fraccionVida, barraY + barraAlto);
            canvas.drawRoundRect(rect, barraAlto / 2, barraAlto / 2, paint);
        }

        // PS numéricos (solo panel del jugador). Solo vuelvo a formatear el
        // texto cuando cambia el entero, es decir, durante los ~350 ms en que
        // la barra persigue a la vida real
        float baseInferior = y + alto * 0.88f;
        if (mostrarNumeros) {
            paintTexto.setTextSize(alto * 0.2f);
            paintTexto.setColor(colorPs);
            int hpEntero = (int) (hpMostrado + 0.5f);
            if (hpEntero < 0) {
                hpEntero = 0;
            }
            if (hpEntero != ultimoHpEntero) {
                ultimoHpEntero = hpEntero;
                textoPs = contexto.getString(R.string.combate_panel_ps, hpEntero, criatura.getHpMax());
            }
            float anchoPs = paintTexto.measureText(textoPs);
            canvas.drawText(textoPs, derecha - anchoPs, baseInferior, paintTexto);
        }

        // Puntos del equipo (criaturas restantes). El punto de la criatura
        // que se está dibujando respeta la foto del evento (su hp interpolado),
        // no el estado final del turno; el resto de miembros usa su estado real
        // (que no cambia mientras se narra el turno).
        float puntoRadio = alto * 0.065f;
        float puntoX = izquierda + puntoRadio;
        float puntoY = baseInferior - puntoRadio;
        for (int i = 0; i < equipo.size(); i++) {
            Criatura miembro = equipo.get(i);
            boolean caido;
            if (miembro == criatura) {
                caido = (hpMostrado <= 0.5f);
            } else {
                caido = miembro.estaDebilitada();
            }
            if (caido) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(puntoRadio * 0.45f);
                paint.setColor(colorPuntoCaido);
                canvas.drawCircle(puntoX, puntoY, puntoRadio, paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                paint.setColor(miembro.getTipo().getColor());
                canvas.drawCircle(puntoX, puntoY, puntoRadio, paint);
            }
            puntoX = puntoX + puntoRadio * 3.2f;
        }
    }

    /**
     * @param hp    vida (puede venir interpolada)
     * @param hpMax vida máxima
     * @return fracción 0-1 acotada
     */
    private float fraccion(float hp, int hpMax) {
        if (hpMax <= 0) {
            return 0f;
        }
        float f = hp / hpMax;
        if (f < 0f) {
            f = 0f;
        }
        if (f > 1f) {
            f = 1f;
        }
        return f;
    }

    /**
     * @param f fracción de vida 0-1
     * @return verde por encima de la mitad, naranja hasta el 20 % y rojo por debajo, interpolados
     */
    private int colorVida(float f) {
        if (f > UMBRAL_VIDA_ALTA) {
            float t = (f - UMBRAL_VIDA_ALTA) / (1f - UMBRAL_VIDA_ALTA);
            return mezclar(colorVidaMedia, colorVidaAlta, t);
        }
        if (f > UMBRAL_VIDA_BAJA) {
            float t = (f - UMBRAL_VIDA_BAJA) / (UMBRAL_VIDA_ALTA - UMBRAL_VIDA_BAJA);
            return mezclar(colorVidaBaja, colorVidaMedia, t);
        }
        return colorVidaBaja;
    }

    /**
     * @param a color de partida
     * @param b color de llegada
     * @param t 0 devuelve a, 1 devuelve b
     * @return color intermedio canal a canal
     */
    private int mezclar(int a, int b, float t) {
        float tAcotado = t;
        if (tAcotado < 0f) {
            tAcotado = 0f;
        }
        if (tAcotado > 1f) {
            tAcotado = 1f;
        }
        int rojo = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * tAcotado);
        int verde = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * tAcotado);
        int azul = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * tAcotado);
        return Color.rgb(rojo, verde, azul);
    }
}
