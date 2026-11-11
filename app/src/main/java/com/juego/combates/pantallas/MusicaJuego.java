/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  MusicaJuego.java
 */
package com.juego.combates.pantallas;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Música y efectos chiptune originales, generados sin archivos externos. */
public final class MusicaJuego {

    private static final int FRECUENCIA_MUSICA = 11025;
    private static final int FRECUENCIA_EFECTO = 16000;
    private static final int MODO_NINGUNO = 0;
    private static final int MODO_MENU = 1;
    private static final int MODO_BATALLA = 2;
    private static final int MODO_RESULTADO = 3;
    private static final Object CANDADO = new Object();
    private static AudioTrack reproductor;
    private static AudioTrack efectoActivo;
    private static int generacion;
    private static int generacionEfecto;
    private static int siguientePistaMenu;
    private static int siguientePistaBatalla;
    private static int modoActual = MODO_NINGUNO;

    private MusicaJuego() {
    }

    /** Reproduce la música del menú y deja que el combate no la apague al salir. */
    public static void reproducirMenu() {
        int pista;
        synchronized (CANDADO) {
            pista = siguientePistaMenu;
            siguientePistaMenu = (siguientePistaMenu + 1) % 2;
        }
        reproducirModo(MODO_MENU, pista);
    }

    /** Reproduce una de las tres pistas de batalla, rotándolas entre combates. */
    public static void reproducirBatalla() {
        int pista;
        synchronized (CANDADO) {
            pista = siguientePistaBatalla;
            siguientePistaBatalla = (siguientePistaBatalla + 1) % 4;
        }
        reproducirModo(MODO_BATALLA, pista);
    }

