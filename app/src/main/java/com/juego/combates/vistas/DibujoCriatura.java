/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  DibujoCriatura.java
 */
package com.juego.combates.vistas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.juego.combates.modelo.Tipo;

/**
 * Métodos estáticos para dibujar una criatura con Canvas (sin imágenes).
 * Cada especie tiene su propia silueta y detalles, pero todas se dibujan con
 * figuras simples para que funcionen igual en el menú, la selección y el combate.
 *
 * Lo tengo aparte porque lo uso desde VistaBatalla y desde VistaCriatura, y
 * las partículas del combate reutilizan la hoja y la estrella de aquí.
 */
public class DibujoCriatura {

    /**
     * Nombres de las especies tal y como los da Fabrica.crear(): la silueta se
     * elige comparando el nombre de la criatura carácter a carácter con estas
     * constantes, así que tienen que coincidir con Fabrica.ESPECIES. Si alguna
     * no coincide, esa criatura se dibuja como Normalino sin dar error.
     */
    public static final String ESPECIE_POR_DEFECTO = "Normalino";
    private static final String ESPECIE_FOGARTO = "Fogarto";
    private static final String ESPECIE_ACUARIN = "Acuarin";
    private static final String ESPECIE_HOJARUZ = "Hojaruz";
    private static final String ESPECIE_CHISPIN = "Chispin";
    private static final String ESPECIE_ROCARDO = "Rocardo";

    /** Anchura de la cintura de la estrella (dibujarEstrella), en fracción del radio. */
    private static final float CINTURA_ANCHO = 0.22f;
    /** Altura de la cintura de la chispa de Chispin: un poco más baja que ancha. */
    private static final float CINTURA_CHISPA = 0.20f;

    private static final Matrix MATRIZ_SHADER = new Matrix();
    private static final PorterDuffColorFilter FILTRO_GOLPE = new PorterDuffColorFilter(Paleta.BRILLO_FUERTE, PorterDuff.Mode.SRC_ATOP);

    private static final RadialGradient VOLUMEN_NORMAL = crearVolumen(Tipo.NORMAL);
    private static final RadialGradient VOLUMEN_FUEGO = crearVolumen(Tipo.FUEGO);
    private static final RadialGradient VOLUMEN_AGUA = crearVolumen(Tipo.AGUA);
    private static final RadialGradient VOLUMEN_PLANTA = crearVolumen(Tipo.PLANTA);
    private static final RadialGradient VOLUMEN_ELECTRICO = crearVolumen(Tipo.ELECTRICO);
    private static final RadialGradient VOLUMEN_ROCA = crearVolumen(Tipo.ROCA);

    /**
     * Constructor privado: todo es estático y no quiero instancias.
     */
    private DibujoCriatura() {
    }

