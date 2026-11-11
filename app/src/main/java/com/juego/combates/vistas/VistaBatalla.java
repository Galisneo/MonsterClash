/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  VistaBatalla.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.juego.combates.R;
import com.juego.combates.batalla.Batalla;
import com.juego.combates.batalla.Evento;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Tipo;

/**
 * Vista personalizada del combate: junta el escenario (Escenario), las dos
 * criaturas con sus animaciones, las partículas (Particulas), los números
 * flotantes (TextosFlotantes) y los paneles de vida (PanelCriatura).
 *
 * Dibuja la "foto" del Evento que se está enseñando (criaturaMostradaJugador,
 * hpMostradoEventoJugador...), no el estado real del combate, para que la vida
 * baje al ritmo de los mensajes. Así la sacudida y el destello de daño caen
 * sobre la criatura correcta aunque el motor ya haya calculado el reemplazo
 * del rival por adelantado.
 *
 * Sin batalla (telón del menú) solo pinta el escenario según la hora del día.
 */
public class VistaBatalla extends View {

    // duraciones de las animaciones
    private static final long DURACION_GOLPE_MS = 450;
    private static final long DURACION_FLASH_GOLPE_MS = 80;
    private static final long DURACION_HIT_STOP_MS = 110;
    private static final long DURACION_ENTRADA_MS = 480;
    private static final long DURACION_KO_MS = 650;
    private static final long DURACION_ATAQUE_MS = 620;
    private static final long DURACION_ESQUIVA_MS = 320;
    private static final long DURACION_DERROTA_MS = 600;

    // ritmo del bucle y de la vida interpolada
    /** Cada cuánto repinta el bucle ambiental (~30 fps). */
    private static final long INTERVALO_BUCLE_MS = 33;
    /** Duración del ciclo de respiración, viento y parpadeo (la "fase" va de 0 a 1 en este tiempo). */
    private static final long CICLO_FASE_MS = 1600;
    /** La vida mostrada alcanza a la real en este tiempo. */
    private static final long TIEMPO_PERSECUCION_VIDA_MS = 350;
    /** El fantasma blanco de la barra se recoge en este tiempo. */
    private static final long TIEMPO_FANTASMA_MS = 600;
    /** Tope del salto de tiempo entre fotogramas, para que un parón no dispare las partículas. */
    private static final long DT_MAXIMO_MS = 50;

    // derrota y KO
    /** Número de filtros de desaturación precalculados para la derrota (más el 0 son 11). */
    private static final int PASOS_DERROTA = 10;
    /** Cuánto pierde de saturación cada paso de derrota. */
    private static final float PASO_SATURACION_DERROTA = 0.075f;
    /** Saturación que le queda a una criatura mientras cae debilitada. */
    private static final float SATURACION_KO = 0.25f;

    // posiciones de las criaturas en fracción del ancho/alto y radio en fracción del lado menor
    private static final float POS_RIVAL_X = 0.735f;
    private static final float POS_RIVAL_Y = 0.405f;
    private static final float RADIO_RIVAL = 0.115f;
    private static final float POS_JUGADOR_X = 0.29f;
    private static final float POS_JUGADOR_Y = 0.735f;
    private static final float RADIO_JUGADOR = 0.15f;
    /** Desde qué distancia (fracción del ancho) entra deslizándose una criatura nueva. */
    private static final float DISTANCIA_ENTRADA = 0.25f;

    // sacudida de cámara al recibir un golpe
    /** Periodos del vaivén de la cámara en ms (distintos en x e y para que no sea un círculo). */
    private static final double SACUDIDA_PERIODO_X_MS = 18.0;
    private static final double SACUDIDA_PERIODO_Y_MS = 22.0;
    /** Amplitud máxima de la sacudida en fracción del ancho (x) y del alto (y). */
    private static final float SACUDIDA_AMPLITUD_X = 0.012f;
    private static final float SACUDIDA_AMPLITUD_Y = 0.004f;
    /** El daño se mide respecto a esta fracción de la vida máxima del golpeado. */
    private static final float FRACCION_VIDA_SACUDIDA = 0.35f;
    /** Un crítico o un KO sacude esto más. */
    private static final float MULTIPLICADOR_CRITICO_SACUDIDA = 1.5f;
    /** Ningún golpe sacude menos que esto (la intensidad va de aquí a 1). */
    private static final float INTENSIDAD_MINIMA = 0.22f;
    /** Alfa (0-255) que alcanza el velo oscuro al final de la derrota: 90 es un 35 %. */
    private static final int ALFA_VELO_DERROTA = 90;

    // movimiento de las criaturas, en fracción del ancho de la vista salvo que se diga otra cosa
    /** Cuánto retrocede la criatura golpeada, alejándose del atacante. */
    private static final float RETROCESO_GOLPE = 0.04f;
    /** Escala con la que aparece una criatura nueva y cuánto crece hasta llegar a 1,12 (luego rebota). */
    private static final float ESCALA_ENTRADA_INICIAL = 0.4f;
    private static final float CRECIMIENTO_ENTRADA = 0.72f;
    /** A partir de este progreso suavizado la entrada encoge hacia 1 (el rebote) y con qué pendiente. */
    private static final float INICIO_REBOTE_ENTRADA = 0.75f;
    private static final float PENDIENTE_REBOTE_ENTRADA = 0.48f;
    /** Caída del KO: cuánto se aplasta en vertical, cuánto se estrecha, cuánto baja (en radios) y cuánto gira en grados. */
    private static final float APLASTE_KO = 0.35f;
    private static final float ESTRECHAMIENTO_KO = 0.10f;
    private static final float CAIDA_KO = 0.95f;
    private static final float GIRO_KO_GRADOS = 25f;
    /** Embestida al atacar: distancia que avanza y cuánto se deforma (más ancha y más baja). */
    private static final float DISTANCIA_EMBESTIDA = 0.060f;
    private static final float DEFORMACION_EMBESTIDA = 0.06f;
    private static final float ENSANCHE_EMBESTIDA = 0.04f;
    /** Rebote en el sitio al curarse, en fracción de la escala. */
    private static final float REBOTE_CURACION = 0.06f;
    /** Cuánto se aparta la criatura cuando le fallan un ataque. */
    private static final float DISTANCIA_ESQUIVA = 0.035f;

