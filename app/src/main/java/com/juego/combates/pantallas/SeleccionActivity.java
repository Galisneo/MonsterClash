/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  SeleccionActivity.java
 */
package com.juego.combates.pantallas;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juego.combates.R;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Fabrica;
import com.juego.combates.modelo.Movimiento;
import com.juego.combates.vistas.Animaciones;
import com.juego.combates.vistas.Paleta;
import com.juego.combates.vistas.VistaCriatura;

import java.util.ArrayList;

/**
 * Pantalla para elegir las 3 criaturas del equipo. Una tarjeta por especie
 * (item_criatura.xml) en una cuadrícula de dos columnas, que se marca y
 * desmarca al pulsarla. Arriba hay tres ranuras que se van rellenando con las
 * criaturas elegidas, en orden.
 *
 * Las tarjetas las creo con el LayoutInflater y las meto en el GridLayout.
 * Para 6 elementos no me hace falta un adaptador.
 */
public class SeleccionActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SeleccionActivity";
    /** Cuántas criaturas hay que elegir: las mismas que tiene un equipo en Fabrica. */
    private static final int TAMANO_EQUIPO = Fabrica.TAMANO_EQUIPO;

    // máximos para que las barras de estadísticas sean comparables entre especies
    private static final int MAX_PS = 220;
    private static final int MAX_ATQ = 100;
    private static final int MAX_DEF = 110;
    private static final int MAX_VEL = 110;

    /** Fundido de entrada de las tarjetas: desde cuántos dp suben, cuánto tardan y el desfase entre ellas. */
    private static final float APARECER_DP = 20f;
    private static final long APARECER_MS = 300;
    private static final long DESFASE_TARJETAS_MS = 60;
    /** Rebote al marcar una tarjeta: cuánto crece y lo que tarda en crecer y en volver. */
    private static final float ESCALA_REBOTE = 1.05f;
    private static final long REBOTE_CRECER_MS = 90;
    private static final long REBOTE_VOLVER_MS = 140;
    /** Opacidad del botón ¡Combatir! mientras no hay 3 elegidas. */
    private static final float ALFA_BOTON_APAGADO = 0.5f;

    private ArrayList<String> seleccionadas;  // nombres elegidos, en orden
    private ArrayList<View> tarjetas;         // la tarjeta i es la especie i
    private String[] especies;
    private TextView contador;
    private Button botonCombatir;
    private boolean botonListoAnterior;

    // las tres ranuras del equipo (el fondo y la criatura dibujada dentro)
    private FrameLayout slotFondo0;
    private FrameLayout slotFondo1;
    private FrameLayout slotFondo2;
    private VistaCriatura slot0;
    private VistaCriatura slot1;
    private VistaCriatura slot2;

    /**
     * Monta la pantalla e infla una tarjeta por especie en la cuadrícula.
     *
     * @param savedInstanceState estado anterior (no lo uso: la pantalla no gira)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion);
        Log.d(TAG, "onCreate");

        seleccionadas = new ArrayList<String>();
        tarjetas = new ArrayList<View>();
        especies = Fabrica.getEspecies();

        contador = findViewById(R.id.contador);
        botonCombatir = findViewById(R.id.boton_combatir);
        botonCombatir.setOnClickListener(this);

        slotFondo0 = findViewById(R.id.slot_fondo_0);
        slotFondo1 = findViewById(R.id.slot_fondo_1);
        slotFondo2 = findViewById(R.id.slot_fondo_2);
        slot0 = findViewById(R.id.slot_0);
        slot1 = findViewById(R.id.slot_1);
        slot2 = findViewById(R.id.slot_2);

        GridLayout cuadricula = findViewById(R.id.cuadricula);
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < especies.length; i++) {
            // false = no la añade todavía, la añado yo con addView
            View tarjeta = inflater.inflate(R.layout.item_criatura, cuadricula, false);
            Criatura c = Fabrica.crear(especies[i]);
            rellenarTarjeta(tarjeta, c);
            tarjeta.setTag(i); // guardo su número para saber cuál se pulsa
            tarjeta.setOnClickListener(this);
            cuadricula.addView(tarjeta);
            tarjetas.add(tarjeta);
            // aparecen en cascada, cada una un poco después que la anterior
            Animaciones.aparecer(tarjeta, APARECER_DP, DESFASE_TARJETAS_MS * i, APARECER_MS);
        }

        refrescar();
    }

    /**
     * Clics del botón de combatir y de las tarjetas. Las tarjetas no tienen id
     * propio (son seis copias del mismo layout), así que las reconozco por el
     * Tag que les puse en onCreate.
     *
     * @param v vista pulsada
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.boton_combatir) {
            alPulsarCombatir();
        } else {
            alPulsarTarjeta((Integer) v.getTag());
        }
    }

    /**
     * Rellena los datos de una tarjeta: el fondo y la marca de agua propios de
     * esta pantalla, la cabecera común (dibujo, nombre y tipo) que pone
     * TarjetaCriatura, y las barras de estadísticas y movimientos.
     *
     * @param tarjeta vista de la tarjeta
     * @param c       criatura a mostrar
     */
    private void rellenarTarjeta(View tarjeta, Criatura c) {
        int color = c.getTipo().getColor();

        View cabecera = tarjeta.findViewById(R.id.cabecera_criatura);
        cabecera.setBackgroundTintList(ColorStateList.valueOf(Paleta.fondoTarjeta(c.getTipo())));

        // emoji del tipo como marca de agua detrás de la criatura
        TextView marca = tarjeta.findViewById(R.id.marca_agua_tipo);
        marca.setText(c.getTipo().getIcono());

        TarjetaCriatura.rellenarCabecera(tarjeta, c);

        // barras de estadísticas
        rellenarBarra(tarjeta, R.id.texto_ps, R.id.barra_ps, getString(R.string.stat_ps), c.getHpMax(), MAX_PS, color);
        rellenarBarra(tarjeta, R.id.texto_atq, R.id.barra_atq, getString(R.string.stat_atq), c.getAtaque(), MAX_ATQ, color);
        rellenarBarra(tarjeta, R.id.texto_def, R.id.barra_def, getString(R.string.stat_def), c.getDefensa(), MAX_DEF, color);
        rellenarBarra(tarjeta, R.id.texto_vel, R.id.barra_vel, getString(R.string.stat_vel), c.getVelocidad(), MAX_VEL, color);

        TextView movs = tarjeta.findViewById(R.id.movimientos);
        movs.setText(listaMovimientos(c));
    }

    /**
     * Pone la etiqueta y el progreso de una barra de estadística.
     *
     * @param tarjeta  tarjeta donde están las vistas
     * @param idTexto  id del TextView de la etiqueta
     * @param idBarra  id del ProgressBar
     * @param etiqueta nombre de la estadística (PS, ATQ...)
     * @param valor    valor de la criatura
     * @param maximo   máximo de la barra
     * @param color    color del relleno
     */
    private void rellenarBarra(View tarjeta, int idTexto, int idBarra, String etiqueta, int valor, int maximo, int color) {
        TextView texto = tarjeta.findViewById(idTexto);
        texto.setText(getString(R.string.etiqueta_stat, etiqueta, valor));
        ProgressBar barra = tarjeta.findViewById(idBarra);
        barra.setMax(maximo);
        barra.setProgress(valor);
        barra.setProgressTintList(ColorStateList.valueOf(color));
    }

    /**
     * @param c criatura
     * @return sus movimientos separados por el separador de strings.xml (" · ")
     */
    private String listaMovimientos(Criatura c) {
        String separador = getString(R.string.seleccion_separador_movimientos);
        String texto = "";
        ArrayList<Movimiento> movimientos = c.getMovimientos();
        for (int i = 0; i < movimientos.size(); i++) {
            if (i > 0) {
                texto = texto + separador;
            }
            texto = texto + movimientos.get(i).getNombre();
        }
        return texto;
    }

    /**
     * Marca o desmarca la especie de esa posición, con un pequeño rebote en la tarjeta.
     *
     * @param indice posición de la tarjeta pulsada
     */
    private void alPulsarTarjeta(int indice) {
        String especie = especies[indice];
        if (seleccionadas.contains(especie)) {
            seleccionadas.remove(especie);
        } else if (seleccionadas.size() < TAMANO_EQUIPO) {
            seleccionadas.add(especie);
        } else {
            Toast.makeText(this, getString(R.string.seleccion_tope, TAMANO_EQUIPO), Toast.LENGTH_SHORT).show();
            return; // no ha cambiado nada
        }
        Log.d(TAG, "Seleccionadas: " + seleccionadas);
        rebotar(tarjetas.get(indice));
        refrescar();
    }

    /**
     * Animación de rebote: la vista crece un poco y vuelve a su tamaño.
     *
     * @param v tarjeta o botón que rebota
     */
    private void rebotar(final View v) {
        v.animate().scaleX(ESCALA_REBOTE).scaleY(ESCALA_REBOTE).setDuration(REBOTE_CRECER_MS)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(REBOTE_VOLVER_MS).start();
                    }
                }).start();
    }

    /**
     * Repinta fondos de tarjetas, insignias, ranuras, contador y botón según la selección.
     */
    private void refrescar() {
        for (int i = 0; i < tarjetas.size(); i++) {
            View tarjeta = tarjetas.get(i);
            TextView insignia = tarjeta.findViewById(R.id.insignia);
            int posicion = seleccionadas.indexOf(especies[i]); // -1 si no está elegida
            if (posicion >= 0) {
                tarjeta.setBackgroundResource(R.drawable.tarjeta_seleccionada);
                insignia.setText(String.valueOf(posicion + 1));
                insignia.setVisibility(View.VISIBLE);
            } else {
                tarjeta.setBackgroundResource(R.drawable.tarjeta);
                insignia.setVisibility(View.GONE);
            }
        }

        rellenarSlot(0, slotFondo0, slot0);
        rellenarSlot(1, slotFondo1, slot1);
        rellenarSlot(2, slotFondo2, slot2);

        contador.setText(getString(R.string.seleccion_contador, seleccionadas.size(), TAMANO_EQUIPO));

        // el botón solo se activa con el equipo completo; le bajo la opacidad para que se note
        boolean listo = (seleccionadas.size() == TAMANO_EQUIPO);
        botonCombatir.setEnabled(listo);
        if (listo) {
            botonCombatir.setAlpha(1f);
            botonCombatir.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.boton_verde)));
            if (!botonListoAnterior) {
                rebotar(botonCombatir);
            }
        } else {
            botonCombatir.setAlpha(ALFA_BOTON_APAGADO);
            botonCombatir.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.boton_apagado)));
        }
        botonListoAnterior = listo;
    }

    /**
     * Rellena una ranura del equipo con la criatura de esa posición, o la deja
     * vacía (borde discontinuo) si aún no hay.
     *
     * @param posicion 0, 1 o 2
     * @param fondo    el FrameLayout de la ranura
     * @param vista    la VistaCriatura de dentro
     */
    private void rellenarSlot(int posicion, FrameLayout fondo, VistaCriatura vista) {
        if (posicion < seleccionadas.size()) {
            Criatura c = Fabrica.crear(seleccionadas.get(posicion));
            vista.setCriatura(c);
            // el mismo círculo blanco de las tarjetas, teñido del color del tipo
            // pero translúcido (Paleta.fondoSlot le pone el alfa)
            fondo.setBackgroundResource(R.drawable.circulo_suave);
            fondo.setBackgroundTintList(ColorStateList.valueOf(Paleta.fondoSlot(c.getTipo())));
        } else {
            vista.setCriatura(null);
            fondo.setBackgroundResource(R.drawable.slot_vacio);
            fondo.setBackgroundTintList(null);
        }
    }

    /**
     * Abre la pantalla de combate pasándole los nombres del equipo como extra.
     * Paso nombres y no Criaturas porque los objetos no se pueden meter en un Intent tal cual.
     */
    private void alPulsarCombatir() {
        if (seleccionadas.size() != TAMANO_EQUIPO) {
            return;
        }
        String[] nombres = new String[seleccionadas.size()];
        for (int i = 0; i < seleccionadas.size(); i++) {
            nombres[i] = seleccionadas.get(i);
        }
        Intent intent = new Intent(this, CombateActivity.class);
        intent.putExtra(CombateActivity.EXTRA_EQUIPO, nombres);
        startActivity(intent);
        Transiciones.adelante(this);
    }
}