    /**
     * Dibuja una criatura concreta. El nombre decide la silueta y el tipo marca
     * la paleta. El espejo se usa para que el rival mire hacia el jugador.
     * El Paint y el Path vienen de fuera para no crear objetos en cada onDraw.
     *
     * Ojo: dejo el Paint compartido con strokeCap y strokeJoin en ROUND (los
     * necesito para las bocas y las grietas) y no los devuelvo a BUTT, así que
     * quien pinte trazos después con el mismo pincel los verá redondeados.
     *
     * @param canvas   dónde dibujar
     * @param paint    pincel reutilizable
     * @param path     path reutilizable
     * @param cx       centro x
     * @param cy       centro y
     * @param r        tamaño base
     * @param especie  nombre de la criatura (null dibuja Normalino)
     * @param tipo     tipo elemental (null usa NORMAL)
     * @param golpeada true si recibe daño
     * @param espejo   true para mirar hacia la izquierda
     * @param fase     progreso cíclico de 0 a 1
     */
    public static void dibujar(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, String especie, Tipo tipo, boolean golpeada, boolean espejo, float fase) {
        // no reasigno los parámetros: uso variables nuevas con el valor ya saneado
        Tipo tipoSeguro = tipo;
        if (tipoSeguro == null) {
            tipoSeguro = Tipo.NORMAL;
        }
        String especieSegura = especie;
        if (especieSegura == null) {
            especieSegura = ESPECIE_POR_DEFECTO;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setShader(null);
        if (golpeada) {
            paint.setColorFilter(FILTRO_GOLPE);
        } else {
            paint.setColorFilter(null);
        }

        canvas.save();
        if (espejo) {
            canvas.scale(-1f, 1f, cx, cy);
        }

        dibujarMonstruoSimple(canvas, paint, path, cx, cy, r, especieSegura, tipoSeguro, fase);
        canvas.restore();

        paint.setColorFilter(null);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Repertorio visual sencillo: formas planas, contornos limpios y pocos
     * detalles para que las criaturas parezcan sprites de aventura retro y no
     * figuras de plástico con demasiados brillos o degradados.
     */
    private static void dibujarMonstruoSimple(Canvas canvas, Paint paint, Path path,
            float cx, float cy, float r, String especie, Tipo tipo, float fase) {
        int base = Paleta.base(tipo);
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        float rebote = (float) Math.sin(fase * Math.PI * 2f) * r * 0.025f;
        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.45f, 0.20f);

        paint.setColor(sombra);
        if (ESPECIE_FOGARTO.equals(especie)) {
            path.reset();
            path.moveTo(cx + r * 0.55f, cy + r * 0.15f);
            path.lineTo(cx + r * 1.25f, cy - r * 0.10f);
            path.lineTo(cx + r * 0.78f, cy + r * 0.45f);
            path.close();
            canvas.drawPath(path, paint);
        } else if (ESPECIE_ACUARIN.equals(especie)) {
            path.reset();
            path.moveTo(cx - r * 0.65f, cy - r * 0.10f);
            path.lineTo(cx - r * 1.12f, cy - r * 0.45f);
            path.lineTo(cx - r * 0.85f, cy + r * 0.40f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + r * 0.65f, cy - r * 0.10f);
            path.lineTo(cx + r * 1.12f, cy - r * 0.45f);
            path.lineTo(cx + r * 0.85f, cy + r * 0.40f);
            path.close();
            canvas.drawPath(path, paint);
        } else if (ESPECIE_HOJARUZ.equals(especie)) {
            path.reset();
            path.moveTo(cx, cy - r * 0.55f);
            path.lineTo(cx - r * 0.25f, cy - r * 1.25f);
            path.lineTo(cx + r * 0.42f, cy - r * 0.88f);
            path.close();
            canvas.drawPath(path, paint);
        } else if (ESPECIE_CHISPIN.equals(especie)) {
            path.reset();
            path.moveTo(cx - r * 0.45f, cy - r * 0.48f);
            path.lineTo(cx - r * 0.70f, cy - r * 1.20f);
            path.lineTo(cx - r * 0.12f, cy - r * 0.78f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + r * 0.45f, cy - r * 0.48f);
            path.lineTo(cx + r * 0.70f, cy - r * 1.20f);
            path.lineTo(cx + r * 0.12f, cy - r * 0.78f);
            path.close();
            canvas.drawPath(path, paint);
        }

        paint.setColor(base);
        float cuerpo = ESPECIE_ROCARDO.equals(especie) ? 0.92f : 0.78f;
        canvas.drawOval(cx - r * cuerpo, cy - r * 0.72f + rebote,
                cx + r * cuerpo, cy + r * 0.86f + rebote, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.075f);
        paint.setColor(sombra);
        canvas.drawOval(cx - r * cuerpo, cy - r * 0.72f + rebote,
                cx + r * cuerpo, cy + r * 0.86f + rebote, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(luz);
        canvas.drawOval(cx - r * 0.34f, cy + r * 0.16f + rebote,
                cx + r * 0.34f, cy + r * 0.62f + rebote, paint);
        dibujarOjo(canvas, paint, cx - r * 0.28f, cy - r * 0.22f + rebote, r * 0.13f, r * 0.10f);
        dibujarOjo(canvas, paint, cx + r * 0.28f, cy - r * 0.22f + rebote, r * 0.13f, r * 0.10f);
        dibujarBoca(canvas, paint, cx, cy + r * 0.07f + rebote, r, sombra);
    }

    /**
     * Fogarto: pera inclinada, ceja levantada, colmillo y cola de tres lenguas
     * que parpadean con la fase. Es el veloz, así que lo dibujo nervioso.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     * @param fase   progreso cíclico de 0 a 1 para el parpadeo de la cola
     */
    private static void dibujarFogarto(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo, float fase) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);
        float pulso = (float) Math.sin(fase * Math.PI * 2f);

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.55f, 0.28f);

        // cola de tres lenguas: parpadea con fase pero no crea objetos
        dibujarLenguaFuego(canvas, paint, path, cx + r * 0.72f, cy + r * 0.30f, r * (0.95f + pulso * 0.05f), 0, sombra, Paleta.LLAMA_ROJA, acento);
        dibujarLenguaFuego(canvas, paint, path, cx + r * 0.56f, cy + r * 0.18f, r * (0.72f - pulso * 0.04f), -1, sombra, Paleta.LLAMA_NARANJA, acento);
        dibujarLenguaFuego(canvas, paint, path, cx + r * 0.88f, cy + r * 0.20f, r * (0.64f + pulso * 0.03f), 1, sombra, Paleta.LLAMA_CLARA, acento);

        canvas.save();
        canvas.rotate(-5f, cx, cy + r * 0.88f);

        // cuerpo en pera, más veloz que robusto
        path.reset();
        path.moveTo(cx - r * 0.30f, cy - r * 0.98f);
        path.cubicTo(cx - r * 1.00f, cy - r * 0.78f, cx - r * 0.95f, cy + r * 0.30f, cx - r * 0.44f, cy + r * 0.92f);
        path.cubicTo(cx - r * 0.10f, cy + r * 1.22f, cx + r * 0.78f, cy + r * 0.94f, cx + r * 0.76f, cy + r * 0.20f);
        path.cubicTo(cx + r * 0.74f, cy - r * 0.58f, cx + r * 0.22f, cy - r * 1.08f, cx - r * 0.30f, cy - r * 0.98f);
        dibujarFormaConVolumen(canvas, paint, path, cx, cy, r, tipo);
        dibujarContornoPath(canvas, paint, path, r, sombra);