    // posición y ancho de los paneles en fracción de la vista
    private static final float PANEL_RIVAL_X = 0.04f;
    private static final float PANEL_RIVAL_Y = 0.045f;
    private static final float PANEL_JUGADOR_X = 0.48f;
    private static final float PANEL_JUGADOR_Y = 0.68f;
    private static final float PANEL_ANCHO = 0.48f;

    private Batalla batalla;
    private Paint paint;
    private Paint paintCapa;
    private Path path;

    private Escenario escenario;
    private Particulas particulas;
    private TextosFlotantes textos;
    private PanelCriatura panelJugador;
    private PanelCriatura panelRival;

    private ColorMatrixColorFilter filtroGris;
    private ColorMatrixColorFilter[] filtrosDerrota;
    private int colorVeloDerrota;

    private boolean bucleActivo;
    private boolean congelado;   // true cuando el combate ha acabado y no hace falta seguir animando

    // sacudida y ataque
    private long inicioGolpe = -1;
    private int objetivoGolpe = Evento.NADA;
    private float intensidadGolpe;
    private long inicioHitStop = -1; // congela partículas y vida un instante en crítico o KO
    private long inicioAtaque = -1;
    private DetalleGolpe detalleAtaque;
    private long inicioEsquivaJugador = -1;
    private long inicioEsquivaRival = -1;

    // entrada y caída
    private long inicioEntradaJugador = -1;
    private long inicioEntradaRival = -1;
    private long inicioKOJugador = -1;
    private long inicioKORival = -1;
    private long inicioDerrotaFinal = -1;
    private boolean derrotaFinal;

    // foto del evento actual: qué criatura y qué vida enseño en este momento
    private Criatura criaturaMostradaJugador;
    private Criatura criaturaMostradaRival;
    private int hpMostradoEventoJugador;
    private int hpMostradoEventoRival;

    // vida interpolada en paneles: persigue a la del evento poco a poco
    private float hpMostradoJugador;
    private float hpMostradoRival;
    private float hpFantasmaJugador;
    private float hpFantasmaRival;

    // reloj del fotograma anterior, para calcular el dt real; -1 obliga a
    // empezar de cero tras un parón (vista oculta o bucle recién arrancado)
    private long ultimoFrame = -1;

    /**
     * Bucle ambiental: repinta cada INTERVALO_BUCLE_MS (~30 fps) mientras la
     * vista está visible, para nubes, respiración y matas.
     */
    private Runnable bucleAmbiente = new Runnable() {
        @Override
        public void run() {
            if (!bucleActivo) {
                return;
            }
            // estoy en el hilo principal, así que invalido directamente: con
            // postInvalidateDelayed metía un segundo mensaje en la cola por tick
            // y el repintado llegaba 33 ms más tarde de lo pedido
            invalidate();
            postDelayed(this, INTERVALO_BUCLE_MS);
        }
    };

    /**
     * Constructor para crear la vista desde código.
     *
     * @param context contexto de Android
     */
    public VistaBatalla(Context context) {
        super(context);
        inicializar();
    }

    /**
     * Constructor que usa Android al cargar la vista desde un layout: el escenario
     * de activity_combate.xml y el telón sin batalla de activity_menu.xml.
     *
     * @param context contexto de Android
     * @param attrs   atributos del XML
     */
    public VistaBatalla(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    /**
     * Crea una sola vez los pinceles, filtros y las piezas del combate que
     * reutilizo en onDraw.
     */
    private void inicializar() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCapa = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();
        colorVeloDerrota = getContext().getColor(R.color.velo_derrota);

        escenario = new Escenario(getContext());
        particulas = new Particulas(getContext());
        textos = new TextosFlotantes(getContext());
        panelRival = new PanelCriatura(getContext(), false);
        panelJugador = new PanelCriatura(getContext(), true);

        // la matriz solo hace falta para construir el filtro, así que es local
        ColorMatrix matrizGris = new ColorMatrix();
        matrizGris.setSaturation(SATURACION_KO);
        filtroGris = new ColorMatrixColorFilter(matrizGris);
        // precalculo los filtros de la derrota: crear un ColorMatrixColorFilter
        // por fotograma sería crear objetos dentro de onDraw
        filtrosDerrota = new ColorMatrixColorFilter[PASOS_DERROTA + 1];
        for (int i = 0; i < filtrosDerrota.length; i++) {
            ColorMatrix matriz = new ColorMatrix();
            matriz.setSaturation(1f - i * PASO_SATURACION_DERROTA);
            filtrosDerrota[i] = new ColorMatrixColorFilter(matriz);
        }
    }

