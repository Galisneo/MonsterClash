/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  LectorDeEventos.java
 */
package com.juego.combates.vistas;

import android.util.Log;

import com.juego.combates.batalla.Batalla;
import com.juego.combates.batalla.Evento;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Movimiento;
import com.juego.combates.modelo.Tipo;

import java.util.ArrayList;

/**
 * Lee los Evento que devuelve Batalla y saca de ellos lo que la parte visual
 * necesita: un DetalleGolpe por ataque (tipo, daño, crítico, eficacia, fallo,
 * KO, curación) y preguntas sencillas sobre cada mensaje (¿es un cambio?, ¿es
 * un fallo?...) para elegir el emoji de la burbuja.
 *
 * El motor no sabe nada de la parte visual, así que todo lo deduzco del texto
 * y del efecto de cada evento. Los trozos de texto que busco están aquí como
 * constantes, cada una con el mensaje exacto de Batalla que la genera: si
 * alguien cambia un texto del motor, este es el único sitio que hay que tocar.
 */
public class LectorDeEventos {

    private static final String TAG = "LectorDeEventos";

    // ---- Trozos de texto que genera Batalla ----

    /** Batalla: "¡X usa Y!" (anuncio de un ataque con PP). */
    static final String TEXTO_USA = " usa ";
    /** Batalla: "¡A X no le quedan PP! ¡Usa Forcejeo!" (anuncio del ataque sin PP). */
    static final String TEXTO_AVISO_FORCEJEO = "Usa " + Batalla.NOMBRE_FORCEJEO;
    /** Batalla: "¡Adelante, X!" (entra una criatura del jugador). */
    static final String TEXTO_ENTRADA_JUGADOR = "Adelante";
    /** Batalla: "¡El rival envía a X!" (entra una criatura del rival). */
    static final String TEXTO_ENTRADA_RIVAL = "envía";
    /** Batalla: "¡Vuelve, X!" (se retira la criatura del jugador en un cambio voluntario). */
    static final String TEXTO_VUELVE = "Vuelve";
    /** Batalla: "X recupera N PS." (curación con efecto). */
    static final String TEXTO_CURACION = "recupera";
    /** Batalla: "¡Golpe crítico!". */
    static final String TEXTO_CRITICO = "crítico";
    /** Batalla: "¡Pero el ataque falló!". */
    static final String TEXTO_FALLO = "falló";
    /** Batalla: "¡X se debilitó!". */
    static final String TEXTO_KO = "debilitó";
    /** Batalla: "¡Es muy eficaz!" (va al principio del mensaje). */
    static final String TEXTO_MUY_EFICAZ = "¡Es muy eficaz";
    /** Batalla: "No es muy eficaz..." (va al principio del mensaje). */
    static final String TEXTO_POCO_EFICAZ = "No es muy eficaz";
    /** Batalla: "¡Un entrenador rival te desafía!" (primer mensaje del combate). */
    static final String TEXTO_RETO = "desafía";

    /** Eficacia que apunto en el detalle cuando el motor dice "¡Es muy eficaz!". */
    private static final double EFICACIA_ALTA = 2.0;
    /** Eficacia que apunto cuando dice "No es muy eficaz...". */
    private static final double EFICACIA_BAJA = 0.5;

    /**
     * No guarda estado: solo agrupa la lectura de eventos. Lo hago clase y no
     * métodos estáticos para que CombateActivity tenga "un lector" con nombre.
     */
    public LectorDeEventos() {
    }

    /**
     * Prepara un DetalleGolpe por evento. Si un mensaje pertenece al mismo
     * ataque que el anuncio, comparte el mismo detalle (la misma referencia).
     *
     * @param eventos lista generada por el motor
     * @return lista paralela de detalles visuales (null en los mensajes que no son de un ataque)
     */
    public ArrayList<DetalleGolpe> prepararDetalles(ArrayList<Evento> eventos) {
        ArrayList<DetalleGolpe> detalles = new ArrayList<DetalleGolpe>();
        DetalleGolpe actual = null;
        for (int i = 0; i < eventos.size(); i++) {
            Evento e = eventos.get(i);
            if (esAnuncioAtaque(e)) {
                actual = leerDetalleAtaque(eventos, i);
                detalles.add(actual);
            } else {
                if (esCorteVisual(e)) {
                    actual = null;
                }
                detalles.add(actual);
            }
        }
        return detalles;
    }

