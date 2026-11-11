/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Escenario.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.juego.combates.R;

import java.util.Calendar;
import java.util.Random;

/**
 * Fondo del combate: cielo según la hora, sol o luna con estrellas, nubes que
 * cruzan la pantalla, colinas, hierba con matas que se mecen, viñeta y las dos
 * plataformas de las criaturas.
 *
 * Va aparte de VistaBatalla para que esa clase se quede con el combate en sí.
 * No es una View: VistaBatalla me llama desde su onDraw y me pasa su Canvas,
 * y yo pinto siempre con mi propio Paint para no depender del estado que
 * dejen las criaturas en el pincel compartido.
 */
public class Escenario {

    /** Fracción de la altura donde está el horizonte (empieza la hierba). */
    private static final float HORIZONTE = 0.30f;
    /** Posición del sol y de la luna, en fracción del ancho y del alto. */
    private static final float ASTRO_X = 0.80f;
    private static final float ASTRO_Y = 0.12f;
    /** Radio del halo del sol y de la luna en fracción del lado menor; el degradado y el círculo usan el mismo. */
    private static final float RADIO_HALO_SOL = 0.24f;
    private static final float RADIO_HALO_LUNA = 0.20f;
    /** Cuánto se desplaza el cráter respecto al centro de la luna (a la derecha y arriba) para hacer el cuarto. */
    private static final float DESFASE_CRATER_X = 0.015f;
    private static final float DESFASE_CRATER_Y = 0.015f;
    /**
     * Plataformas ovaladas (sombra e interior) en fracción del ancho y del alto.
     * Van a ojo bajo cada criatura: tienen que cuadrar con POS_RIVAL_X/Y y
     * POS_JUGADOR_X/Y de VistaBatalla, que es quien coloca a las criaturas.
     */
    private static final float PLATAFORMA_RIVAL_IZQ = 0.55f;
    private static final float PLATAFORMA_RIVAL_ARRIBA = 0.40f;
    private static final float PLATAFORMA_RIVAL_DER = 0.92f;
    private static final float PLATAFORMA_RIVAL_ABAJO = 0.48f;
    private static final float PLATAFORMA_RIVAL_INT_IZQ = 0.58f;
    private static final float PLATAFORMA_RIVAL_INT_ARRIBA = 0.405f;
    private static final float PLATAFORMA_RIVAL_INT_DER = 0.89f;
    private static final float PLATAFORMA_RIVAL_INT_ABAJO = 0.455f;
    private static final float PLATAFORMA_JUGADOR_IZQ = 0.06f;
    private static final float PLATAFORMA_JUGADOR_ARRIBA = 0.80f;
    private static final float PLATAFORMA_JUGADOR_DER = 0.54f;
    private static final float PLATAFORMA_JUGADOR_ABAJO = 0.92f;
    private static final float PLATAFORMA_JUGADOR_INT_IZQ = 0.10f;
    private static final float PLATAFORMA_JUGADOR_INT_ARRIBA = 0.805f;
    private static final float PLATAFORMA_JUGADOR_INT_DER = 0.50f;
    private static final float PLATAFORMA_JUGADOR_INT_ABAJO = 0.875f;
    /** Franjas horarias: mañana desde las 7, tarde desde las 17 y noche desde las 21. */
    private static final int HORA_MANANA = 7;
    private static final int HORA_TARDE = 17;
    private static final int HORA_NOCHE = 21;
    private static final int NUM_MATAS = 18;
    private static final int MAX_ESTRELLAS = 40;
    /** Margen (fracción del ancho) que una nube recorre fuera de pantalla antes de dar la vuelta. */
    private static final float MARGEN_NUBES = 0.22f;

    private Paint paint;
    private RectF rect;
    private Path path;
    private float densidad;   // densidad de pantalla, la leo una vez y no en cada dibujado

    private Shader cieloManana;
    private Shader cieloTarde;
    private Shader cieloNoche;
    private Shader suelo;
    private Shader haloSol;
    private Shader haloLuna;
    private Shader vineta;

    private int hora = 12;        // hora del día (0-23) que decide cielo y astro
    private long tiempoNubes;     // reloj propio de las nubes: solo avanza fuera del hit-stop