    /**
     * Indica qué combate dibujar. Se llama al empezar cada batalla.
     *
     * @param batalla el combate nuevo
     */
    public void setBatalla(Batalla batalla) {
        this.batalla = batalla;
        this.criaturaMostradaJugador = batalla.getActivaJugador();
        this.hpMostradoEventoJugador = batalla.getActivaJugador().getHp();
        this.criaturaMostradaRival = batalla.getActivaRival();
        this.hpMostradoEventoRival = batalla.getActivaRival().getHp();
        hpMostradoJugador = hpMostradoEventoJugador;
        hpMostradoRival = hpMostradoEventoRival;
        hpFantasmaJugador = hpMostradoEventoJugador;
        hpFantasmaRival = hpMostradoEventoRival;

        escenario.actualizarHora();

        inicioGolpe = -1;
        objetivoGolpe = Evento.NADA;
        intensidadGolpe = 0f;
        inicioHitStop = -1;
        inicioAtaque = -1;
        detalleAtaque = null;
        inicioEsquivaJugador = -1;
        inicioEsquivaRival = -1;
        inicioEntradaJugador = -1;
        inicioEntradaRival = -1;
        inicioKOJugador = -1;
        inicioKORival = -1;
        inicioDerrotaFinal = -1;
        derrotaFinal = false;
        congelado = false;
        panelJugador.setCriatura(criaturaMostradaJugador);
        panelRival.setCriatura(criaturaMostradaRival);
        particulas.limpiar();
        textos.limpiar();
        arrancarBucleAmbiente();
        invalidate();
    }

    /**
     * Para el bucle ambiental cuando el combate ya ha terminado y el diálogo de
     * fin está encima: no tiene sentido seguir repintando un escenario estático.
     * setBatalla() lo vuelve a arrancar.
     */
    public void congelarEscena() {
        congelado = true;
        pararBucleAmbiente();
    }

    /**
     * Fuerza la hora del escenario. Ninguna pantalla lo llama: existe para
     * hacer las capturas de mañana, tarde y noche del README, llamándolo a mano
     * desde CombateActivity mientras se toma la captura.
     *
     * @param hora hora de 0 a 23
     */
    public void setHora(int hora) {
        escenario.setHora(hora);
        invalidate();
    }

    /**
     * Arranca la anticipación del golpe cuando aparece "X usa Y".
     *
     * @param detalle detalle visual del ataque
     */
    public void anunciarAtaque(DetalleGolpe detalle) {
        if (detalle == null) {
            return;
        }
        detalleAtaque = detalle;
        inicioAtaque = SystemClock.uptimeMillis();
        invalidate();
    }

    /**
     * Muestra el estado de un evento: fija qué criaturas dibujar con su vida
     * de ese instante (la "foto" que trae cada Evento, no el estado final del
     * turno) y arranca la animación que toque: entrada, KO, sacudida, esquiva
     * o curación. Dibujar la foto es lo que permite que el golpe caiga sobre la
     * criatura correcta aunque el motor ya haya calculado el reemplazo del
     * rival por adelantado.
     *
     * @param e       evento que se está enseñando
     * @param detalle lectura visual del ataque actual (puede ser null)
     */
    public void mostrarEvento(Evento e, DetalleGolpe detalle) {
        long ahora = SystemClock.uptimeMillis();
        String texto = e.getTexto();
        boolean cambioJugador = (e.getJugador() != criaturaMostradaJugador);
        boolean cambioRival = (e.getRival() != criaturaMostradaRival);

        // en la presentación setBatalla ya ha puesto las activas, así que la
        // comparación de objetos no salta: miro también el texto exacto de
        // Batalla, con los trozos que tiene apuntados LectorDeEventos
        if (e.getEfecto() == Evento.REFRESCO) {
            if (e.getBando() == Evento.JUGADOR && texto.contains(LectorDeEventos.TEXTO_ENTRADA_JUGADOR)) {
                cambioJugador = true;
            }
            if (e.getBando() == Evento.RIVAL && texto.contains(LectorDeEventos.TEXTO_ENTRADA_RIVAL)) {
                cambioRival = true;
            }
        }

        if (cambioJugador) {
            inicioEntradaJugador = ahora;
            inicioKOJugador = -1;
        } else if (hpMostradoEventoJugador > 0 && e.getHpJugador() <= 0) {
            inicioKOJugador = ahora;
        }

        if (cambioRival) {
            inicioEntradaRival = ahora;
            inicioKORival = -1;
        } else if (hpMostradoEventoRival > 0 && e.getHpRival() <= 0) {
            inicioKORival = ahora;
        }

        this.criaturaMostradaJugador = e.getJugador();
        this.hpMostradoEventoJugador = e.getHpJugador();
        this.criaturaMostradaRival = e.getRival();
        this.hpMostradoEventoRival = e.getHpRival();
        if (cambioJugador) {
            panelJugador.setCriatura(criaturaMostradaJugador);
        }
        if (cambioRival) {
            panelRival.setCriatura(criaturaMostradaRival);
        }

        if (e.getEfecto() == Evento.GOLPE_RIVAL || e.getEfecto() == Evento.GOLPE_JUGADOR) {
            objetivoGolpe = e.getEfecto();
            inicioGolpe = ahora;
            intensidadGolpe = calcularIntensidad(e, detalle);
            crearImpacto(e, detalle);
            // hit-stop: en crítico o KO congelo un instante partículas y vida
            // para que el golpe pese, sin tocar el ritmo de 700 ms de la Activity
            if (detalle != null && (detalle.esCritico() || detalle.esKo())) {
                inicioHitStop = ahora;
            }
        }

        if (detalle != null && detalle.haFallado() && texto.contains(LectorDeEventos.TEXTO_FALLO)) {
            if (detalle.atacaJugador()) {
                inicioEsquivaRival = ahora;
            } else {
                inicioEsquivaJugador = ahora;
            }
        }

        if (detalle != null && detalle.esCuracion() && texto.contains(LectorDeEventos.TEXTO_CURACION)) {
            crearCuracion(detalle);
        }

        invalidate();
    }

