/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Particulas.java
 */
package com.juego.combates.vistas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.juego.combates.R;
import com.juego.combates.modelo.Tipo;

/**
 * Pool de partículas del combate: ascuas, gotas, hojas, rayos, cascotes y
 * estrellas que salen al golpear o al curarse.
 *
 * Uso arrays paralelos de tamaño fijo en vez de una lista de objetos para no
 * crear nada mientras se anima: una partícula "muerta" es simplemente un
 * hueco libre del array. VistaBatalla me crea, me avanza y me pinta.
 */
public class Particulas {

    private static final int MAX_PARTICULAS = 96;
    private static final long DURACION_PARTICULA_MS = 1500;
    /** Aceleración hacia abajo en píxeles por segundo al cuadrado. */
    private static final float GRAVEDAD_PARTICULA = 280f;
    /** Velocidad de giro en grados por segundo. */
    private static final float GIRO_PARTICULA = 240f;
    private static final int NUM_PARTICULAS_CURACION = 18;
    /** Cuántas partículas más salen en un crítico o un KO. */
    private static final int EXTRA_CRITICO = 8;
    /** Partículas por golpe según el tipo del ataque (Normal es el valor por defecto). */
    private static final int PARTICULAS_NORMAL = 16;
    private static final int PARTICULAS_FUEGO = 20;
    private static final int PARTICULAS_AGUA = 18;
    private static final int PARTICULAS_PLANTA = 18;
    private static final int PARTICULAS_ELECTRICO = 22;
    private static final int PARTICULAS_ROCA = 18;

    // receta del anillo de impacto, todo en fracción del radio de la criatura
    /** Fuerza mínima de salida y lo que suma cada una de las cinco velocidades que reparto con i % 5. */
    private static final float FUERZA_BASE = 1.6f;
    private static final float FUERZA_PASO = 0.20f;
    /** Cuánto por encima del centro nacen las partículas y el impulso extra hacia arriba. */
    private static final float ALTURA_ORIGEN = 0.15f;
    private static final float IMPULSO_ARRIBA = 0.80f;
    /** Tamaño mínimo y lo que suma cada uno de los cuatro tamaños que reparto con i % 4. */
    private static final float TAMANO_BASE = 0.035f;
    private static final float TAMANO_PASO = 0.012f;
    /** Giro inicial en grados que separa una partícula de la siguiente. */
    private static final float GIRO_INICIAL_GRADOS = 27f;
    /** Tamaño de la estrella grande de los críticos. */
    private static final float TAMANO_ESTRELLA_CRITICO = 0.08f;
    /** Altura de la cintura de la estrella de las partículas: igual que su anchura (ver DibujoCriatura.dibujarEstrella). */
    private static final float CINTURA_ESTRELLA = 0.22f;

    /** Formas posibles de una partícula; las guardo como int para poder meterlas en el array. */
    private static final int FORMA_CIRCULO = 0;
    private static final int FORMA_HOJA = 1;
    private static final int FORMA_RAYO = 2;
    private static final int FORMA_CASCOTE = 3;
    private static final int FORMA_ESTRELLA = 4;

    private Paint paint;
    private Path path;
    private int colorCritico;   // estrella amarilla clara de los críticos (colors.xml)

    private boolean[] particulaViva;
    private float[] particulaX;
    private float[] particulaY;
    private float[] particulaVx;
    private float[] particulaVy;
    private float[] particulaVida;
    private float[] particulaTam;
    private float[] particulaGiro;
    private int[] particulaColor;
    private int[] particulaForma;

    /**
     * Reserva los arrays del pool y prepara el pincel.
     *
     * @param context contexto para leer el color del crítico en colors.xml
     */
    public Particulas(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // el rayo es un trazo en zigzag: con las puntas redondeadas no se ven
        // los cortes entre segmentos (es el mismo acabado que llevan las criaturas)
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        path = new Path();
        colorCritico = context.getColor(R.color.particula_critico);

        particulaViva = new boolean[MAX_PARTICULAS];
        particulaX = new float[MAX_PARTICULAS];
        particulaY = new float[MAX_PARTICULAS];
        particulaVx = new float[MAX_PARTICULAS];
        particulaVy = new float[MAX_PARTICULAS];
        particulaVida = new float[MAX_PARTICULAS];
        particulaTam = new float[MAX_PARTICULAS];
        particulaGiro = new float[MAX_PARTICULAS];
        particulaColor = new int[MAX_PARTICULAS];
        particulaForma = new int[MAX_PARTICULAS];
    }