        // barriga crema
        paint.setColor(acento);
        canvas.drawOval(cx - r * 0.30f, cy + r * 0.06f, cx + r * 0.42f, cy + r * 0.80f, paint);

        // ceja levantada y ojos a distinta altura
        dibujarOjo(canvas, paint, cx - r * 0.31f, cy - r * 0.25f, r * 0.15f, r * 0.10f);
        dibujarOjo(canvas, paint, cx + r * 0.26f, cy - r * 0.33f, r * 0.16f, r * 0.11f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.07f);
        paint.setColor(sombra);
        canvas.drawLine(cx + r * 0.12f, cy - r * 0.55f, cx + r * 0.48f, cy - r * 0.64f, paint);
        paint.setStyle(Paint.Style.FILL);

        // boca con colmillo
        dibujarBoca(canvas, paint, cx + r * 0.02f, cy + r * 0.04f, r, sombra);
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx + r * 0.18f, cy + r * 0.16f);
        path.lineTo(cx + r * 0.31f, cy + r * 0.16f);
        path.lineTo(cx + r * 0.23f, cy + r * 0.36f);
        path.close();
        canvas.drawPath(path, paint);

        // mechón de fuego
        paint.setColor(luz);
        path.reset();
        path.moveTo(cx - r * 0.05f, cy - r * 0.96f);
        path.lineTo(cx - r * 0.25f, cy - r * 1.30f);
        path.lineTo(cx + r * 0.16f, cy - r * 1.05f);
        path.lineTo(cx + r * 0.02f, cy - r * 1.42f);
        path.lineTo(cx + r * 0.34f, cy - r * 1.00f);
        path.close();
        canvas.drawPath(path, paint);

        dibujarBrillo(canvas, paint, cx - r * 0.28f, cy - r * 0.63f, r * 0.13f);
        canvas.restore();
    }

    /**
     * Acuarin: gota invertida con aletas, ojos grandes con rubor y burbujas que suben con la fase.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     * @param fase   progreso cíclico de 0 a 1 para las burbujas
     */
    private static void dibujarAcuarin(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo, float fase) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);
        float pulso = (float) Math.sin(fase * Math.PI * 2f);

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.45f, 0.24f);

        // aletas laterales
        paint.setColor(sombra);
        path.reset();
        path.moveTo(cx - r * 0.70f, cy - r * 0.12f);
        path.lineTo(cx - r * 1.28f, cy - r * 0.48f);
        path.lineTo(cx - r * 1.02f, cy + r * 0.24f);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx + r * 0.70f, cy - r * 0.12f);
        path.lineTo(cx + r * 1.28f, cy - r * 0.48f);
        path.lineTo(cx + r * 1.02f, cy + r * 0.24f);
        path.close();
        canvas.drawPath(path, paint);

        // cuerpo gota invertida
        path.reset();
        path.moveTo(cx, cy - r * 1.22f);
        path.cubicTo(cx + r * 0.88f, cy - r * 0.50f, cx + r * 0.96f, cy + r * 0.64f, cx + r * 0.34f, cy + r * 1.02f);
        path.cubicTo(cx - r * 0.34f, cy + r * 1.02f, cx - r * 0.96f, cy + r * 0.64f, cx - r * 0.88f, cy - r * 0.50f);
        path.cubicTo(cx - r * 0.58f, cy - r * 0.88f, cx - r * 0.22f, cy - r * 1.08f, cx, cy - r * 1.22f);
        dibujarFormaConVolumen(canvas, paint, path, cx, cy, r, tipo);
        dibujarContornoPath(canvas, paint, path, r, sombra);

        // panza y ola
        paint.setColor(Paleta.BRILLO_FUERTE);
        canvas.drawOval(cx - r * 0.40f, cy + r * 0.14f, cx + r * 0.40f, cy + r * 0.76f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.08f);
        paint.setColor(luz);
        canvas.drawArc(cx - r * 0.35f, cy + r * 0.30f, cx + r * 0.35f, cy + r * 0.60f, 180, -180, false, paint);
        paint.setStyle(Paint.Style.FILL);

        // ojos grandes y rubor
        dibujarOjo(canvas, paint, cx - r * 0.28f, cy - r * 0.28f, r * 0.19f, r * 0.14f);
        dibujarOjo(canvas, paint, cx + r * 0.28f, cy - r * 0.28f, r * 0.19f, r * 0.14f);
        paint.setColor(Paleta.RUBOR);
        canvas.drawOval(cx - r * 0.62f, cy - r * 0.08f, cx - r * 0.38f, cy + r * 0.06f, paint);
        canvas.drawOval(cx + r * 0.38f, cy - r * 0.08f, cx + r * 0.62f, cy + r * 0.06f, paint);
        dibujarBoca(canvas, paint, cx, cy + r * 0.03f, r, sombra);

        // burbujas
        paint.setColor(acento);
        canvas.drawCircle(cx + r * 0.54f, cy - r * (0.60f + pulso * 0.05f), r * 0.11f, paint);
        canvas.drawCircle(cx + r * 0.73f, cy - r * (0.38f - pulso * 0.03f), r * 0.07f, paint);
        paint.setColor(Paleta.BRILLO_MEDIO);
        canvas.drawCircle(cx + r * 0.50f, cy - r * 0.64f, r * 0.035f, paint);
        dibujarBrillo(canvas, paint, cx - r * 0.25f, cy - r * 0.70f, r * 0.12f);
    }

    /**
     * Hojaruz: cuerpo ancho y tranquilo (es el de más PS), párpados a media asta,
     * hojas a los lados, capullo rosa y patitas.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     * @param fase   progreso cíclico de 0 a 1 para mecer la hoja de arriba
     */
    private static void dibujarHojaruz(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo, float fase) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);
        float pulso = (float) Math.sin(fase * Math.PI * 2f);

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.72f, 0.28f);

        dibujarHoja(canvas, paint, path, cx - r * 0.54f, cy - r * 0.58f, cx - r * 1.34f, cy - r * 0.88f, cx - r * 0.96f, cy - r * 0.08f, sombra);
        dibujarHoja(canvas, paint, path, cx + r * 0.54f, cy - r * 0.58f, cx + r * 1.34f, cy - r * 0.88f, cx + r * 0.96f, cy - r * 0.08f, sombra);
        dibujarHoja(canvas, paint, path, cx - r * 0.02f, cy - r * 0.78f, cx - r * 0.30f, cy - r * (1.45f + pulso * 0.03f), cx + r * 0.36f, cy - r * 1.00f, luz);

        // cuerpo ancho y tranquilo
        path.reset();
        path.moveTo(cx - r * 0.86f, cy - r * 0.50f);
        path.cubicTo(cx - r * 1.18f, cy + r * 0.10f, cx - r * 0.74f, cy + r * 0.98f, cx, cy + r * 1.08f);
        path.cubicTo(cx + r * 0.74f, cy + r * 0.98f, cx + r * 1.18f, cy + r * 0.10f, cx + r * 0.86f, cy - r * 0.50f);
        path.cubicTo(cx + r * 0.50f, cy - r * 0.92f, cx - r * 0.50f, cy - r * 0.92f, cx - r * 0.86f, cy - r * 0.50f);
        dibujarFormaConVolumen(canvas, paint, path, cx, cy, r, tipo);
        dibujarContornoPath(canvas, paint, path, r, sombra);

        // patitas
        paint.setColor(sombra);
        canvas.drawOval(cx - r * 0.58f, cy + r * 0.86f, cx - r * 0.20f, cy + r * 1.13f, paint);
        canvas.drawOval(cx + r * 0.20f, cy + r * 0.86f, cx + r * 0.58f, cy + r * 1.13f, paint);

        // barriga de hoja
        dibujarHoja(canvas, paint, path, cx, cy + r * 0.08f, cx - r * 0.44f, cy + r * 0.48f, cx + r * 0.12f, cy + r * 0.80f, luz);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.05f);
        paint.setColor(sombra);
        canvas.drawLine(cx - r * 0.02f, cy + r * 0.18f, cx + r * 0.07f, cy + r * 0.70f, paint);
        paint.setStyle(Paint.Style.FILL);

        // ojos medio dormidos
        dibujarOjo(canvas, paint, cx - r * 0.30f, cy - r * 0.18f, r * 0.15f, r * 0.10f);
        dibujarOjo(canvas, paint, cx + r * 0.30f, cy - r * 0.18f, r * 0.15f, r * 0.10f);
        paint.setColor(luz);
        canvas.drawRect(cx - r * 0.47f, cy - r * 0.30f, cx - r * 0.13f, cy - r * 0.19f, paint);
        canvas.drawRect(cx + r * 0.13f, cy - r * 0.30f, cx + r * 0.47f, cy - r * 0.19f, paint);
        dibujarBoca(canvas, paint, cx, cy + r * 0.06f, r, sombra);

        // capullo rosa
        paint.setColor(acento);
        canvas.drawCircle(cx + r * 0.28f, cy - r * 1.08f, r * 0.16f, paint);
        paint.setColor(Paleta.BRILLO_MEDIO);
        canvas.drawCircle(cx + r * 0.23f, cy - r * 1.13f, r * 0.05f, paint);
    }

    /**
     * Chispin: cuerpo pequeño (0,8·r), orejas en zigzag, mejillas con halo,
     * cola rayo y un temblor con Math.sin porque es el más rápido de todos.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x (la sombra se queda aquí; el cuerpo tiembla alrededor)
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     * @param fase   progreso cíclico de 0 a 1 para el temblor y las chispas
     */
    private static void dibujarChispin(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo, float fase) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);
        float pulso = (float) Math.sin(fase * Math.PI * 2f);
        float temblor = r * 0.03f * pulso;

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.22f, 0.22f);
        // la sombra no tiembla, el cuerpo sí: uso otra variable en vez de reasignar cx
        float centroX = cx + temblor;

        // halo de mejillas eléctrico
        paint.setColor(Paleta.HALO_MEJILLA);
        canvas.drawCircle(centroX - r * 0.50f, cy + r * 0.02f, r * (0.30f + pulso * 0.03f), paint);
        canvas.drawCircle(centroX + r * 0.50f, cy + r * 0.02f, r * (0.30f - pulso * 0.03f), paint);

        // cola rayo
        paint.setColor(sombra);
        path.reset();
        path.moveTo(centroX + r * 0.46f, cy + r * 0.12f);
        path.lineTo(centroX + r * 1.22f, cy - r * 0.08f);
        path.lineTo(centroX + r * 0.90f, cy + r * 0.22f);
        path.lineTo(centroX + r * 1.32f, cy + r * 0.50f);
        path.lineTo(centroX + r * 0.52f, cy + r * 0.40f);
        path.close();
        canvas.drawPath(path, paint);

        // orejas en zigzag
        paint.setColor(luz);
        dibujarZigzagOreja(canvas, paint, path, centroX - r * 0.42f, cy - r * 0.54f, r, -1);
        dibujarZigzagOreja(canvas, paint, path, centroX + r * 0.42f, cy - r * 0.54f, r, 1);

        // cuerpo más pequeño y rápido
        ponerVolumen(paint, tipo, centroX, cy, r * 0.82f);
        canvas.drawOval(centroX - r * 0.68f, cy - r * 0.70f, centroX + r * 0.68f, cy + r * 0.90f, paint);
        paint.setShader(null);
        dibujarContornoOval(canvas, paint, centroX - r * 0.68f, cy - r * 0.70f, centroX + r * 0.68f, cy + r * 0.90f, r * 0.07f, sombra);

        paint.setColor(Paleta.BRILLO_SUAVE);
        canvas.drawOval(centroX - r * 0.28f, cy + r * 0.20f, centroX + r * 0.28f, cy + r * 0.70f, paint);

        paint.setColor(acento);
        canvas.drawCircle(centroX - r * 0.47f, cy + r * 0.02f, r * 0.12f, paint);
        canvas.drawCircle(centroX + r * 0.47f, cy + r * 0.02f, r * 0.12f, paint);

        dibujarOjo(canvas, paint, centroX - r * 0.22f, cy - r * 0.26f, r * 0.14f, r * 0.10f);
        dibujarOjo(canvas, paint, centroX + r * 0.22f, cy - r * 0.26f, r * 0.14f, r * 0.10f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.06f);
        paint.setColor(sombra);
        path.reset();
        path.moveTo(centroX - r * 0.10f, cy + r * 0.08f);
        path.lineTo(centroX, cy + r * 0.18f);
        path.lineTo(centroX + r * 0.10f, cy + r * 0.08f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        // chispitas pequeñas: la estrella no pone color, lo pongo yo antes
        paint.setColor(luz);
        dibujarEstrella(canvas, paint, path, centroX - r * 1.02f, cy - r * (0.42f + pulso * 0.04f), r * 0.16f, CINTURA_CHISPA);
        paint.setColor(acento);
        dibujarEstrella(canvas, paint, path, centroX + r * 1.00f, cy - r * (0.58f - pulso * 0.05f), r * 0.13f, CINTURA_CHISPA);
    }

    /**
     * Rocardo: polígono de cima plana con facetas, grietas, musgo y un cristal.
     * No recibe fase porque el de más defensa se queda quieto a propósito.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     */
    private static void dibujarRocardo(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.72f, 0.30f);

        // cuerpo de roca con cima plana
        path.reset();
        path.moveTo(cx - r * 0.52f, cy - r * 0.98f);
        path.lineTo(cx + r * 0.34f, cy - r * 0.98f);
        path.lineTo(cx + r * 0.92f, cy - r * 0.42f);
        path.lineTo(cx + r * 0.86f, cy + r * 0.52f);
        path.lineTo(cx + r * 0.38f, cy + r * 1.02f);
        path.lineTo(cx - r * 0.48f, cy + r * 0.98f);
        path.lineTo(cx - r * 0.98f, cy + r * 0.32f);
        path.lineTo(cx - r * 0.92f, cy - r * 0.50f);
        path.close();
        dibujarFormaConVolumen(canvas, paint, path, cx, cy, r, tipo);
        dibujarContornoPath(canvas, paint, path, r, sombra);

        // facetas
        paint.setColor(luz);
        path.reset();
        path.moveTo(cx - r * 0.48f, cy - r * 0.90f);
        path.lineTo(cx + r * 0.05f, cy - r * 0.50f);
        path.lineTo(cx - r * 0.58f, cy - r * 0.34f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Paleta.FACETA_ROCA);
        path.reset();
        path.moveTo(cx + r * 0.20f, cy + r * 0.02f);
        path.lineTo(cx + r * 0.78f, cy - r * 0.16f);
        path.lineTo(cx + r * 0.56f, cy + r * 0.56f);
        path.close();
        canvas.drawPath(path, paint);

        // cristales y musgo
        paint.setColor(acento);
        path.reset();
        path.moveTo(cx + r * 0.42f, cy - r * 1.00f);
        path.lineTo(cx + r * 0.62f, cy - r * 1.34f);
        path.lineTo(cx + r * 0.77f, cy - r * 0.94f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Paleta.MUSGO);
        canvas.drawOval(cx - r * 0.76f, cy - r * 0.56f, cx - r * 0.36f, cy - r * 0.38f, paint);
        canvas.drawOval(cx - r * 0.84f, cy - r * 0.40f, cx - r * 0.50f, cy - r * 0.25f, paint);

        // grietas
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.055f);
        paint.setColor(sombra);
        path.reset();
        path.moveTo(cx - r * 0.08f, cy - r * 0.66f);
        path.lineTo(cx - r * 0.22f, cy - r * 0.36f);
        path.lineTo(cx - r * 0.10f, cy - r * 0.10f);
        path.lineTo(cx - r * 0.26f, cy + r * 0.18f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        // ojos serios
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx - r * 0.54f, cy - r * 0.18f);
        path.lineTo(cx - r * 0.18f, cy - r * 0.25f);
        path.lineTo(cx - r * 0.26f, cy + r * 0.02f);
        path.lineTo(cx - r * 0.58f, cy + r * 0.06f);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx + r * 0.54f, cy - r * 0.18f);
        path.lineTo(cx + r * 0.18f, cy - r * 0.25f);
        path.lineTo(cx + r * 0.26f, cy + r * 0.02f);
        path.lineTo(cx + r * 0.58f, cy + r * 0.06f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Paleta.TEXTO_OSCURO);
        canvas.drawCircle(cx - r * 0.36f, cy - r * 0.08f, r * 0.055f, paint);
        canvas.drawCircle(cx + r * 0.36f, cy - r * 0.08f, r * 0.055f, paint);
    }

    /**
     * Normalino: redondo y resistente, orejas grandes, mechón, hocico y cola de bola.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     centro x
     * @param cy     centro y
     * @param r      tamaño base
     * @param tipo   tipo del que saco luz, sombra y acento
     * @param fase   progreso cíclico de 0 a 1 para la respiración
     */
    private static void dibujarNormalino(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo, float fase) {
        int sombra = Paleta.sombra(tipo);
        int luz = Paleta.luz(tipo);
        int acento = Paleta.acento(tipo);
        float pulso = (float) Math.sin(fase * Math.PI * 2f);

        dibujarSombraContacto(canvas, paint, cx, cy, r, 1.58f, 0.28f);

        // cola de bola
        paint.setColor(sombra);
        canvas.drawCircle(cx + r * 0.82f, cy + r * 0.26f, r * 0.22f, paint);
        paint.setColor(luz);
        canvas.drawCircle(cx + r * 0.86f, cy + r * 0.20f, r * 0.12f, paint);

        // orejas grandes
        paint.setColor(sombra);
        canvas.drawOval(cx - r * 0.92f, cy - r * 1.04f, cx - r * 0.32f, cy - r * 0.36f, paint);
        canvas.drawOval(cx + r * 0.32f, cy - r * 1.04f, cx + r * 0.92f, cy - r * 0.36f, paint);
        paint.setColor(acento);
        canvas.drawOval(cx - r * 0.75f, cy - r * 0.88f, cx - r * 0.46f, cy - r * 0.52f, paint);
        canvas.drawOval(cx + r * 0.46f, cy - r * 0.88f, cx + r * 0.75f, cy - r * 0.52f, paint);

        // cuerpo redondo y resistente
        ponerVolumen(paint, tipo, cx, cy, r);
        canvas.drawCircle(cx, cy, r * (0.92f + pulso * 0.01f), paint);
        paint.setShader(null);
        paint.setColor(luz);
        canvas.drawOval(cx - r * 0.45f, cy + r * 0.14f, cx + r * 0.45f, cy + r * 0.78f, paint);
        dibujarContornoOval(canvas, paint, cx - r * 0.92f, cy - r * 0.92f, cx + r * 0.92f, cy + r * 0.92f, r * 0.07f, sombra);

        // mechón
        paint.setColor(sombra);
        path.reset();
        path.moveTo(cx - r * 0.12f, cy - r * 0.82f);
        path.lineTo(cx - r * 0.04f, cy - r * 1.22f);
        path.lineTo(cx + r * 0.12f, cy - r * 0.86f);
        path.lineTo(cx + r * 0.30f, cy - r * 1.16f);
        path.lineTo(cx + r * 0.26f, cy - r * 0.76f);
        path.close();
        canvas.drawPath(path, paint);

        dibujarOjo(canvas, paint, cx - r * 0.30f, cy - r * 0.18f, r * 0.15f, r * 0.11f);
        dibujarOjo(canvas, paint, cx + r * 0.30f, cy - r * 0.18f, r * 0.15f, r * 0.11f);

        // hocico
        paint.setColor(Paleta.HOCICO);
        canvas.drawOval(cx - r * 0.28f, cy + r * 0.00f, cx + r * 0.28f, cy + r * 0.34f, paint);
        paint.setColor(Paleta.TEXTO_OSCURO);
        canvas.drawOval(cx - r * 0.08f, cy + r * 0.07f, cx + r * 0.08f, cy + r * 0.18f, paint);
        dibujarBoca(canvas, paint, cx, cy + r * 0.18f, r * 0.75f, sombra);
        dibujarBrillo(canvas, paint, cx - r * 0.32f, cy - r * 0.52f, r * 0.11f);
    }

    /**
     * Hoja con dos curvas quadTo: de a hasta la punta c pasando por el control b,
     * y vuelta por el punto medio. La uso en Hojaruz y en las partículas de Planta.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param ax     primer punto x
     * @param ay     primer punto y
     * @param bx     control x
     * @param by     control y
     * @param cx     punta x
     * @param cy     punta y
     * @param color  color de la hoja (con su alfa si hace falta)
     */
    public static void dibujarHoja(Canvas canvas, Paint paint, Path path, float ax, float ay, float bx, float by, float cx, float cy, int color) {
        paint.setColor(color);
        path.reset();
        path.moveTo(ax, ay);
        path.quadTo(bx, by, cx, cy);
        path.quadTo((ax + cx) * 0.5f, (ay + cy) * 0.5f, ax, ay);
        path.close();
        canvas.drawPath(path, paint);
    }

    /**
     * Estrella de ocho puntas con el color que ya lleve el pincel. Es la chispita
     * que flota junto a Chispin y también la partícula de crítico y de curación.
     * Las dos se diferencian solo en la cintura: la chispa de Chispin la lleva
     * algo más estrecha en vertical (CINTURA_CHISPA) y las partículas la llevan
     * igual de ancha que de alta (Particulas.CINTURA_ESTRELLA).
     *
     * @param canvas  dónde dibujar
     * @param paint   pincel con el color y el alfa ya puestos
     * @param path    path reutilizable
     * @param cx      centro x
     * @param cy      centro y
     * @param r       radio de las puntas largas
     * @param cintura altura de los vértices interiores en fracción de r (su anchura es siempre CINTURA_ANCHO)
     */
    public static void dibujarEstrella(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, float cintura) {
        path.reset();
        path.moveTo(cx, cy - r);
        path.lineTo(cx + r * CINTURA_ANCHO, cy - r * cintura);
        path.lineTo(cx + r, cy);
        path.lineTo(cx + r * CINTURA_ANCHO, cy + r * cintura);
        path.lineTo(cx, cy + r);
        path.lineTo(cx - r * CINTURA_ANCHO, cy + r * cintura);
        path.lineTo(cx - r, cy);
        path.lineTo(cx - r * CINTURA_ANCHO, cy - r * cintura);
        path.close();
        canvas.drawPath(path, paint);
    }

    /**
     * Aquí creo el shader de volumen una sola vez por tipo (radio 1, luz
     * arriba-izquierda antes del espejo) y luego lo recoloco con setLocalMatrix.
     *
     * @param tipo tipo del que sacar luz, base y sombra
     * @return degradado radial unitario
     */
    private static RadialGradient crearVolumen(Tipo tipo) {
        return new RadialGradient(-0.35f, -0.45f, 1.35f,
                new int[]{Paleta.luz(tipo), Paleta.base(tipo), Paleta.sombra(tipo)},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP);
    }

    /**
     * @param tipo tipo de la criatura
     * @return el shader de volumen ya creado para ese tipo
     */
    private static RadialGradient volumen(Tipo tipo) {
        switch (tipo) {
            case FUEGO:
                return VOLUMEN_FUEGO;
            case AGUA:
                return VOLUMEN_AGUA;
            case PLANTA:
                return VOLUMEN_PLANTA;
            case ELECTRICO:
                return VOLUMEN_ELECTRICO;
            case ROCA:
                return VOLUMEN_ROCA;
            case NORMAL:
            default:
                return VOLUMEN_NORMAL;
        }
    }

    /**
     * Escala y centra el shader de volumen del tipo sobre (cx, cy) con radio r
     * y lo deja puesto en el pincel.
     *
     * @param paint pincel al que poner el shader
     * @param tipo  tipo cuyo degradado uso
     * @param cx    centro x del degradado
     * @param cy    centro y del degradado
     * @param r     radio con el que escalo el degradado unitario
     */
    private static void ponerVolumen(Paint paint, Tipo tipo, float cx, float cy, float r) {
        MATRIZ_SHADER.reset();
        MATRIZ_SHADER.setScale(r, r);
        MATRIZ_SHADER.postTranslate(cx, cy);
        volumen(tipo).setLocalMatrix(MATRIZ_SHADER);
        // el alfa del pincel modula el degradado: si el último trazo fue la
        // sombra de contacto (alfa 0x33) el cuerpo saldría translúcido, así que
        // dejo el color opaco antes de poner el shader
        paint.setColor(Color.WHITE);
        paint.setShader(volumen(tipo));
    }

    /**
     * Rellena el path con el volumen del tipo y quita el shader.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   silueta ya construida
     * @param cx     centro x del degradado
     * @param cy     centro y del degradado
     * @param r      radio del degradado
     * @param tipo   tipo cuyo degradado uso
     */
    private static void dibujarFormaConVolumen(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, Tipo tipo) {
        ponerVolumen(paint, tipo, cx, cy, r);
        canvas.drawPath(path, paint);
        paint.setShader(null);
    }

    /**
     * Contorno de 0,07·r del color de sombra sobre el path ya rellenado.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   silueta ya rellenada
     * @param r      tamaño base, del que sale el grosor
     * @param color  color del contorno
     */
    private static void dibujarContornoPath(Canvas canvas, Paint paint, Path path, float r, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.07f);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Contorno de un óvalo, para los cuerpos que dibujo con drawOval en vez de path.
     *
     * @param canvas    dónde dibujar
     * @param paint     pincel reutilizable
     * @param izquierda borde izquierdo del óvalo
     * @param arriba    borde superior
     * @param derecha   borde derecho
     * @param abajo     borde inferior
     * @param grosor    ancho del trazo
     * @param color     color del contorno
     */
    private static void dibujarContornoOval(Canvas canvas, Paint paint, float izquierda, float arriba, float derecha, float abajo, float grosor, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(grosor);
        paint.setColor(color);
        canvas.drawOval(izquierda, arriba, derecha, abajo, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Óvalo negro translúcido bajo la criatura para que parezca apoyada en el suelo.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param cx     centro x de la criatura
     * @param cy     centro y de la criatura
     * @param r      tamaño base
     * @param ancho  anchura relativa a r
     * @param alto   altura relativa a r
     */
    private static void dibujarSombraContacto(Canvas canvas, Paint paint, float cx, float cy, float r, float ancho, float alto) {
        paint.setShader(null);
        paint.setColor(Paleta.SOMBRA_CONTACTO);
        canvas.drawOval(cx - r * ancho * 0.5f, cy + r * 0.78f, cx + r * ancho * 0.5f, cy + r * (0.78f + alto), paint);
    }

    /**
     * Ojo ovalado: blanco, pupila oscura y brillo.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param cx     centro x del ojo
     * @param cy     centro y del ojo
     * @param rx     radio horizontal
     * @param ry     radio vertical
     */
    private static void dibujarOjo(Canvas canvas, Paint paint, float cx, float cy, float rx, float ry) {
        paint.setColor(Color.WHITE);
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint);
        paint.setColor(Paleta.TEXTO_OSCURO);
        canvas.drawCircle(cx, cy + ry * 0.05f, ry * 0.55f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - rx * 0.24f, cy - ry * 0.28f, ry * 0.20f, paint);
    }

    /**
     * Sonrisa en arco con trazo redondeado.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param cx     centro x de la boca
     * @param cy     centro y de la boca
     * @param r      tamaño base del que salen el ancho y el grosor
     * @param color  color del trazo
     */
    private static void dibujarBoca(Canvas canvas, Paint paint, float cx, float cy, float r, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.06f);
        paint.setColor(color);
        canvas.drawArc(cx - r * 0.20f, cy - r * 0.04f, cx + r * 0.20f, cy + r * 0.18f, 20, 140, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Brillo especular: dos círculos blancos semitransparentes arriba a la izquierda.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param cx     centro x del brillo grande
     * @param cy     centro y del brillo grande
     * @param r      radio del brillo grande
     */
    private static void dibujarBrillo(Canvas canvas, Paint paint, float cx, float cy, float r) {
        paint.setColor(Paleta.BRILLO_SUAVE);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Paleta.BRILLO_TENUE);
        canvas.drawCircle(cx + r * 0.95f, cy + r * 0.70f, r * 0.45f, paint);
    }

    /**
     * Una lengua de fuego de la cola de Fogarto: llama exterior, núcleo claro y contorno.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel reutilizable
     * @param path   path reutilizable
     * @param cx     base x de la llama
     * @param cy     base y de la llama
     * @param r      altura aproximada
     * @param lado   -1, 0 o 1 para inclinar la punta
     * @param sombra color del contorno
     * @param color  color de la llama exterior
     * @param acento color del núcleo
     */
    private static void dibujarLenguaFuego(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, int lado, int sombra, int color, int acento) {
        float giro = lado * r * 0.12f;
        paint.setColor(color);
        path.reset();
        path.moveTo(cx - r * 0.12f, cy + r * 0.36f);
        path.cubicTo(cx + r * (0.26f + lado * 0.10f), cy - r * 0.02f, cx + r * (0.02f + lado * 0.28f), cy - r * 0.62f, cx + giro, cy - r * 0.92f);
        path.cubicTo(cx + r * (0.42f + lado * 0.12f), cy - r * 0.44f, cx + r * 0.48f, cy + r * 0.16f, cx + r * 0.12f, cy + r * 0.38f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(acento);
        path.reset();
        path.moveTo(cx, cy + r * 0.22f);
        path.cubicTo(cx + r * 0.14f, cy - r * 0.10f, cx + r * 0.06f, cy - r * 0.40f, cx + giro * 0.35f, cy - r * 0.58f);
        path.cubicTo(cx + r * 0.25f, cy - r * 0.20f, cx + r * 0.24f, cy + r * 0.12f, cx, cy + r * 0.22f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.06f);
        paint.setColor(sombra);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Oreja en zigzag de Chispin con el color ya puesto en paint.
     *
     * @param canvas dónde dibujar
     * @param paint  pincel con el color ya puesto
     * @param path   path reutilizable
     * @param cx     base x de la oreja
     * @param cy     base y de la oreja
     * @param r      tamaño base
     * @param lado   -1 izquierda, 1 derecha
     */
    private static void dibujarZigzagOreja(Canvas canvas, Paint paint, Path path, float cx, float cy, float r, int lado) {
        path.reset();
        path.moveTo(cx, cy);
        path.lineTo(cx + lado * r * 0.34f, cy - r * 0.78f);
        path.lineTo(cx + lado * r * 0.04f, cy - r * 0.58f);
        path.lineTo(cx + lado * r * 0.26f, cy - r * 1.05f);
        path.lineTo(cx - lado * r * 0.22f, cy - r * 0.16f);
        path.close();
        canvas.drawPath(path, paint);
    }
}
