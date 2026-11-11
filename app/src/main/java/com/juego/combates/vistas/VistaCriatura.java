/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  VistaCriatura.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Fabrica;
import com.juego.combates.modelo.Tipo;

import java.util.Random;

/**
 * Vista personalizada que dibuja una sola criatura, quieta y sin espejo.
 * La uso en el menú (criaturas de adorno), en las tarjetas de selección,
 * en las ranuras del equipo y en el diálogo de cambio de criatura.
 */
public class VistaCriatura extends View {

    /** Dónde va el centro de la criatura: a media anchura y algo por debajo de media altura. */
    private static final float CENTRO_X = 0.5f;
    private static final float CENTRO_Y = 0.60f;
    /** Radio de dibujo en fracción del lado menor de la vista. */
    private static final float RADIO = 0.32f;

    private Paint paint;
    private Path path;
    private String especie = DibujoCriatura.ESPECIE_POR_DEFECTO;
    private Tipo tipo = Tipo.NORMAL;
    private boolean vacia = false;  // true = no dibuja nada (ranura libre)

    /**
     * Constructor para crear la vista desde código.
     *
     * @param context contexto de Android
     */
    public VistaCriatura(Context context) {
        super(context);
        inicializar();
    }

    /**
     * Constructor que usa Android al cargar la vista desde un XML.
     * Es obligatorio, si no da InflateException al abrir la pantalla.
     *
     * @param context contexto de Android
     * @param attrs   atributos del XML
     */
    public VistaCriatura(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    /**
     * Crea una sola vez el pincel y el path que DibujoCriatura reutiliza en cada onDraw.
     */
    private void inicializar() {
        // ANTI_ALIAS suaviza los bordes de los círculos
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();
    }

    /**
     * Cambia la criatura completa para que la vista pueda usar su tipo y su silueta.
     * Si le paso null, la vista se queda vacía (lo uso para las ranuras del equipo sin rellenar).
     *
     * @param criatura criatura a dibujar, o null para no dibujar nada
     */
    public void setCriatura(Criatura criatura) {
        if (criatura == null) {
            vacia = true;
        } else {
            vacia = false;
            especie = criatura.getNombre();
            tipo = criatura.getTipo();
        }
        invalidate();
    }

    /**
     * Pone una especie elegida al azar entre las de Fabrica. Lo usan las
     * criaturas de adorno del login y de recuperar contraseña.
     *
     * @param rng generador con el que elegir la especie
     */
    public void ponerEspecieAlAzar(Random rng) {
        String[] especies = Fabrica.getEspecies();
        setCriatura(Fabrica.crear(especies[rng.nextInt(especies.length)]));
    }

    /**
     * Dibuja la criatura centrada y quieta, o nada si la vista está vacía.
     *
     * @param canvas dónde dibujar
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (vacia) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return; // todavía no tiene tamaño
        }
        float cx = w * CENTRO_X;
        float cy = h * CENTRO_Y; // bajo el centro para que quepan las orejas de Chispin
        float r = Math.min(w, h) * RADIO;
        // sin golpe, sin espejo y con fase 0: aquí la criatura está siempre quieta
        DibujoCriatura.dibujar(canvas, paint, path, cx, cy, r, especie, tipo, false, false, 0f);
    }
}
