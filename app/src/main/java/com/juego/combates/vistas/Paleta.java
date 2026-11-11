/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Paleta.java
 */
package com.juego.combates.vistas;

import android.graphics.Color;

import com.juego.combates.modelo.Tipo;

/**
 * Colores visuales elegidos a mano para cada tipo y algunos colores sueltos
 * que necesita DibujoCriatura (que es estático y no tiene Context para leer
 * colors.xml).
 *
 * No cambio los colores de Tipo porque forman parte del modelo y los usa el
 * arnés. Aquí solo doy tonos extra para dibujar mejor las criaturas y la UI.
 *
 * Todo es estático: la clase no se instancia, por eso el constructor es privado.
 */
public class Paleta {

    /**
     * Texto oscuro para fondos claros. Es la única definición de este tono: en
     * colors.xml solo está su versión al 80 % (texto_secundario_oscuro), que
     * usa EstiloBotonMovimiento para la segunda línea de los botones.
     */
    public static final int TEXTO_OSCURO = 0xFF20232A;
    /** Texto claro para fondos oscuros (blanco puro, como texto_blanco en colors.xml). */
    public static final int TEXTO_CLARO = 0xFFFFFFFF;
    /** Por encima de esta luminancia (0-255) el fondo se considera claro y el texto va oscuro. */
    public static final int UMBRAL_LUMINANCIA = 160;
    /** Transparencia del fondo de las tarjetas de selección: 48 de 255, un 19 %, para que el color del tipo se note sin tapar el texto. */
    public static final int ALFA_TARJETA = 48;
    /** Transparencia del fondo de las ranuras del equipo: 0x55 de 255, un 33 %, algo más fuerte que la tarjeta. */
    public static final int ALFA_SLOT = 0x55;

    // ---- Colores sueltos de DibujoCriatura ----
    // Van aquí y no en colors.xml porque DibujoCriatura es estático y no tiene
    // Context; los agrupo con nombre para no repetir números por el código.

    /** Blanco al 67 %: panza de Acuarin y también el destello blanco del golpe. */
    public static final int BRILLO_FUERTE = 0xAAFFFFFF;
    /** Blanco al 60 %: brillos pequeños de burbujas y capullo. */
    public static final int BRILLO_MEDIO = 0x99FFFFFF;
    /** Blanco al 53 %: brillo especular principal y panza de Chispin. */
    public static final int BRILLO_SUAVE = 0x88FFFFFF;
    /** Blanco al 33 %: segundo brillo especular, más pequeño. */
    public static final int BRILLO_TENUE = 0x55FFFFFF;
    /** Blanco casi opaco (93 %) para el hocico de Normalino. */
    public static final int HOCICO = 0xEEFFFFFF;
    /** Negro al 20 % para la sombra de contacto bajo la criatura. */
    public static final int SOMBRA_CONTACTO = 0x33000000;
    /** Rosa translúcido del rubor de Acuarin. */
    public static final int RUBOR = 0x55FF8FB1;
    /** Amarillo translúcido del halo eléctrico de las mejillas de Chispin. */
    public static final int HALO_MEJILLA = 0x44FFD34D;
    /** Marrón translúcido de la faceta en sombra de Rocardo. */
    public static final int FACETA_ROCA = 0x775E463B;
    /** Verde del musgo de Rocardo. */
    public static final int MUSGO = 0xFF5A9B58;
    /** Las tres lenguas de la cola de Fogarto, de más roja a más clara. */
    public static final int LLAMA_ROJA = 0xFFE0442E;
    public static final int LLAMA_NARANJA = 0xFFFF5A28;
    public static final int LLAMA_CLARA = 0xFFFF7A2F;

    /**
     * Constructor privado: la clase es solo un almacén de métodos estáticos y no
     * quiero que nadie haga new Paleta() por error.
     */
    private Paleta() {
    }

    /**
     * @param tipo tipo elemental
     * @return color base del modelo
     */
    public static int base(Tipo tipo) {
        return tipo.getColor();
    }

    /**
     * @param tipo tipo elemental
     * @return tono oscuro para contornos y sombras
     */
    public static int sombra(Tipo tipo) {
        switch (tipo) {
            case FUEGO:
                return 0xFF8F2A10;
            case AGUA:
                return 0xFF174E96;
            case PLANTA:
                return 0xFF1F6B36;
            case ELECTRICO:
                return 0xFFB26E00;
            case ROCA:
                return 0xFF5E463B;
            case NORMAL:
            default:
                return 0xFF596270;
        }
    }

    /**
     * @param tipo tipo elemental
     * @return tono claro para luces
     */
    public static int luz(Tipo tipo) {
        switch (tipo) {
            case FUEGO:
                return 0xFFFF8A4C;
            case AGUA:
                return 0xFF74C7FF;
            case PLANTA:
                return 0xFF6BD37F;
            case ELECTRICO:
                return 0xFFFFD34D;
            case ROCA:
                return 0xFFB99A81;
            case NORMAL:
            default:
                return 0xFFD4DAE2;
        }
    }

    /**
     * @param tipo tipo elemental
     * @return color de detalle propio del tipo
     */
    public static int acento(Tipo tipo) {
        switch (tipo) {
            case FUEGO:
                return 0xFFFFE3A3;
            case AGUA:
                return 0xFFB9F2FF;
            case PLANTA:
                return 0xFFFF8FB1;
            case ELECTRICO:
                return 0xFFFF6A3D;
            case ROCA:
                return 0xFF9DE0C3;
            case NORMAL:
            default:
                return 0xFFFFD6E6;
        }
    }

    /**
     * @param tipo tipo elemental
     * @return color pensado para botones activos
     */
    public static int colorBoton(Tipo tipo) {
        switch (tipo) {
            case NORMAL:
                return 0xFF5B6B85;
            case ROCA:
                return 0xFF7B6256;
            default:
                return base(tipo);
        }
    }

    /**
     * @param tipo tipo elemental
     * @return fondo translúcido para las tarjetas de selección
     */
    public static int fondoTarjeta(Tipo tipo) {
        int base = base(tipo);
        return Color.argb(ALFA_TARJETA, Color.red(base), Color.green(base), Color.blue(base));
    }

    /**
     * @param tipo tipo elemental
     * @return fondo translúcido, algo más fuerte que el de la tarjeta, para las ranuras del equipo
     */
    public static int fondoSlot(Tipo tipo) {
        int base = base(tipo);
        return Color.argb(ALFA_SLOT, Color.red(base), Color.green(base), Color.blue(base));
    }

    /**
     * Elige texto oscuro o claro según la luminancia del color.
     *
     * @param color color de fondo
     * @return color de texto legible encima
     */
    public static int textoSobre(int color) {
        // fórmula clásica de luminancia: el verde pesa más porque el ojo lo ve más brillante
        double luminancia = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color);
        if (luminancia > UMBRAL_LUMINANCIA) {
            return TEXTO_OSCURO;
        }
        return TEXTO_CLARO;
    }
}