    /**
     * Arranca el efecto visual final sin decidir nada del combate: en derrota
     * empieza la desaturación; en victoria no hay efecto (el confeti va en el diálogo).
     *
     * @param victoria true si ganó el jugador
     */
    public void mostrarFinVisual(boolean victoria) {
        derrotaFinal = false;
        if (victoria) {
            inicioDerrotaFinal = -1;
        } else {
            inicioDerrotaFinal = SystemClock.uptimeMillis();
        }
        postInvalidateOnAnimation();
    }

    /**
     * Con el tamaño nuevo, el escenario recrea sus degradados.
     *
     * @param w    ancho nuevo
     * @param h    alto nuevo
     * @param oldw ancho anterior
     * @param oldh alto anterior
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        escenario.redimensionar(w, h);
    }

    /**
     * Al entrar en la ventana arranca el bucle ambiental.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        arrancarBucleAmbiente();
    }

    /**
     * Al salir de la ventana para el bucle para no repintar una vista que ya no está.
     */
    @Override
    protected void onDetachedFromWindow() {
        pararBucleAmbiente();
        super.onDetachedFromWindow();
    }

    /**
     * Para el bucle cuando la ventana deja de verse y lo rearranca al volver.
     *
     * @param visibility View.VISIBLE, INVISIBLE o GONE de la ventana
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            arrancarBucleAmbiente();
        } else {
            pararBucleAmbiente();
        }
    }

    /**
     * Arranca el repintado a ~30 fps para nubes, respiración y matas.
     * No arranca si la escena está congelada tras el fin del combate.
     */
    private void arrancarBucleAmbiente() {
        if (bucleActivo || congelado) {
            return;
        }
        bucleActivo = true;
        ultimoFrame = -1;
        removeCallbacks(bucleAmbiente);
        post(bucleAmbiente);
    }

    /**
     * Detiene el bucle ambiental (vista oculta, desmontada o escena congelada).
     */
    private void pararBucleAmbiente() {
        bucleActivo = false;
        removeCallbacks(bucleAmbiente);
    }

    /**
     * Cuánto ha pasado de una animación, de 0 a 1.
     *
     * @param inicio   reloj en que empezó, o -1 si no ha empezado
     * @param duracion lo que dura en ms
     * @param ahora    reloj actual en ms
     * @return progreso 0-1, o -1 si no está en marcha
     */
    private float progreso(long inicio, long duracion, long ahora) {
        if (inicio < 0) {
            return -1f;
        }
        long transcurrido = ahora - inicio;
        if (transcurrido >= duracion) {
            return -1f;
        }
        return (float) transcurrido / (float) duracion;
    }

    /**
     * Suaviza el progreso para que la animación empiece rápida y frene al final.
     *
     * @param t progreso lineal 0-1
     * @return progreso suavizado 0-1 (cúbica invertida)
     */
    private float suavizar(float t) {
        float inverso = 1f - t;
        return 1f - inverso * inverso * inverso;
    }

    /**
     * @param esJugador true para la criatura del jugador, false para la del rival
     * @param w         ancho de la vista
     * @return coordenada x del centro de esa criatura
     */
    private float centroX(boolean esJugador, int w) {
        if (esJugador) {
            return w * POS_JUGADOR_X;
        }
        return w * POS_RIVAL_X;
    }

    /**
     * @param esJugador true para la criatura del jugador, false para la del rival
     * @param h         alto de la vista
     * @return coordenada y del centro de esa criatura
     */
    private float centroY(boolean esJugador, int h) {
        if (esJugador) {
            return h * POS_JUGADOR_Y;
        }
        return h * POS_RIVAL_Y;
    }

    /**
     * @param esJugador true para la criatura del jugador, false para la del rival
     * @param w         ancho de la vista
     * @param h         alto de la vista
     * @return radio de dibujo de esa criatura (la del jugador se ve más grande porque está delante)
     */
    private float radio(boolean esJugador, int w, int h) {
        if (esJugador) {
            return Math.min(w, h) * RADIO_JUGADOR;
        }
        return Math.min(w, h) * RADIO_RIVAL;
    }