    /**
     * Receta de partículas por tipo (ascuas, gotas, hojas, rayos, cascotes) en
     * anillo desde el centro de la criatura; en crítico añade estrellas.
     *
     * @param tipo    tipo del ataque, que decide forma, color y cantidad
     * @param cx      centro x de la criatura golpeada
     * @param cy      centro y de la criatura golpeada
     * @param r       radio de la criatura, del que salen fuerza y tamaño
     * @param critico true si el golpe fue crítico
     * @param ko      true si el golpe debilitó a la criatura
     */
    public void crear(Tipo tipo, float cx, float cy, float r, boolean critico, boolean ko) {
        int cantidad = PARTICULAS_NORMAL;
        int forma = FORMA_CIRCULO;
        // por defecto (Normal) uso el acento del tipo; el resto usa su tono de luz
        // salvo Agua, cuyas gotas van con el acento celeste
        int color = Paleta.acento(tipo);
        if (tipo == Tipo.FUEGO) {
            cantidad = PARTICULAS_FUEGO;
            forma = FORMA_CIRCULO;
            color = Paleta.luz(Tipo.FUEGO);
        } else if (tipo == Tipo.AGUA) {
            cantidad = PARTICULAS_AGUA;
            forma = FORMA_CIRCULO;
            color = Paleta.acento(Tipo.AGUA);
        } else if (tipo == Tipo.PLANTA) {
            cantidad = PARTICULAS_PLANTA;
            forma = FORMA_HOJA;
            color = Paleta.luz(Tipo.PLANTA);
        } else if (tipo == Tipo.ELECTRICO) {
            cantidad = PARTICULAS_ELECTRICO;
            forma = FORMA_RAYO;
            color = Paleta.luz(Tipo.ELECTRICO);
        } else if (tipo == Tipo.ROCA) {
            cantidad = PARTICULAS_ROCA;
            forma = FORMA_CASCOTE;
            color = Paleta.luz(Tipo.ROCA);
        }
        if (critico || ko) {
            cantidad = cantidad + EXTRA_CRITICO;
        }
        for (int i = 0; i < cantidad; i++) {
            int indice = particulaLibre();
            if (indice < 0) {
                return;
            }
            float angulo = (float) (Math.PI * 2.0 * i / cantidad);
            float fuerza = r * (FUERZA_BASE + (i % 5) * FUERZA_PASO);
            particulaViva[indice] = true;
            particulaX[indice] = cx;
            particulaY[indice] = cy - r * ALTURA_ORIGEN;
            particulaVx[indice] = (float) Math.cos(angulo) * fuerza;
            particulaVy[indice] = (float) Math.sin(angulo) * fuerza - r * IMPULSO_ARRIBA;
            particulaVida[indice] = DURACION_PARTICULA_MS * (0.50f + (i % 4) * 0.10f);
            particulaTam[indice] = r * (TAMANO_BASE + (i % 4) * TAMANO_PASO);
            particulaGiro[indice] = i * GIRO_INICIAL_GRADOS;
            particulaColor[indice] = color;
            particulaForma[indice] = forma;
            // en crítico, una de cada cinco es una estrella grande y clara
            if (critico && i % 5 == 0) {
                particulaColor[indice] = colorCritico;
                particulaForma[indice] = FORMA_ESTRELLA;
                particulaTam[indice] = r * TAMANO_ESTRELLA_CRITICO;
            }
        }
    }

    /**
     * Dieciocho estrellas verdes que suben desde la base de la criatura curada.
     *
     * @param cx centro x de la criatura
     * @param cy centro y de la criatura
     * @param r  radio de la criatura
     */
    public void crearCuracion(float cx, float cy, float r) {
        for (int i = 0; i < NUM_PARTICULAS_CURACION; i++) {
            int indice = particulaLibre();
            if (indice < 0) {
                return;
            }
            float angulo = (float) (Math.PI * 2.0 * i / (double) NUM_PARTICULAS_CURACION);
            particulaViva[indice] = true;
            particulaX[indice] = cx + (float) Math.cos(angulo) * r * 0.45f;
            particulaY[indice] = cy + r * 0.20f;
            particulaVx[indice] = (float) Math.cos(angulo) * r * 0.30f;
            particulaVy[indice] = -r * (1.1f + (i % 3) * 0.18f);
            particulaVida[indice] = 900f + (i % 4) * 80f;
            particulaTam[indice] = r * (0.035f + (i % 3) * 0.015f);
            particulaGiro[indice] = i * 40f;
            // el verde de curación es el mismo tono de luz de Planta
            particulaColor[indice] = Paleta.luz(Tipo.PLANTA);
            particulaForma[indice] = FORMA_ESTRELLA;
        }
    }