    /** Fanfarria original para la victoria o caída oscura para la derrota. */
    public static void reproducirResultado(final boolean victoria) {
        final int miGeneracion;
        synchronized (CANDADO) {
            detenerMusicaDentro();
            modoActual = MODO_RESULTADO;
            miGeneracion = ++generacion;
        }
        Thread hilo = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] muestras = crearResultado(victoria);
                synchronized (CANDADO) {
                    if (miGeneracion != generacion || modoActual != MODO_RESULTADO) {
                        return;
                    }
                    reproductor = crearReproductor(muestras, FRECUENCIA_MUSICA, false);
                    reproductor.play();
                }
                limpiarResultado(miGeneracion, muestras.length);
            }
        }, "monster-clash-resultado");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Detiene la música solo si todavía pertenece al menú. */
    public static void detenerMenu() {
        detenerModo(MODO_MENU);
    }

    /** Detiene la música solo si todavía pertenece al combate. */
    public static void detenerBatalla() {
        detenerModo(MODO_BATALLA);
    }

    /** Detiene la música de resultado si la Activity se cierra. */
    public static void detenerResultado() {
        detenerModo(MODO_RESULTADO);
    }

    /** Reproduce un blip corto para cualquier opción pulsada. */
    public static void efectoSeleccion() {
        reproducirEfecto(crearEfecto(false));
    }

    /** Sonido breve y más marcado para confirmar una acción de combate. */
    public static void efectoConfirmacion() {
        reproducirEfecto(crearEfecto(true));
    }

    /** Efecto de ataque original de aproximadamente un segundo. */
    public static void efectoAtaque() {
        reproducirEfecto(crearEfectoAtaque());
    }

    private static void reproducirModo(final int modo, final int pista) {
        final int miGeneracion;
        synchronized (CANDADO) {
            detenerMusicaDentro();
            modoActual = modo;
            miGeneracion = ++generacion;
        }
        Thread hilo = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] muestras = crearPista(modo == MODO_BATALLA, pista);
                synchronized (CANDADO) {
                    if (miGeneracion != generacion || modoActual != modo) {
                        return;
                    }
                    reproductor = crearReproductor(muestras, FRECUENCIA_MUSICA, true);
                    reproductor.play();
                }
            }
        }, "monster-clash-musica");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void detenerModo(int modo) {
        synchronized (CANDADO) {
            if (modoActual != modo) {
                return;
            }
            modoActual = MODO_NINGUNO;
            generacion++;
            detenerMusicaDentro();
        }
    }

    private static void reproducirEfecto(final short[] muestras) {
        final int miGeneracion;
        final long duracion = (long) muestras.length * 1000L / FRECUENCIA_EFECTO + 35L;
        synchronized (CANDADO) {
            miGeneracion = ++generacionEfecto;
            if (efectoActivo != null) {
                efectoActivo.stop();
                efectoActivo.release();
            }
            efectoActivo = crearReproductor(muestras, FRECUENCIA_EFECTO);
            efectoActivo.play();
        }
        Thread limpieza = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(duracion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (CANDADO) {
                    if (miGeneracion == generacionEfecto && efectoActivo != null) {
                        efectoActivo.stop();
                        efectoActivo.release();
                        efectoActivo = null;
                    }
                }
            }
        }, "monster-clash-efecto");
        limpieza.setDaemon(true);
        limpieza.start();
    }

    private static AudioTrack crearReproductor(short[] muestras, int frecuencia) {
        return crearReproductor(muestras, frecuencia, false);
    }

    private static AudioTrack crearReproductor(short[] muestras, int frecuencia, boolean bucle) {
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(frecuencia)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(muestras.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        track.write(muestras, 0, muestras.length);
        if (bucle) {
            track.setLoopPoints(0, muestras.length, -1);
            track.setVolume(0.28f);
        } else {
            track.setVolume(frecuencia == FRECUENCIA_MUSICA ? 0.30f : 0.32f);
        }
        return track;
    }

    private static void detenerMusicaDentro() {
        if (reproductor != null) {
            reproductor.stop();
            reproductor.release();
            reproductor = null;
        }
    }

    private static void limpiarResultado(final int miGeneracion, final int muestras) {
        final long duracion = (long) muestras * 1000L / FRECUENCIA_MUSICA + 80L;
        Thread limpieza = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(duracion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (CANDADO) {
                    if (miGeneracion == generacion && modoActual == MODO_RESULTADO
                            && reproductor != null) {
                        reproductor.stop();
                        reproductor.release();
                        reproductor = null;
                        modoActual = MODO_NINGUNO;
                    }
                }
            }
        }, "monster-clash-fin-musica");
        limpieza.setDaemon(true);
        limpieza.start();
    }

    /** Cuatro arreglos de batalla y dos variaciones de menú con frases largas. */
    private static short[] crearPista(boolean combate, int pista) {
        int tempo = combate ? 150 + pista * 5 : 112;
        double beat = 60.0 / tempo;
        int pasos = 96;
        int muestras = (int) (FRECUENCIA_MUSICA * beat * pasos);
        int[][] melodias = {
                {0, 4, 7, 11, 7, 4, 2, 4, 5, 9, 12, 9, 7, 5, 4, 2, 0, 2, 5, 9, 7, 5, 2, -1},
                {7, 10, 12, 15, 12, 10, 7, 5, 3, 7, 10, 14, 10, 7, 5, 3, 0, 3, 7, 12, 10, 7, 3, 0},
                {0, 3, 7, 10, 12, 10, 7, 3, 5, 8, 12, 15, 12, 8, 5, 2, -2, 2, 5, 9, 7, 5, 2, -1},
                {12, 11, 7, 4, 9, 7, 4, 2, 5, 9, 14, 12, 9, 5, 4, 0, 2, 5, 7, 12, 10, 7, 5, 2}
        };
        int[][] bajos = {
                {0, 0, 5, 5, 7, 7, 2, 2, 0, 0, 5, 5},
                {7, 7, 3, 3, 5, 5, 0, 0, 7, 7, 10, 10},
                {0, 0, 8, 8, 5, 5, 3, 3, 0, 0, 5, 5},
                {5, 5, 2, 2, 7, 7, 0, 0, 5, 5, 9, 9}
        };
        int[] melodia = melodias[pista % melodias.length];
        int[] bajo = bajos[pista % bajos.length];
        short[] salida = new short[muestras];
        for (int i = 0; i < muestras; i++) {
            double t = i / (double) FRECUENCIA_MUSICA;
            int paso = (int) (t / beat);
            double dentro = t - paso * beat;
            int seccion = paso / 24;
            int[] cambios = combate ? new int[]{0, 2, 5, 7, 9, 4} : new int[]{0, 5, 2, 7, 4, 9};
            int tonalidad = cambios[(seccion + pista) % cambios.length];
            int indice = paso % melodia.length;
            double nota = frecuencia(261.63, melodia[indice] + tonalidad + (combate ? 12 : 0));
            double notaBajo = frecuencia(130.81, bajo[paso % bajo.length] + tonalidad);
            double valor = 0.15 * envolvente(dentro, beat * 0.72) * timbre(nota, t);
            valor += 0.09 * envolvente(dentro, beat * 0.88) * Math.sin(2 * Math.PI * notaBajo * t);
            if (paso % 4 == 0 && dentro < 0.07) {
                valor += (combate ? 0.08 : 0.045) * (1 - dentro / 0.07) * Math.sin(2 * Math.PI * 70 * t * 0.25);
            }
            if (combate && (paso % 4 == 1 || paso % 4 == 3) && dentro < 0.045) {
                valor += 0.035 * (1 - dentro / 0.045) * Math.sin(2 * Math.PI * 1800 * t * 7);
            }
            // Evita el clic de la unión: el principio y el final del bucle parten de cero.
            double borde = Math.min(1.0, Math.min(i / (double) (FRECUENCIA_MUSICA / 12),
                    (muestras - 1 - i) / (double) (FRECUENCIA_MUSICA / 12)));
            valor *= Math.max(0, borde);
            salida[i] = (short) (Math.max(-0.34, Math.min(0.34, valor)) * 32767);
        }
        return salida;
    }

    private static short[] crearEfecto(boolean confirmar) {
        int muestras = (int) (FRECUENCIA_EFECTO * (confirmar ? 0.12 : 0.075));
        short[] salida = new short[muestras];
        double inicio = confirmar ? 440 : 660;
        double fin = confirmar ? 880 : 990;
        for (int i = 0; i < muestras; i++) {
            double t = i / (double) FRECUENCIA_EFECTO;
            double progreso = i / (double) muestras;
            double hz = inicio + (fin - inicio) * progreso;
            double envolvente = (1 - progreso) * Math.min(1, i / 80.0);
            salida[i] = (short) (0.35 * envolvente * Math.sin(2 * Math.PI * hz * t) * 32767);
        }
        return salida;
    }

    /** Fanfarria de 4,2 s: golpe agudo inicial y descenso musical posterior. */
    private static short[] crearResultado(boolean victoria) {
        int pasos = 32;
        double beat = 0.26;
        int muestras = (int) (FRECUENCIA_MUSICA * beat * pasos);
        int[] alegres = {0, 4, 7, 12, 16, 19, 24, 19, 16, 12, 7, 4, 0, 0, 4, 7,
                12, 16, 19, 24, 19, 16, 12, 7, 4, 0, -5, 0, 4, 0, -5, 0};
        int[] oscuros = {12, 7, 3, 0, -4, -7, -12, -7, -4, 0, 3, 7, 12, 7, 3, 0,
                -5, -9, -12, -17, -12, -9, -5, 0, -4, -7, -12, -16, -12, -7, -4, -12};
        int[] notas = victoria ? alegres : oscuros;
        short[] salida = new short[muestras];
        for (int i = 0; i < muestras; i++) {
            double t = i / (double) FRECUENCIA_MUSICA;
            int paso = Math.min(pasos - 1, (int) (t / beat));
            double dentro = t - paso * beat;
            double nota = frecuencia(victoria ? 261.63 : 220.0, notas[paso]);
            double valor = 0.19 * envolvente(dentro, beat * 0.88) * timbre(nota, t);
            if (victoria && paso % 4 == 0) {
                valor += 0.08 * envolvente(dentro, beat * 0.65) * timbre(nota * 1.5, t);
            }
            if (!victoria) {
                valor += 0.05 * envolvente(dentro, beat * 0.95)
                        * Math.sin(2 * Math.PI * nota * 0.5 * t);
            }
            double borde = Math.min(1.0, Math.min(i / (double) (FRECUENCIA_MUSICA / 10),
                    (muestras - 1 - i) / (double) (FRECUENCIA_MUSICA / 10)));
            salida[i] = (short) (Math.max(-0.38, Math.min(0.38, valor * borde)) * 32767);
        }
        return salida;
    }

    /** Efecto original de ataque con barrido, golpe amortiguado y duración de 1 s. */
    private static short[] crearEfectoAtaque() {
        int muestras = FRECUENCIA_EFECTO;
        short[] salida = new short[muestras];
        for (int i = 0; i < muestras; i++) {
            double t = i / (double) FRECUENCIA_EFECTO;
            double progreso = i / (double) muestras;
            double frecuencia = 180 + 720 * Math.min(1, progreso * 1.7);
            double ataque = Math.min(1, i / (FRECUENCIA_EFECTO * 0.025));
            double caida = Math.max(0, 1 - progreso);
            double golpe = progreso < 0.20 ? Math.sin(2 * Math.PI * 95 * t)
                    * (1 - progreso / 0.20) : 0;
            double valor = 0.20 * ataque * caida * timbre(frecuencia, t) + 0.10 * golpe;
            salida[i] = (short) (Math.max(-0.35, Math.min(0.35, valor)) * 32767);
        }
        return salida;
    }

    private static double frecuencia(double base, int semitonos) {
        return base * Math.pow(2, semitonos / 12.0);
    }

    /** Timbre retro suave: armónicos continuos, sin saltos de onda cuadrada. */
    private static double timbre(double frecuencia, double tiempo) {
        return 0.74 * Math.sin(2 * Math.PI * frecuencia * tiempo)
                + 0.18 * Math.sin(2 * Math.PI * frecuencia * 2 * tiempo)
                + 0.08 * Math.sin(2 * Math.PI * frecuencia * 3 * tiempo);
    }

    private static double envolvente(double tiempo, double duracion) {
        double ataque = Math.min(1, tiempo / 0.018);
        double salida = Math.min(1, Math.max(0, (duracion - tiempo) / 0.055));
        return ataque * salida;
    }
}