    // colores leídos de colors.xml una vez en el constructor
    private int colorCieloMananaArriba;
    private int colorCieloMananaCentro;
    private int colorCieloMananaAbajo;
    private int colorCieloTardeArriba;
    private int colorCieloTardeCentro;
    private int colorCieloTardeAbajo;
    private int colorCieloNocheArriba;
    private int colorCieloNocheCentro;
    private int colorCieloNocheAbajo;
    private int colorSol;
    private int colorHaloSol;
    private int colorHaloSolTransparente;
    private int colorLuna;
    private int colorLunaCrater;
    private int colorHaloLuna;
    private int colorHaloLunaTransparente;
    private int colorNubeCercana;
    private int colorNubeMedia;
    private int colorNubeLejana;
    private int colorColinaLejana;
    private int colorColinaCercana;
    private int colorHierbaClara;
    private int colorHierbaOscura;
    private int colorHierbaBorde;
    private int colorMata;
    private int colorPlataformaRivalSombra;
    private int colorPlataformaRival;
    private int colorPlataformaJugadorSombra;
    private int colorPlataformaJugador;
    private int colorVinetaBorde;

    // estrellas fijas (posiciones de un Random con semilla)
    private float[] estrellaX;
    private float[] estrellaY;
    private float[] estrellaTam;
    private float[] estrellaBrillo;

    /**
     * Crea el pincel, lee los colores del escenario y reparte las estrellas.
     * Los shaders dependen del tamaño y se crean en redimensionar().
     *
     * @param context contexto para leer colors.xml y la densidad de pantalla
     */
    public Escenario(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // las matas son trazos sueltos: con las puntas redondeadas se ven como
        // hierba y no como palitos cortados
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        rect = new RectF();
        path = new Path();
        densidad = context.getResources().getDisplayMetrics().density;

        colorCieloMananaArriba = context.getColor(R.color.cielo_manana_arriba);
        colorCieloMananaCentro = context.getColor(R.color.cielo_manana_centro);
        colorCieloMananaAbajo = context.getColor(R.color.cielo_manana_abajo);
        colorCieloTardeArriba = context.getColor(R.color.cielo_tarde_arriba);
        colorCieloTardeCentro = context.getColor(R.color.cielo_tarde_centro);
        colorCieloTardeAbajo = context.getColor(R.color.cielo_tarde_abajo);
        colorCieloNocheArriba = context.getColor(R.color.cielo_noche_arriba);
        colorCieloNocheCentro = context.getColor(R.color.cielo_noche_centro);
        colorCieloNocheAbajo = context.getColor(R.color.cielo_noche_abajo);
        colorSol = context.getColor(R.color.sol);
        colorHaloSol = context.getColor(R.color.halo_sol);
        colorHaloSolTransparente = context.getColor(R.color.halo_sol_transparente);
        colorLuna = context.getColor(R.color.luna);
        colorLunaCrater = context.getColor(R.color.luna_crater);
        colorHaloLuna = context.getColor(R.color.halo_luna);
        colorHaloLunaTransparente = context.getColor(R.color.halo_luna_transparente);
        colorNubeCercana = context.getColor(R.color.nube_cercana);
        colorNubeMedia = context.getColor(R.color.nube_media);
        colorNubeLejana = context.getColor(R.color.nube_lejana);
        colorColinaLejana = context.getColor(R.color.colina_lejana);
        colorColinaCercana = context.getColor(R.color.colina_cercana);
        colorHierbaClara = context.getColor(R.color.hierba_clara);
        colorHierbaOscura = context.getColor(R.color.hierba_oscura);
        colorHierbaBorde = context.getColor(R.color.hierba_borde);
        colorMata = context.getColor(R.color.mata_hierba);
        colorPlataformaRivalSombra = context.getColor(R.color.plataforma_rival_sombra);
        colorPlataformaRival = context.getColor(R.color.plataforma_rival);
        colorPlataformaJugadorSombra = context.getColor(R.color.plataforma_jugador_sombra);
        colorPlataformaJugador = context.getColor(R.color.plataforma_jugador);
        colorVinetaBorde = context.getColor(R.color.vineta_borde);

        // leo la hora ya en el constructor para que el telón del menú (que
        // nunca recibe batalla) también sea de tarde o de noche
        actualizarHora();

        // las estrellas salen de un Random con semilla fija: así son las mismas
        // en cada partida y no tengo que guardarlas en ningún sitio
        estrellaX = new float[MAX_ESTRELLAS];
        estrellaY = new float[MAX_ESTRELLAS];
        estrellaTam = new float[MAX_ESTRELLAS];
        estrellaBrillo = new float[MAX_ESTRELLAS];
        Random random = new Random(1234);
        for (int i = 0; i < MAX_ESTRELLAS; i++) {
            estrellaX[i] = random.nextFloat();
            estrellaY[i] = random.nextFloat() * 0.28f;
            estrellaTam[i] = 0.45f + random.nextFloat() * 1.20f;
            estrellaBrillo[i] = 0.45f + random.nextFloat() * 0.55f;
        }
    }