    /**
     * Mira hacia delante desde el anuncio de ataque para saber cómo será el golpe.
     *
     * @param eventos lista completa
     * @param indice  índice del anuncio
     * @return detalle listo para la vista
     */
    private DetalleGolpe leerDetalleAtaque(ArrayList<Evento> eventos, int indice) {
        DetalleGolpe detalle = new DetalleGolpe();
        Evento anuncio = eventos.get(indice);
        boolean atacaJugador = (anuncio.getBando() == Evento.JUGADOR);
        detalle.setAtacaJugador(atacaJugador);

        Criatura atacante;
        Criatura defensor;
        int hpDefensorAntes;
        int hpAtacanteAntes;
        if (atacaJugador) {
            atacante = anuncio.getJugador();
            defensor = anuncio.getRival();
            hpDefensorAntes = anuncio.getHpRival();
            hpAtacanteAntes = anuncio.getHpJugador();
        } else {
            atacante = anuncio.getRival();
            defensor = anuncio.getJugador();
            hpDefensorAntes = anuncio.getHpJugador();
            hpAtacanteAntes = anuncio.getHpRival();
        }

        String nombreMovimiento = extraerNombreMovimiento(anuncio.getTexto());
        Movimiento movimiento = buscarMovimiento(atacante, nombreMovimiento);
        detalle.setTipo(tipoMovimiento(movimiento));
        // la curación la decide el Movimiento, no el texto: si la vida ya estaba
        // llena el motor dice "Pero no tiene efecto..." y no aparece "recupera",
        // pero la criatura no debe embestir igualmente
        if (movimiento != null) {
            detalle.setCuracion(movimiento.esCuracion());
        }

        for (int i = indice + 1; i < eventos.size(); i++) {
            Evento futuro = eventos.get(i);
            String texto = futuro.getTexto();
            if (esAnuncioAtaque(futuro) || esCorteVisual(futuro)) {
                break;
            }

            if (esGolpe(futuro)) {
                int hpDefensorDespues;
                if (atacaJugador) {
                    hpDefensorDespues = futuro.getHpRival();
                } else {
                    hpDefensorDespues = futuro.getHpJugador();
                }
                int dano = hpDefensorAntes - hpDefensorDespues;
                if (dano < 0) {
                    dano = 0;
                }
                detalle.setDano(dano);
            }

            if (esCuracion(futuro)) {
                int hpAtacanteDespues;
                if (atacaJugador) {
                    hpAtacanteDespues = futuro.getHpJugador();
                } else {
                    hpAtacanteDespues = futuro.getHpRival();
                }
                int curado = hpAtacanteDespues - hpAtacanteAntes;
                if (curado < 0) {
                    curado = 0;
                }
                detalle.setDano(curado);
                detalle.setCuracion(true);
            }
            if (esCritico(futuro)) {
                detalle.setCritico(true);
            }
            if (texto.startsWith(TEXTO_MUY_EFICAZ)) {
                detalle.setEficacia(EFICACIA_ALTA);
            } else if (texto.startsWith(TEXTO_POCO_EFICAZ)) {
                detalle.setEficacia(EFICACIA_BAJA);
            }
            if (esFallo(futuro)) {
                detalle.setFallo(true);
            }
            if (esKo(futuro)) {
                detalle.setKo(true);
            }
        }

        // dejo la traza en el Log para comprobar el DetalleGolpe en varios
        // combates, como pide la entrega; el defensor sale para saber contra quién iba
        Log.d(TAG, "DetalleGolpe contra " + defensor.getNombre() + ": " + detalle.resumen());
        return detalle;
    }

    /**
     * @param e evento a comprobar
     * @return true si es "¡X usa Y!" o el aviso de Forcejeo sin PP
     */
    public boolean esAnuncioAtaque(Evento e) {
        String texto = e.getTexto();
        if (texto.contains(TEXTO_USA)) {
            return true;
        }
        if (texto.contains(TEXTO_AVISO_FORCEJEO)) {
            return true;
        }
        return false;
    }

    /**
     * @param e evento a comprobar
     * @return true si es un cambio de criatura (Adelante, envía, Vuelve), que cierra el ataque en curso
     */
    private boolean esCorteVisual(Evento e) {
        return esCambio(e);
    }