    /**
     * @return índice de la primera partícula muerta del pool, o -1 si están todas vivas
     */
    private int particulaLibre() {
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            if (!particulaViva[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Mueve las partículas vivas con gravedad y giro, y apaga las que agotan su vida.
     *
     * @param dt milisegundos desde el fotograma anterior
     */
    public void actualizar(long dt) {
        float segundos = (float) dt / 1000f;
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            if (particulaViva[i]) {
                particulaVida[i] = particulaVida[i] - dt;
                if (particulaVida[i] <= 0f) {
                    particulaViva[i] = false;
                } else {
                    particulaX[i] = particulaX[i] + particulaVx[i] * segundos;
                    particulaY[i] = particulaY[i] + particulaVy[i] * segundos;
                    particulaVy[i] = particulaVy[i] + GRAVEDAD_PARTICULA * segundos;
                    particulaGiro[i] = particulaGiro[i] + GIRO_PARTICULA * segundos;
                }
            }
        }
    }

    /**
     * Pinta las partículas vivas del pool según su forma. Se desvanecen con la
     * vida que les queda.
     *
     * @param canvas dónde dibujar
     */
    public void dibujar(Canvas canvas) {
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            if (particulaViva[i]) {
                float t = 1f - particulaVida[i] / DURACION_PARTICULA_MS;
                int alfa = (int) (255f * (1f - t));
                if (alfa < 0) {
                    alfa = 0;
                }
                // primero el color y después el alfa: setColor recibe el ARGB
                // completo y me devolvería la partícula opaca si lo hiciera al revés
                int color = particulaColor[i];
                paint.setColor(color);
                paint.setAlpha(alfa);
                canvas.save();
                canvas.rotate(particulaGiro[i], particulaX[i], particulaY[i]);
                if (particulaForma[i] == FORMA_HOJA) {
                    // la hoja pone su propio color, así que le paso el alfa ya metido
                    DibujoCriatura.dibujarHoja(canvas, paint, path,
                            particulaX[i] - particulaTam[i], particulaY[i],
                            particulaX[i], particulaY[i] - particulaTam[i],
                            particulaX[i] + particulaTam[i], particulaY[i],
                            Color.argb(alfa, Color.red(color), Color.green(color), Color.blue(color)));
                } else if (particulaForma[i] == FORMA_RAYO) {
                    dibujarRayoParticula(canvas, particulaX[i], particulaY[i], particulaTam[i]);
                } else if (particulaForma[i] == FORMA_CASCOTE) {
                    canvas.drawRect(particulaX[i] - particulaTam[i] * 0.55f, particulaY[i] - particulaTam[i] * 0.45f,
                            particulaX[i] + particulaTam[i] * 0.55f, particulaY[i] + particulaTam[i] * 0.45f, paint);
                } else if (particulaForma[i] == FORMA_ESTRELLA) {
                    DibujoCriatura.dibujarEstrella(canvas, paint, path, particulaX[i], particulaY[i], particulaTam[i], CINTURA_ESTRELLA);
                } else {
                    canvas.drawCircle(particulaX[i], particulaY[i], particulaTam[i], paint);
                }
                canvas.restore();
            }
        }
        paint.setAlpha(255);
    }

    /**
     * Rayo en zigzag de cuatro segmentos para las partículas eléctricas.
     *
     * @param canvas dónde dibujar
     * @param cx     centro x
     * @param cy     centro y
     * @param r      mitad de la altura del rayo
     */
    private void dibujarRayoParticula(Canvas canvas, float cx, float cy, float r) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.28f);
        path.reset();
        path.moveTo(cx - r * 0.6f, cy - r);
        path.lineTo(cx + r * 0.1f, cy - r * 0.2f);
        path.lineTo(cx - r * 0.1f, cy + r * 0.1f);
        path.lineTo(cx + r * 0.6f, cy + r);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * @return true si queda alguna partícula viva que haya que seguir animando
     */
    public boolean hayVivas() {
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            if (particulaViva[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apaga todas las partículas (al empezar una batalla nueva).
     */
    public void limpiar() {
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            particulaViva[i] = false;
        }
    }
}