    /**
     * Un fotograma completo: avanza el tiempo, pinta el escenario con la
     * sacudida de cámara, las criaturas, partículas y textos, y encima los
     * paneles quietos; en derrota lo envuelve todo en la capa desaturada.
     *
     * @param canvas dónde dibujar
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }

        long ahora = SystemClock.uptimeMillis();
        actualizarFrame(ahora);
        float fase = (float) (ahora % CICLO_FASE_MS) / (float) CICLO_FASE_MS;
        float progresoDerrota = progresoDerrotaFinal(ahora);
        boolean dibujarDerrota = (progresoDerrota >= 0f);

        if (dibujarDerrota) {
            int filtro = (int) (progresoDerrota * PASOS_DERROTA);
            if (filtro < 0) {
                filtro = 0;
            }
            if (filtro > PASOS_DERROTA) {
                filtro = PASOS_DERROTA;
            }
            paintCapa.setAlpha(255);
            paintCapa.setColorFilter(filtrosDerrota[filtro]);
            canvas.saveLayer(0, 0, w, h, paintCapa);
        }

        float camX = 0f;
        float camY = 0f;
        float tGolpe = progreso(inicioGolpe, DURACION_GOLPE_MS, ahora);
        boolean animandoGolpe = tGolpe >= 0f;
        if (animandoGolpe) {
            float queda = 1f - tGolpe;
            camX = (float) Math.sin((ahora - inicioGolpe) / SACUDIDA_PERIODO_X_MS) * w * SACUDIDA_AMPLITUD_X * intensidadGolpe * queda;
            camY = (float) Math.cos((ahora - inicioGolpe) / SACUDIDA_PERIODO_Y_MS) * h * SACUDIDA_AMPLITUD_Y * intensidadGolpe * queda;
        }

        // la sacudida de cámara solo afecta al escenario: los paneles se dibujan
        // después del restore() para que no bailen con el golpe
        canvas.save();
        canvas.translate(camX, camY);
        escenario.dibujarFondo(canvas, w, h, fase);

        if (batalla != null && criaturaMostradaJugador != null && criaturaMostradaRival != null) {
            escenario.dibujarPlataformas(canvas, w, h);
            // dibujo cada criatura mientras le quede vida; en el golpe que la
            // debilita la sigo dibujando durante la sacudida y la caída para
            // que el impacto se vea antes de que desaparezca
            dibujarCriaturas(canvas, w, h, ahora, fase, tGolpe);
            particulas.dibujar(canvas);
            textos.dibujar(canvas, h);
        }
        canvas.restore();

        // paneles de información, fuera de la sacudida
        if (batalla != null && criaturaMostradaJugador != null && criaturaMostradaRival != null) {
            panelRival.dibujarPanel(canvas, criaturaMostradaRival, hpMostradoRival, hpFantasmaRival,
                    batalla.getEquipoRival(), w * PANEL_RIVAL_X, h * PANEL_RIVAL_Y, w * PANEL_ANCHO);
            panelJugador.dibujarPanel(canvas, criaturaMostradaJugador, hpMostradoJugador, hpFantasmaJugador,
                    batalla.getEquipoJugador(), w * PANEL_JUGADOR_X, h * PANEL_JUGADOR_Y, w * PANEL_ANCHO);
        }

        if (dibujarDerrota) {
            canvas.restore();
            paintCapa.setAlpha(255);
            paintCapa.setColorFilter(null);
            // velo oscuro que va de 0 a ALFA_VELO_DERROTA con el progreso de la derrota
            int alfaVelo = (int) (ALFA_VELO_DERROTA * progresoDerrota);
            paint.setColor(Color.argb(alfaVelo, Color.red(colorVeloDerrota), Color.green(colorVeloDerrota), Color.blue(colorVeloDerrota)));
            canvas.drawRect(0, 0, w, h, paint);
        }

        // repinto a ritmo de pantalla solo mientras algo se mueve de verdad;
        // el resto del tiempo basta con el bucle ambiental a 30 fps
        if (hayEfectosActivos() || animandoGolpe || hayAnimacionesDeLado(ahora)
                || (progresoDerrota >= 0f && !derrotaFinal)) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * @return true si queda alguna partícula o texto vivo que haya que seguir animando
     */
    private boolean hayEfectosActivos() {
        if (particulas.hayVivas()) {
            return true;
        }
        return textos.hayVivos();
    }

    /**
     * @param ahora reloj actual en ms
     * @return true si alguna entrada, KO, embestida, esquiva o barra de vida sigue en marcha
     */
    private boolean hayAnimacionesDeLado(long ahora) {
        if (progreso(inicioEntradaJugador, DURACION_ENTRADA_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioEntradaRival, DURACION_ENTRADA_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioKOJugador, DURACION_KO_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioKORival, DURACION_KO_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioAtaque, DURACION_ATAQUE_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioEsquivaJugador, DURACION_ESQUIVA_MS, ahora) >= 0f) {
            return true;
        }
        if (progreso(inicioEsquivaRival, DURACION_ESQUIVA_MS, ahora) >= 0f) {
            return true;
        }
        if (Math.abs(hpMostradoJugador - hpMostradoEventoJugador) > 0.5f || Math.abs(hpMostradoRival - hpMostradoEventoRival) > 0.5f) {
            return true;
        }
        if (Math.abs(hpFantasmaJugador - hpMostradoJugador) > 0.5f || Math.abs(hpFantasmaRival - hpMostradoRival) > 0.5f) {
            return true;
        }
        return false;
    }

    /**
     * Progreso de la desaturación de derrota. Cuando termina, congelo la escena:
     * el diálogo de fin ya está encima y no hace falta seguir pintando la capa
     * con filtro a pantalla completa en cada fotograma.
     *
     * @param ahora reloj actual en ms
     * @return 0-1 mientras avanza, 1 al acabar, -1 si no hay derrota
     */
    private float progresoDerrotaFinal(long ahora) {
        if (derrotaFinal) {
            return 1f;
        }
        if (inicioDerrotaFinal < 0) {
            return -1f;
        }
        long transcurrido = ahora - inicioDerrotaFinal;
        if (transcurrido >= DURACION_DERROTA_MS) {
            derrotaFinal = true;
            congelarEscena();
            return 1f;
        }
        return suavizar((float) transcurrido / (float) DURACION_DERROTA_MS);
    }