    /**
     * Fuerza la hora del escenario. Solo lo llama VistaBatalla.setHora, que
     * existe para hacer capturas de mañana, tarde y noche; ninguna pantalla lo usa.
     *
     * @param hora hora de 0 a 23 (fuera de rango la envuelvo)
     */
    public void setHora(int hora) {
        int horaSegura = hora;
        while (horaSegura < 0) {
            horaSegura = horaSegura + 24;
        }
        while (horaSegura > 23) {
            horaSegura = horaSegura - 24;
        }
        this.hora = horaSegura;
    }

    /**
     * Toma la hora del reloj del sistema. Se llama al crear el escenario y al
     * empezar cada batalla.
     */
    public void actualizarHora() {
        Calendar calendario = Calendar.getInstance();
        hora = calendario.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Recrea los degradados para el tamaño nuevo de la vista.
     *
     * @param w ancho en píxeles
     * @param h alto en píxeles
     */
    public void redimensionar(int w, int h) {
        float horizonte = h * HORIZONTE;
        float astroX = w * ASTRO_X;
        float astroY = h * ASTRO_Y;
        cieloManana = new LinearGradient(0, 0, 0, h,
                new int[]{colorCieloMananaArriba, colorCieloMananaCentro, colorCieloMananaAbajo},
                new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP);
        cieloTarde = new LinearGradient(0, 0, 0, h,
                new int[]{colorCieloTardeArriba, colorCieloTardeCentro, colorCieloTardeAbajo},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        cieloNoche = new LinearGradient(0, 0, 0, h,
                new int[]{colorCieloNocheArriba, colorCieloNocheCentro, colorCieloNocheAbajo},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        suelo = new LinearGradient(0, horizonte, 0, h, colorHierbaClara, colorHierbaOscura, Shader.TileMode.CLAMP);
        haloSol = new RadialGradient(astroX, astroY, Math.min(w, h) * RADIO_HALO_SOL, colorHaloSol, colorHaloSolTransparente, Shader.TileMode.CLAMP);
        haloLuna = new RadialGradient(astroX, astroY, Math.min(w, h) * RADIO_HALO_LUNA, colorHaloLuna, colorHaloLunaTransparente, Shader.TileMode.CLAMP);
        vineta = new RadialGradient(w * 0.5f, h * 0.50f, Math.max(w, h) * 0.68f, Color.TRANSPARENT, colorVinetaBorde, Shader.TileMode.CLAMP);
    }

    /**
     * Avanza el reloj de las nubes. VistaBatalla no me llama durante el
     * hit-stop, así que las nubes también se congelan en ese instante.
     *
     * @param dt milisegundos desde el fotograma anterior
     */
    public void avanzarNubes(long dt) {
        tiempoNubes = tiempoNubes + dt;
    }

    /**
     * Fondo del escenario: cielo, astro, estrellas, nubes, colinas, hierba, matas y viñeta.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     * @param fase   progreso cíclico de 0 a 1 para el parpadeo y el viento
     */
    public void dibujarFondo(Canvas canvas, int w, int h, float fase) {
        Shader cielo = shaderCielo();
        if (cielo != null) {
            paint.setShader(cielo);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }

        dibujarAstro(canvas, w, h, fase);
        dibujarColinas(canvas, w, h);

        if (suelo != null) {
            paint.setShader(suelo);
            canvas.drawRect(0, h * HORIZONTE, w, h, paint);
            paint.setShader(null);
        }
        paint.setColor(colorHierbaBorde);
        canvas.drawRect(0, h * HORIZONTE, w, h * 0.325f, paint);

        dibujarMatas(canvas, w, h, fase);

        if (vineta != null) {
            paint.setShader(vineta);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }
    }

    /**
     * Las dos plataformas ovaladas sobre la hierba: la del rival al fondo y la
     * del jugador delante. Van aparte del fondo porque solo se pintan con batalla.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     */
    public void dibujarPlataformas(Canvas canvas, int w, int h) {
        paint.setColor(colorPlataformaRivalSombra);
        rect.set(w * PLATAFORMA_RIVAL_IZQ, h * PLATAFORMA_RIVAL_ARRIBA, w * PLATAFORMA_RIVAL_DER, h * PLATAFORMA_RIVAL_ABAJO);
        canvas.drawOval(rect, paint);
        paint.setColor(colorPlataformaRival);
        rect.set(w * PLATAFORMA_RIVAL_INT_IZQ, h * PLATAFORMA_RIVAL_INT_ARRIBA, w * PLATAFORMA_RIVAL_INT_DER, h * PLATAFORMA_RIVAL_INT_ABAJO);
        canvas.drawOval(rect, paint);

        paint.setColor(colorPlataformaJugadorSombra);
        rect.set(w * PLATAFORMA_JUGADOR_IZQ, h * PLATAFORMA_JUGADOR_ARRIBA, w * PLATAFORMA_JUGADOR_DER, h * PLATAFORMA_JUGADOR_ABAJO);
        canvas.drawOval(rect, paint);
        paint.setColor(colorPlataformaJugador);
        rect.set(w * PLATAFORMA_JUGADOR_INT_IZQ, h * PLATAFORMA_JUGADOR_INT_ARRIBA, w * PLATAFORMA_JUGADOR_INT_DER, h * PLATAFORMA_JUGADOR_INT_ABAJO);
        canvas.drawOval(rect, paint);
    }

    /**
     * @return el degradado del cielo según la hora: mañana, tarde (17-21) o noche
     */
    private Shader shaderCielo() {
        if (esNoche()) {
            return cieloNoche;
        }
        if (hora >= HORA_TARDE && hora < HORA_NOCHE) {
            return cieloTarde;
        }
        return cieloManana;
    }

    /**
     * @return true de 21:00 a 6:59, que es cuando salen la luna y las estrellas
     */
    private boolean esNoche() {
        if (hora >= HORA_NOCHE) {
            return true;
        }
        if (hora < HORA_MANANA) {
            return true;
        }
        return false;
    }

    /**
     * Sol con halo de día o luna con estrellas de noche, y las nubes por delante.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     * @param fase   progreso cíclico de 0 a 1 para el parpadeo de las estrellas
     */
    private void dibujarAstro(Canvas canvas, int w, int h, float fase) {
        float astroX = w * ASTRO_X;
        float astroY = h * ASTRO_Y;
        if (esNoche()) {
            if (haloLuna != null) {
                paint.setShader(haloLuna);
                canvas.drawCircle(astroX, astroY, Math.min(w, h) * RADIO_HALO_LUNA, paint);
                paint.setShader(null);
            }
            paint.setColor(colorLuna);
            canvas.drawCircle(astroX, astroY, Math.min(w, h) * 0.045f, paint);
            // la luna en cuarto la hago tapando parte del disco con un círculo del
            // color del cielo, desplazado un poco a la derecha y arriba del centro
            paint.setColor(colorLunaCrater);
            canvas.drawCircle(w * (ASTRO_X + DESFASE_CRATER_X), h * (ASTRO_Y - DESFASE_CRATER_Y), Math.min(w, h) * 0.043f, paint);
            dibujarEstrellas(canvas, w, h, fase);
        } else {
            if (haloSol != null) {
                paint.setShader(haloSol);
                canvas.drawCircle(astroX, astroY, Math.min(w, h) * RADIO_HALO_SOL, paint);
                paint.setShader(null);
            }
            paint.setColor(colorSol);
            canvas.drawCircle(astroX, astroY, Math.min(w, h) * 0.055f, paint);
        }
        dibujarNubes(canvas, w, h);
    }

    /**
     * Las 40 estrellas fijas parpadeando con la fase.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     * @param fase   progreso cíclico de 0 a 1
     */
    private void dibujarEstrellas(Canvas canvas, int w, int h, float fase) {
        for (int i = 0; i < MAX_ESTRELLAS; i++) {
            int alfa = (int) (255f * estrellaBrillo[i] * (0.65f + 0.35f * Math.sin(fase * Math.PI * 2f + i)));
            if (alfa < 40) {
                alfa = 40;
            }
            paint.setColor(Color.argb(alfa, 255, 255, 255));
            canvas.drawCircle(estrellaX[i] * w, estrellaY[i] * h, estrellaTam[i] * densidad, paint);
        }
    }

    /**
     * Tres nubes a distinta altura y opacidad, cada una con su propia velocidad.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     */
    private void dibujarNubes(Canvas canvas, int w, int h) {
        paint.setColor(colorNubeCercana);
        dibujarNube(canvas, posicionNube(0.18f, 60000L, w), h * 0.13f, Math.min(w, h) * 0.045f);
        paint.setColor(colorNubeMedia);
        dibujarNube(canvas, posicionNube(0.56f, 90000L, w), h * 0.20f, Math.min(w, h) * 0.035f);
        paint.setColor(colorNubeLejana);
        dibujarNube(canvas, posicionNube(0.84f, 120000L, w), h * 0.09f, Math.min(w, h) * 0.030f);
    }

    /**
     * @param posicionInicial fracción del ancho donde empieza la nube
     * @param duracionMs      lo que tarda en cruzar la pantalla entera
     * @param w               ancho de la vista
     * @return coordenada x actual de la nube
     */
    private float posicionNube(float posicionInicial, long duracionMs, int w) {
        // aquí muevo cada nube con su propio reloj (tiempoNubes), independiente
        // del ciclo de respiración, para que no vayan todas al mismo compás
        float avance = (float) (tiempoNubes % duracionMs) / duracionMs;
        return envolverX((posicionInicial + avance * (1f + MARGEN_NUBES * 2f)) * w, w);
    }

    /**
     * @param x coordenada que puede haberse salido de la pantalla
     * @param w ancho de la vista
     * @return la misma coordenada envuelta al otro lado
     */
    private float envolverX(float x, int w) {
        // dejo un margen para que la nube salte de borde cuando ya está entera
        // fuera de la pantalla y no se vea aparecer de golpe
        float margen = w * MARGEN_NUBES;
        float envuelta = x;
        while (envuelta > w + margen) {
            envuelta = envuelta - w - margen * 2f;
        }
        while (envuelta < -margen) {
            envuelta = envuelta + w + margen * 2f;
        }
        return envuelta;
    }

    /**
     * Una nube: tres círculos y una base redondeada con el color ya puesto en paint.
     *
     * @param canvas dónde dibujar
     * @param cx     centro x de la nube
     * @param cy     centro y de la nube
     * @param r      radio del círculo central
     */
    private void dibujarNube(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawCircle(cx - r * 1.1f, cy + r * 0.3f, r * 0.75f, paint);
        canvas.drawCircle(cx + r * 1.1f, cy + r * 0.3f, r * 0.8f, paint);
        rect.set(cx - r * 1.6f, cy, cx + r * 1.7f, cy + r * 0.95f);
        canvas.drawRoundRect(rect, r * 0.5f, r * 0.5f, paint);
    }

    /**
     * Dos filas de colinas lejanas con cubicTo, semitransparentes para dar profundidad.
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     */
    private void dibujarColinas(Canvas canvas, int w, int h) {
        path.reset();
        path.moveTo(0, h * HORIZONTE);
        path.cubicTo(w * 0.18f, h * 0.18f, w * 0.34f, h * 0.34f, w * 0.52f, h * 0.25f);
        path.cubicTo(w * 0.70f, h * 0.16f, w * 0.84f, h * 0.30f, w, h * 0.23f);
        path.lineTo(w, h * 0.38f);
        path.lineTo(0, h * 0.38f);
        path.close();
        paint.setColor(colorColinaLejana);
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(0, h * 0.32f);
        path.cubicTo(w * 0.20f, h * 0.25f, w * 0.34f, h * 0.35f, w * 0.50f, h * 0.29f);
        path.cubicTo(w * 0.70f, h * 0.22f, w * 0.82f, h * 0.36f, w, h * 0.30f);
        path.lineTo(w, h * 0.40f);
        path.lineTo(0, h * 0.40f);
        path.close();
        paint.setColor(colorColinaCercana);
        canvas.drawPath(path, paint);
    }

    /**
     * Matas de hierba repartidas por el suelo que se mecen con la fase (viento).
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     * @param fase   progreso cíclico de 0 a 1
     */
    private void dibujarMatas(Canvas canvas, int w, int h, float fase) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, w * 0.003f));
        paint.setColor(colorMata);
        for (int i = 0; i < NUM_MATAS; i++) {
            // reparto las matas en cuatro alturas y tres tamaños con el resto de i
            float x = w * (0.04f + i * 0.055f);
            float base = h * (0.36f + (i % 4) * 0.13f);
            float alto = h * (0.020f + (i % 3) * 0.006f);
            float viento = (float) Math.sin(fase * Math.PI * 2f + i) * w * 0.004f;
            canvas.drawLine(x, base, x + viento - w * 0.010f, base - alto, paint);
            canvas.drawLine(x, base, x + viento + w * 0.010f, base - alto * 0.8f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
