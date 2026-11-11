/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  TextosFlotantes.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.juego.combates.R;
import com.juego.combates.modelo.Tipo;

/**
 * Números de daño y curación que suben y se desvanecen sobre la criatura.
 *
 * Igual que Particulas, es un pool de arrays paralelos de tamaño fijo para no
 * crear objetos durante la animación. VistaBatalla me crea, me avanza y me pinta.
 */
public class TextosFlotantes {

    private static final int MAX_TEXTOS = 12;
    private static final long DURACION_TEXTO_MS = 700;
    /** Fracción del alto de la vista que sube el número durante su vida. */
    private static final float SUBIDA = 0.045f;
    /** El número de crítico sale un 40 % más grande. */
    private static final float ESCALA_CRITICO = 1.40f;
    /** Tamaño de letra del número de daño y del "+N" de curación, en fracción del radio de la criatura. */
    private static final float TAMANO_DANO = 0.42f;
    private static final float TAMANO_CURACION = 0.40f;
    /** Grosor del borde oscuro del número, en fracción de su tamaño de letra. */
    private static final float GROSOR_BORDE = 0.12f;

    private Paint paintTexto;
    private int colorNormal;        // blanco del daño normal
    private int colorCritico;       // amarillo del crítico
    private int colorEficaz;        // naranja de "muy eficaz"
    private int colorPocoEficaz;    // gris de "poco eficaz"
    private int colorCuracion;      // verde de "+N", el mismo de las partículas de curación

    private boolean[] textoVivo;
    private String[] textoValor;
    private float[] textoX;
    private float[] textoY;
    private float[] textoVida;
    private float[] textoTam;
    private int[] textoColor;

    /**
     * Reserva los arrays del pool, prepara el pincel y lee los colores.
     *
     * @param context contexto para leer colors.xml
     */
    public TextosFlotantes(Context context) {
        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorNormal = context.getColor(R.color.texto_blanco);
        colorCritico = context.getColor(R.color.texto_dano_critico);
        colorEficaz = context.getColor(R.color.texto_dano_eficaz);
        colorPocoEficaz = context.getColor(R.color.texto_dano_poco_eficaz);
        colorCuracion = Paleta.luz(Tipo.PLANTA);

        textoVivo = new boolean[MAX_TEXTOS];
        textoValor = new String[MAX_TEXTOS];
        textoX = new float[MAX_TEXTOS];
        textoY = new float[MAX_TEXTOS];
        textoVida = new float[MAX_TEXTOS];
        textoTam = new float[MAX_TEXTOS];
        textoColor = new int[MAX_TEXTOS];
        limpiar();
    }

    /**
     * Número de daño: blanco normal, amarillo y un 40 % mayor en crítico,
     * naranja si es muy eficaz y gris si es poco eficaz.
     *
     * @param dano     puntos de daño a enseñar
     * @param x        posición x del texto
     * @param y        posición y inicial (luego sube)
     * @param r        radio de la criatura, del que sale el tamaño de letra
     * @param critico  true si fue crítico
     * @param eficacia multiplicador de tipo: mayor que 1 muy eficaz, menor poco eficaz
     */
    public void crearDano(int dano, float x, float y, float r, boolean critico, double eficacia) {
        // textoLibre nunca devuelve -1: si el pool está lleno reutiliza el hueco 0
        int indice = textoLibre();
        textoVivo[indice] = true;
        textoValor[indice] = String.valueOf(dano);
        textoX[indice] = x;
        textoY[indice] = y;
        textoVida[indice] = DURACION_TEXTO_MS;
        textoTam[indice] = r * TAMANO_DANO;
        textoColor[indice] = colorNormal;
        if (critico) {
            textoTam[indice] = textoTam[indice] * ESCALA_CRITICO;
            textoColor[indice] = colorCritico;
        } else if (eficacia > 1.0) {
            textoColor[indice] = colorEficaz;
        } else if (eficacia < 1.0) {
            textoColor[indice] = colorPocoEficaz;
        }
    }

    /**
     * Texto verde "+N" para la curación.
     *
     * @param cantidad PS recuperados
     * @param x        posición x del texto
     * @param y        posición y inicial
     * @param r        radio de la criatura, del que sale el tamaño de letra
     */
    public void crearCuracion(int cantidad, float x, float y, float r) {
        // textoLibre nunca devuelve -1: si el pool está lleno reutiliza el hueco 0
        int indice = textoLibre();
        textoVivo[indice] = true;
        textoValor[indice] = "+" + cantidad;
        textoX[indice] = x;
        textoY[indice] = y;
        textoVida[indice] = DURACION_TEXTO_MS;
        textoTam[indice] = r * TAMANO_CURACION;
        textoColor[indice] = colorCuracion;
    }

    /**
     * @return índice del primer hueco libre del pool de textos; si está lleno reutilizo el 0
     */
    private int textoLibre() {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            if (!textoVivo[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Descuenta vida a los números flotantes y apaga los que llegan a cero.
     *
     * @param dt milisegundos desde el fotograma anterior
     */
    public void actualizar(long dt) {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            if (textoVivo[i]) {
                textoVida[i] = textoVida[i] - dt;
                if (textoVida[i] <= 0f) {
                    textoVivo[i] = false;
                }
            }
        }
    }

    /**
     * Números de daño y curación: suben, se desvanecen y llevan borde oscuro
     * (dos pasadas del mismo texto: STROKE y FILL).
     *
     * @param canvas    dónde dibujar
     * @param altoVista alto de la vista, del que sale cuánto sube el número
     */
    public void dibujar(Canvas canvas, int altoVista) {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            if (textoVivo[i]) {
                float t = 1f - textoVida[i] / DURACION_TEXTO_MS;
                int alfa = (int) (255f * (1f - t));
                if (alfa < 0) {
                    alfa = 0;
                }
                float y = textoY[i] - altoVista * SUBIDA * t;
                paintTexto.setTextSize(textoTam[i]);
                paintTexto.setFakeBoldText(true);
                paintTexto.setStyle(Paint.Style.STROKE);
                paintTexto.setStrokeWidth(textoTam[i] * GROSOR_BORDE);
                paintTexto.setColor(Color.argb(alfa, Color.red(Paleta.TEXTO_OSCURO), Color.green(Paleta.TEXTO_OSCURO), Color.blue(Paleta.TEXTO_OSCURO)));
                canvas.drawText(textoValor[i], textoX[i], y, paintTexto);
                paintTexto.setStyle(Paint.Style.FILL);
                paintTexto.setColor(Color.argb(alfa, Color.red(textoColor[i]), Color.green(textoColor[i]), Color.blue(textoColor[i])));
                canvas.drawText(textoValor[i], textoX[i], y, paintTexto);
                paintTexto.setFakeBoldText(false);
            }
        }
        paintTexto.setStyle(Paint.Style.FILL);
        paintTexto.setAlpha(255);
    }

    /**
     * @return true si queda algún número vivo que haya que seguir animando
     */
    public boolean hayVivos() {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            if (textoVivo[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apaga todos los textos (al empezar una batalla nueva).
     */
    public void limpiar() {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            textoVivo[i] = false;
            textoValor[i] = "";
        }
    }
}