    /**
     * @param ahora reloj actual en ms
     * @return true mientras dura la pausa de impacto de un crítico o KO
     */
    private boolean enHitStop(long ahora) {
        if (inicioHitStop < 0) {
            return false;
        }
        return (ahora - inicioHitStop) < DURACION_HIT_STOP_MS;
    }

    /**
     * Avanza vida, nubes, partículas y textos según el tiempo real transcurrido.
     * Durante el hit-stop no avanza nada: el fotograma se queda clavado.
     *
     * @param ahora reloj actual en ms
     */
    private void actualizarFrame(long ahora) {
        if (ultimoFrame < 0) {
            ultimoFrame = ahora;
            return;
        }
        long dt = ahora - ultimoFrame;
        ultimoFrame = ahora;
        if (enHitStop(ahora)) {
            return;
        }
        // las nubes reciben el dt entero: un parón largo solo las adelanta
        escenario.avanzarNubes(dt);
        // el resto lo acoto para que un parón no dispare las partículas de golpe
        if (dt > DT_MAXIMO_MS) {
            dt = DT_MAXIMO_MS;
        }

        actualizarVida(dt);
        particulas.actualizar(dt);
        textos.actualizar(dt);
    }

    /**
     * La vida mostrada persigue a la del evento en ~350 ms y el fantasma blanco se recoge en ~600 ms.
     *
     * @param dt milisegundos desde el fotograma anterior (acotados a DT_MAXIMO_MS)
     */
    private void actualizarVida(long dt) {
        float pasoVida = (float) dt / (float) TIEMPO_PERSECUCION_VIDA_MS;
        if (pasoVida > 1f) {
            pasoVida = 1f;
        }
        hpMostradoJugador = hpMostradoJugador + (hpMostradoEventoJugador - hpMostradoJugador) * suavizar(pasoVida);
        hpMostradoRival = hpMostradoRival + (hpMostradoEventoRival - hpMostradoRival) * suavizar(pasoVida);

        float pasoFantasma = (float) dt / (float) TIEMPO_FANTASMA_MS;
        if (pasoFantasma > 1f) {
            pasoFantasma = 1f;
        }
        // el fantasma nunca va por debajo de la vida mostrada: si sube (curación) salta con ella
        if (hpFantasmaJugador < hpMostradoJugador) {
            hpFantasmaJugador = hpMostradoJugador;
        } else {
            hpFantasmaJugador = hpFantasmaJugador + (hpMostradoJugador - hpFantasmaJugador) * suavizar(pasoFantasma);
        }
        if (hpFantasmaRival < hpMostradoRival) {
            hpFantasmaRival = hpMostradoRival;
        } else {
            hpFantasmaRival = hpFantasmaRival + (hpMostradoRival - hpFantasmaRival) * suavizar(pasoFantasma);
        }
    }

    /**
     * Dibuja primero al rival (al fondo) y luego al jugador (delante).
     *
     * @param canvas dónde dibujar
     * @param w      ancho de la vista
     * @param h      alto de la vista
     * @param ahora  reloj actual en ms
     * @param fase   progreso cíclico de 0 a 1 para la respiración
     * @param tGolpe progreso 0-1 de la sacudida actual, o -1 si no hay golpe
     */
    private void dibujarCriaturas(Canvas canvas, int w, int h, long ahora, float fase, float tGolpe) {
        dibujarLado(canvas, false, ahora, fase, tGolpe, w, h);
        dibujarLado(canvas, true, ahora, fase, tGolpe, w, h);
    }

    // ---- Lo que cambia entre el lado del jugador y el del rival ----