    /**
     * Saca el nombre del movimiento de "¡X usa Y!" (lo que va entre " usa " y el último "!").
     *
     * @param texto texto exacto del anuncio
     * @return nombre del movimiento, "Forcejeo" si es ese aviso, o "" si no lo encuentro
     */
    private String extraerNombreMovimiento(String texto) {
        if (texto.contains(Batalla.NOMBRE_FORCEJEO)) {
            return Batalla.NOMBRE_FORCEJEO;
        }
        int inicio = texto.indexOf(TEXTO_USA);
        if (inicio < 0) {
            return "";
        }
        inicio = inicio + TEXTO_USA.length();
        int fin = texto.lastIndexOf("!");
        if (fin <= inicio) {
            fin = texto.length();
        }
        return texto.substring(inicio, fin);
    }

    /**
     * Busca el Movimiento real del atacante por nombre.
     *
     * @param atacante         criatura que ataca
     * @param nombreMovimiento nombre leído del anuncio
     * @return el movimiento, o null si es Forcejeo o no aparece en su lista
     */
    private Movimiento buscarMovimiento(Criatura atacante, String nombreMovimiento) {
        ArrayList<Movimiento> movimientos = atacante.getMovimientos();
        for (int i = 0; i < movimientos.size(); i++) {
            Movimiento m = movimientos.get(i);
            if (m.getNombre().equals(nombreMovimiento)) {
                return m;
            }
        }
        return null;
    }

    /**
     * @param movimiento movimiento encontrado, o null
     * @return su tipo; NORMAL para Forcejeo o si no se ha encontrado
     */
    private Tipo tipoMovimiento(Movimiento movimiento) {
        if (movimiento == null) {
            return Tipo.NORMAL;
        }
        return movimiento.getTipo();
    }

    // ---- Preguntas sobre un evento (para el emoji de la burbuja) ----

    /**
     * @param e evento a comprobar
     * @return true si el evento lleva el efecto de golpe (la vida del defensor ha bajado)
     */
    public boolean esGolpe(Evento e) {
        return e.getEfecto() == Evento.GOLPE_RIVAL || e.getEfecto() == Evento.GOLPE_JUGADOR;
    }

    /**
     * @param e evento a comprobar
     * @return true si entra o sale una criatura (Adelante, envía, Vuelve)
     */
    public boolean esCambio(Evento e) {
        String texto = e.getTexto();
        if (texto.contains(TEXTO_ENTRADA_JUGADOR)) {
            return true;
        }
        if (texto.contains(TEXTO_ENTRADA_RIVAL)) {
            return true;
        }
        if (texto.contains(TEXTO_VUELVE)) {
            return true;
        }
        return false;
    }

    /**
     * @param e evento a comprobar
     * @return true si es "¡Pero el ataque falló!"
     */
    public boolean esFallo(Evento e) {
        return e.getTexto().contains(TEXTO_FALLO);
    }

    /**
     * @param e evento a comprobar
     * @return true si es "X recupera N PS."
     */
    public boolean esCuracion(Evento e) {
        return e.getTexto().contains(TEXTO_CURACION);
    }

    /**
     * @param e evento a comprobar
     * @return true si es "¡Golpe crítico!"
     */
    public boolean esCritico(Evento e) {
        return e.getTexto().contains(TEXTO_CRITICO);
    }

    /**
     * @param e evento a comprobar
     * @return true si es "¡Es muy eficaz!"
     */
    public boolean esMuyEficaz(Evento e) {
        return e.getTexto().startsWith(TEXTO_MUY_EFICAZ);
    }

    /**
     * @param e evento a comprobar
     * @return true si es "No es muy eficaz..."
     */
    public boolean esPocoEficaz(Evento e) {
        return e.getTexto().startsWith(TEXTO_POCO_EFICAZ);
    }

    /**
     * @param e evento a comprobar
     * @return true si es "¡X se debilitó!"
     */
    public boolean esKo(Evento e) {
        return e.getTexto().contains(TEXTO_KO);
    }

    /**
     * @param e evento a comprobar
     * @return true si el mensaje habla de Forcejeo (el aviso de "no le quedan PP")
     */
    public boolean esForcejeo(Evento e) {
        return e.getTexto().contains(Batalla.NOMBRE_FORCEJEO);
    }

    /**
     * @param e evento a comprobar
     * @return true si es el desafío del principio del combate
     */
    public boolean esReto(Evento e) {
        return e.getTexto().contains(TEXTO_RETO);
    }
}
