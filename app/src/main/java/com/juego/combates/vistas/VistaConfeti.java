/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  VistaConfeti.java
 */
package com.juego.combates.vistas;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.juego.combates.modelo.Tipo;

import java.util.Random;

/**
 * Capa pequeña de confeti para el diálogo de victoria.
 *
 * No usa imágenes: son rectángulos dibujados con Canvas y posiciones
 * preasignadas para no crear objetos mientras se pinta.
 */
public class VistaConfeti extends View {

    private static final int MAX_PIEZAS = 72;
    /** Lo que tarda la fase en ir de 0 a 1; cada pieza recorre la pantalla a su velocidad sobre este ciclo. */
    private static final long DURACION_CICLO_MS = 2400;
    /** Altura relativa que baja una pieza antes de volver arriba (1,25 pantallas para que no salte a la vista). */
    private static final float RECORRIDO = 1.25f;
    /** Las piezas empiezan un 20 % por encima del borde superior para entrar ya cayendo. */
    private static final float DESFASE_ARRIBA = 0.20f;
    /** Opacidad de las piezas en la parte alta y en la baja (a partir de LIMITE_ALFA_ABAJO). */
    private static final int ALFA_ARRIBA = 210;
    private static final int ALFA_ABAJO = 150;
    private static final float LIMITE_ALFA_ABAJO = 0.75f;
    private static final float DOS_PI = (float) (Math.PI * 2);
    /** Velocidad de caída de una pieza: entre VELOCIDAD_MINIMA y VELOCIDAD_MINIMA + VELOCIDAD_RANGO recorridos por ciclo. */
    private static final float VELOCIDAD_MINIMA = 0.55f;
    private static final float VELOCIDAD_RANGO = 0.70f;
    /** Tamaño de una pieza en dp: entre TAMANO_MINIMO_DP y TAMANO_MINIMO_DP + TAMANO_RANGO_DP. */
    private static final float TAMANO_MINIMO_DP = 4f;
    private static final float TAMANO_RANGO_DP = 6f;
    /** Vaivén lateral de cada pieza, en fracción del ancho. */
    private static final float VAIVEN = 0.035f;
    /** Cada pieza es un rectángulo de estas proporciones respecto a su tamaño (mitad del ancho y mitad del alto). */
    private static final float MITAD_ANCHO_PIEZA = 0.45f;
    private static final float MITAD_ALTO_PIEZA = 0.18f;

    // los cinco colores del confeti son tonos de Paleta que ya usan las criaturas,
    // así el diálogo de victoria casa con el resto del juego
    private static final int COLOR_AMARILLO = Paleta.luz(Tipo.ELECTRICO);
    private static final int COLOR_ROSA = Paleta.acento(Tipo.PLANTA);
    private static final int COLOR_AZUL = Paleta.luz(Tipo.AGUA);
    private static final int COLOR_VERDE = Paleta.luz(Tipo.PLANTA);
    private static final int COLOR_NARANJA = Paleta.luz(Tipo.FUEGO);

    private Paint paint;
    private RectF rect;
    private ValueAnimator animador;
    private boolean activo;
    private float fase;
    private float densidad; // la leo una vez: en onDraw se usa por pieza y por fotograma

    private float[] piezaX;
    private float[] piezaY;
    private float[] piezaVelocidad;
    private float[] piezaTam;
    private float[] piezaGiro;
    private int[] piezaColor;

    /**
     * Constructor para crear la vista desde código.
     *
     * @param context contexto de Android
     */
    public VistaConfeti(Context context) {
        super(context);
        inicializar();
    }