    /**
     * @param esJugador true para el lado del jugador
     * @return la criatura que se está enseñando en ese lado (la de la foto del evento)
     */
    private Criatura criaturaMostrada(boolean esJugador) {
        if (esJugador) {
            return criaturaMostradaJugador;
        }
        return criaturaMostradaRival;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return la vida de la foto del evento en ese lado
     */
    private int hpMostradoEvento(boolean esJugador) {
        if (esJugador) {
            return hpMostradoEventoJugador;
        }
        return hpMostradoEventoRival;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return reloj en que empezó la entrada de ese lado, o -1
     */
    private long inicioEntrada(boolean esJugador) {
        if (esJugador) {
            return inicioEntradaJugador;
        }
        return inicioEntradaRival;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return reloj en que empezó la caída de ese lado, o -1
     */
    private long inicioKO(boolean esJugador) {
        if (esJugador) {
            return inicioKOJugador;
        }
        return inicioKORival;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return reloj en que empezó la esquiva de ese lado, o -1
     */
    private long inicioEsquiva(boolean esJugador) {
        if (esJugador) {
            return inicioEsquivaJugador;
        }
        return inicioEsquivaRival;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return el efecto de Evento que significa "golpe a este lado"
     */
    private int efectoGolpeContra(boolean esJugador) {
        if (esJugador) {
            return Evento.GOLPE_JUGADOR;
        }
        return Evento.GOLPE_RIVAL;
    }

    /**
     * @param esJugador true para el lado del jugador
     * @return +1 si ese lado mira hacia la derecha (jugador) y -1 si mira hacia la izquierda (rival)
     */
    private float sentido(boolean esJugador) {
        if (esJugador) {
            return 1f;
        }
        return -1f;
    }

    /**
     * Dibuja la criatura de un lado aplicando entrada, ataque, golpe recibido,
     * esquiva o KO. Todo lo que depende del lado (criatura, vida, relojes,
     * posición, sentido y espejo) lo saco aquí de esJugador con los ayudantes
     * de arriba, para que la llamada no tenga que pasar quince valores.
     *
     * @param canvas    dónde dibujar
     * @param esJugador true para la criatura del jugador, false para la del rival
     * @param ahora     reloj actual en ms
     * @param fase      progreso cíclico de 0 a 1 para la respiración
     * @param tGolpe    progreso 0-1 de la sacudida en curso, o -1 (solo cuenta si el golpe es contra este lado)
     * @param w         ancho de la vista
     * @param h         alto de la vista
     */
    private void dibujarLado(Canvas canvas, boolean esJugador, long ahora, float fase, float tGolpe, int w, int h) {
        Criatura criatura = criaturaMostrada(esJugador);
        int hp = hpMostradoEvento(esJugador);
        float cx = centroX(esJugador, w);
        float cy = centroY(esJugador, h);
        float r = radio(esJugador, w, h);
        float sentido = sentido(esJugador);
        float tEntrada = progreso(inicioEntrada(esJugador), DURACION_ENTRADA_MS, ahora);
        float tKO = progreso(inicioKO(esJugador), DURACION_KO_MS, ahora);
        float tAtaque = progreso(inicioAtaque, DURACION_ATAQUE_MS, ahora);
        float tEsquiva = progreso(inicioEsquiva(esJugador), DURACION_ESQUIVA_MS, ahora);
        boolean hitStop = enHitStop(ahora);
        // el golpe solo mueve al lado que lo recibe
        boolean enGolpe = tGolpe >= 0f && objetivoGolpe == efectoGolpeContra(esJugador);

        float escalaX = 1f;
        float escalaY = 1f;
        float desplazamiento = 0f;
        float desplazamientoAtaque = 0f;
        float desplazamientoEsquiva = 0f;
        float desplazamientoGolpe = 0f;
        int alfa = 255;
        float giroKO = 0f;
        float posY = cy; // la caída del KO la baja, así que trabajo con una copia
        boolean visible = hp > 0 || enGolpe;

        // la silueta blanca dura solo los primeros 80 ms del golpe (o todo el
        // hit-stop); el resto de la sacudida la criatura se ve con sus colores
        boolean golpeada = false;
        if (enGolpe) {
            if (tGolpe < (float) DURACION_FLASH_GOLPE_MS / (float) DURACION_GOLPE_MS || hitStop) {
                golpeada = true;
            }
            // retroceso alejándose del atacante (hacia atrás según el sentido), que se recupera con suavizar
            float retroceso = w * RETROCESO_GOLPE * (1f - suavizar(tGolpe));
            desplazamientoGolpe = -sentido * retroceso;
        }

        if (tEntrada >= 0f) {
            // entra pequeña y creciendo, con un pequeño rebote al final, desde
            // detrás de su lado (el jugador desde la izquierda, el rival desde la derecha)
            float t = suavizar(tEntrada);
            escalaX = ESCALA_ENTRADA_INICIAL + CRECIMIENTO_ENTRADA * t;
            escalaY = ESCALA_ENTRADA_INICIAL + CRECIMIENTO_ENTRADA * t;
            if (t > INICIO_REBOTE_ENTRADA) {
                escalaX = escalaX - (t - INICIO_REBOTE_ENTRADA) * PENDIENTE_REBOTE_ENTRADA;
                escalaY = escalaY - (t - INICIO_REBOTE_ENTRADA) * PENDIENTE_REBOTE_ENTRADA;
            }
            desplazamiento = (1f - t) * (-sentido * w * DISTANCIA_ENTRADA);
            visible = true;
        } else if (tKO >= 0f) {
            // cae aplastándose, girando hacia atrás y desvaneciéndose
            float t = suavizar(tKO);
            escalaY = 1f - APLASTE_KO * t;
            escalaX = 1f - ESTRECHAMIENTO_KO * t;
            alfa = (int) (255 * (1f - t));
            posY = posY + r * CAIDA_KO * t;
            giroKO = -sentido * GIRO_KO_GRADOS * t;
            visible = true;
        }

        if (detalleAtaque != null && tAtaque >= 0f) {
            if (detalleAtaque.atacaJugador() == esJugador) {
                float empuje = (float) Math.sin(tAtaque * Math.PI);
                if (detalleAtaque.esCuracion()) {
                    // al curarse no embiste: un pequeño rebote en el sitio como anticipación
                    escalaX = escalaX * (1f + REBOTE_CURACION * empuje);
                    escalaY = escalaY * (1f + REBOTE_CURACION * empuje);
                } else {
                    desplazamientoAtaque = sentido * w * DISTANCIA_EMBESTIDA * empuje;
                    escalaY = escalaY * (1f - DEFORMACION_EMBESTIDA * empuje);
                    escalaX = escalaX * (1f + ENSANCHE_EMBESTIDA * empuje);
                }
            }
        }

        if (tEsquiva >= 0f) {
            // se aparta hacia atrás
            float esquiva = (float) Math.sin(tEsquiva * Math.PI);
            desplazamientoEsquiva = -sentido * w * DISTANCIA_ESQUIVA * esquiva;
        }

        if (!visible) {
            return;
        }

        float x = cx + desplazamiento + desplazamientoAtaque + desplazamientoEsquiva + desplazamientoGolpe;
        if (tEntrada >= 0f) {
            dibujarAnilloEntrada(canvas, x, posY, r, criatura.getTipo(), tEntrada);
        }

        if (tKO >= 0f) {
            // capa con filtro gris y alfa para la caída
            paintCapa.setAlpha(alfa);
            paintCapa.setColorFilter(filtroGris);
            canvas.saveLayer(x - r * 2.1f, posY - r * 2.2f, x + r * 2.1f, posY + r * 2.1f, paintCapa);
        } else if (alfa < 255) {
            canvas.saveLayerAlpha(x - r * 2.1f, posY - r * 2.2f, x + r * 2.1f, posY + r * 2.1f, alfa);
        } else {
            canvas.save();
        }

        canvas.rotate(giroKO, x, posY + r * 0.70f);
        canvas.scale(escalaX, escalaY, x, posY);
        if (!esJugador) {
            // el rival lo giro con scale(-1, 1) para que mire hacia el jugador
            canvas.scale(-1f, 1f, x, posY);
        }
        DibujoCriatura.dibujar(canvas, paint, path, x, posY, r, criatura.getNombre(), criatura.getTipo(), golpeada, false, fase);
        canvas.restore();
        paintCapa.setAlpha(255);
        paintCapa.setColorFilter(null);
    }

    /**
     * Anillo del color del tipo que se expande y se apaga cuando entra una criatura.
     *
     * @param canvas   dónde dibujar
     * @param cx       centro x de la criatura
     * @param cy       centro y de la criatura
     * @param r        radio de la criatura
     * @param tipo     tipo del que saco el color de luz
     * @param tEntrada progreso 0-1 de la entrada
     */
    private void dibujarAnilloEntrada(Canvas canvas, float cx, float cy, float r, Tipo tipo, float tEntrada) {
        float t = suavizar(tEntrada);
        int alfa = (int) (180f * (1f - tEntrada));
        int luz = Paleta.luz(tipo);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.08f);
        paint.setColor(Color.argb(alfa, Color.red(luz), Color.green(luz), Color.blue(luz)));
        canvas.drawCircle(cx, cy + r * 0.20f, r * (0.75f + t * 0.75f), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Fuerza de la sacudida: daño relativo a FRACCION_VIDA_SACUDIDA de la vida
     * máxima del que lo recibe, multiplicado en crítico o KO y acotado entre
     * INTENSIDAD_MINIMA y 1.
     *
     * @param e       evento del golpe
     * @param detalle lectura del ataque (puede ser null)
     * @return intensidad entre INTENSIDAD_MINIMA y 1
     */
    private float calcularIntensidad(Evento e, DetalleGolpe detalle) {
        int hpMax;
        if (e.getEfecto() == Evento.GOLPE_RIVAL) {
            hpMax = e.getRival().getHpMax();
        } else {
            hpMax = e.getJugador().getHpMax();
        }

        int dano = 1;
        if (detalle != null && detalle.getDano() > 0) {
            dano = detalle.getDano();
        }
        float intensidad = dano / (hpMax * FRACCION_VIDA_SACUDIDA);
        if (intensidad > 1f) {
            intensidad = 1f;
        }
        if (detalle != null) {
            if (detalle.esCritico() || detalle.esKo()) {
                intensidad = intensidad * MULTIPLICADOR_CRITICO_SACUDIDA;
            }
        }
        if (intensidad > 1f) {
            intensidad = 1f;
        }
        if (intensidad < INTENSIDAD_MINIMA) {
            intensidad = INTENSIDAD_MINIMA;
        }
        return intensidad;
    }

    /**
     * Lanza las partículas del tipo y el número de daño sobre la criatura golpeada.
     *
     * @param e       evento del golpe
     * @param detalle lectura del ataque (puede ser null: entonces partículas Normal sin número)
     */
    private void crearImpacto(Evento e, DetalleGolpe detalle) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        // GOLPE_RIVAL significa que el golpeado es el rival
        boolean esJugador = (e.getEfecto() != Evento.GOLPE_RIVAL);
        float x = centroX(esJugador, w);
        float y = centroY(esJugador, h);
        float r = radio(esJugador, w, h);

        Tipo tipo = Tipo.NORMAL;
        int dano = 0;
        boolean critico = false;
        boolean ko = false;
        double eficacia = 1.0;
        if (detalle != null) {
            tipo = detalle.getTipo();
            dano = detalle.getDano();
            critico = detalle.esCritico();
            ko = detalle.esKo();
            eficacia = detalle.getEficacia();
        }
        particulas.crear(tipo, x, y, r, critico, ko);
        if (dano > 0) {
            textos.crearDano(dano, x - r * 0.25f, y - r * 0.95f, r, critico, eficacia);
        }
    }

    /**
     * Destellos verdes y "+N" sobre la criatura que se cura.
     *
     * @param detalle lectura del ataque, con quién se cura y la cantidad curada en dano
     */
    private void crearCuracion(DetalleGolpe detalle) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        boolean esJugador = detalle.atacaJugador();
        float x = centroX(esJugador, w);
        float y = centroY(esJugador, h);
        float r = radio(esJugador, w, h);
        particulas.crearCuracion(x, y, r);
        if (detalle.getDano() > 0) {
            textos.crearCuracion(detalle.getDano(), x - r * 0.30f, y - r * 1.05f, r);
        }
    }
}