    /**
     * Constructor usado desde dialogo_fin.xml.
     *
     * @param context contexto de Android
     * @param attrs   atributos del XML
     */
    public VistaConfeti(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    /**
     * Reparte las piezas con un Random de semilla fija para que el confeti sea
     * siempre igual y no haga falta crear nada al pintar.
     */
    private void inicializar() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();
        densidad = getResources().getDisplayMetrics().density;
        piezaX = new float[MAX_PIEZAS];
        piezaY = new float[MAX_PIEZAS];
        piezaVelocidad = new float[MAX_PIEZAS];
        piezaTam = new float[MAX_PIEZAS];
        piezaGiro = new float[MAX_PIEZAS];
        piezaColor = new int[MAX_PIEZAS];

        Random random = new Random(5678);
        for (int i = 0; i < MAX_PIEZAS; i++) {
            piezaX[i] = random.nextFloat();
            piezaY[i] = random.nextFloat();
            piezaVelocidad[i] = VELOCIDAD_MINIMA + random.nextFloat() * VELOCIDAD_RANGO;
            piezaTam[i] = TAMANO_MINIMO_DP + random.nextFloat() * TAMANO_RANGO_DP;
            piezaGiro[i] = random.nextFloat() * 360f;
            piezaColor[i] = colorPorIndice(i);
        }
    }

    /**
     * Activa o detiene la lluvia de confeti.
     *
     * @param activo true para mostrarla
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
        if (activo) {
            arrancar();
        } else {
            parar();
            invalidate();
        }
    }

    /**
     * @param indice posición de la pieza
     * @return uno de los cinco colores, rotando
     */
    private int colorPorIndice(int indice) {
        int resto = indice % 5;
        if (resto == 0) {
            return COLOR_AMARILLO;
        } else if (resto == 1) {
            return COLOR_ROSA;
        } else if (resto == 2) {
            return COLOR_AZUL;
        } else if (resto == 3) {
            return COLOR_VERDE;
        }
        return COLOR_NARANJA;
    }

    /**
     * Crea y lanza el ValueAnimator infinito que mueve la fase de 0 a 1 cada ciclo.
     * Si ya hay uno en marcha no hago nada, para no acumular animadores.
     */
    private void arrancar() {
        if (animador != null && animador.isRunning()) {
            return;
        }
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setDuration(DURACION_CICLO_MS);
        animador.setRepeatCount(ValueAnimator.INFINITE);
        animador.setInterpolator(new LinearInterpolator());
        animador.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                fase = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        animador.start();
    }

    /**
     * Cancela el animador para no dejarlo vivo con la vista oculta o desmontada.
     */
    private void parar() {
        if (animador != null) {
            animador.cancel();
            animador.removeAllUpdateListeners();
            animador = null;
        }
    }

    /**
     * Al salir de la ventana paro el animador para no dejarlo vivo sin vista.
     */
    @Override
    protected void onDetachedFromWindow() {
        parar();
        super.onDetachedFromWindow();
    }

    /**
     * Con la ventana oculta paro el animador y al volver a verse lo rearranco si el confeti sigue activo.
     *
     * @param visibility View.VISIBLE, INVISIBLE o GONE de la ventana
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            if (activo) {
                arrancar();
            }
        } else {
            parar();
        }
    }

    /**
     * Pinta las piezas en la posición que les toca según la fase del animador.
     *
     * @param canvas dónde dibujar
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!activo) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }

        for (int i = 0; i < MAX_PIEZAS; i++) {
            // cada pieza baja a su velocidad y cuando completa el recorrido vuelve arriba
            float t = piezaY[i] + fase * piezaVelocidad[i];
            while (t > RECORRIDO) {
                t = t - RECORRIDO;
            }
            // el seno le da el vaivén lateral
            float x = piezaX[i] * w + (float) Math.sin((fase * DOS_PI) + i) * w * VAIVEN;
            float y = t * h - h * DESFASE_ARRIBA;
            float tam = piezaTam[i] * densidad;
            int alfa = ALFA_ARRIBA;
            if (y > h * LIMITE_ALFA_ABAJO) {
                alfa = ALFA_ABAJO;
            }

            paint.setColor(Color.argb(alfa, Color.red(piezaColor[i]), Color.green(piezaColor[i]), Color.blue(piezaColor[i])));
            canvas.save();
            canvas.rotate(piezaGiro[i] + fase * 360f, x, y);
            rect.set(x - tam * MITAD_ANCHO_PIEZA, y - tam * MITAD_ALTO_PIEZA, x + tam * MITAD_ANCHO_PIEZA, y + tam * MITAD_ALTO_PIEZA);
            canvas.drawRect(rect, paint);
            canvas.restore();
        }
    }
}
