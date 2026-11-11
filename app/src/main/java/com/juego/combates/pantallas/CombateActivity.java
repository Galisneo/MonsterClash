/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  CombateActivity.java
 */
package com.juego.combates.pantallas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.juego.combates.R;
import com.juego.combates.batalla.Batalla;
import com.juego.combates.batalla.Evento;
import com.juego.combates.datos.RepositorioPartidas;
import com.juego.combates.datos.Sesion;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Fabrica;
import com.juego.combates.modelo.Movimiento;
import com.juego.combates.modelo.Tipo;
import com.juego.combates.vistas.ChatCombate;
import com.juego.combates.vistas.DetalleGolpe;
import com.juego.combates.vistas.EstiloBotonMovimiento;
import com.juego.combates.vistas.LectorDeEventos;
import com.juego.combates.vistas.PanelCriatura;
import com.juego.combates.vistas.VistaBatalla;
import com.juego.combates.vistas.VistaConfeti;
import com.juego.combates.vistas.VistaCriatura;

import java.util.ArrayList;
import java.util.Random;

/**
 * Pantalla del combate. Lleva el flujo: turnos, mensajes uno a uno, botones y diálogos.
 * El layout está en activity_combate.xml y el escenario es VistaBatalla.
 *
 * Las reglas están en Batalla (sin Android); aquí solo pido "ejecuta el turno"
 * y voy enseñando los eventos que me devuelve. Para no hacerlo todo yo me
 * apoyo en tres ayudantes de vistas/: LectorDeEventos (saca el DetalleGolpe
 * de cada ataque), ChatCombate (las burbujas del historial) y
 * EstiloBotonMovimiento (los botones de colores por tipo).
 */
public class CombateActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "CombateActivity";

    /** Clave del extra con los nombres del equipo (String[]). La usa también SeleccionActivity. */
    public static final String EXTRA_EQUIPO = "com.juego.combates.EQUIPO";

    /** Movimientos que tiene cada criatura y botones que hay en el layout. */
    private static final int NUM_BOTONES_MOV = 4;
    /** Tiempo entre mensaje y mensaje del combate. */
    private static final long RETARDO_MENSAJE_MS = 700;
    /** Lo que dejo ver el KO (y la desaturación si es derrota) antes del diálogo de fin. */
    private static final long RETARDO_FIN_MS = 1200;

    /** Opacidad de un botón que no se puede pulsar ahora. */
    private static final float ALFA_DESHABILITADO = 0.45f;
    /** Opacidad, en el diálogo de equipo, de la criatura que ya está en combate. */
    private static final float ALFA_EN_COMBATE = 0.5f;
    /** Opacidad, en el diálogo de equipo, de una criatura debilitada. */
    private static final float ALFA_DEBILITADA = 0.4f;

    private Handler handler;   // para programar código en el hilo principal
    private Random rng;        // uno solo para toda la partida

    private Batalla batalla;
    private VistaBatalla vistaBatalla;
    private ChatCombate chat;
    private LectorDeEventos lector;
    private EstiloBotonMovimiento estilo;
    private Button[] botonesMov;          // los 4 botones de movimiento, en orden
    private Button botonEquipo;
    private Button botonNueva;
    private boolean[] botonPulsable;      // si cada botón de movimiento tiene PP
    private AlertDialog dialogoActual;    // para cerrarlo en onDestroy
    private String[] nombresEquipoJugador; // null = equipo al azar

    private ArrayList<Evento> eventosPendientes;        // los que estoy enseñando
    private ArrayList<DetalleGolpe> detallesPendientes; // lectura visual de cada evento
    private int indiceEvento;                           // por cuál voy

    private int turnosJugados;                 // turnos gastados por el jugador (ataques y cambios voluntarios)
    private Sesion sesion;                     // para saber si hay que guardar la partida
    private RepositorioPartidas repositorioPartidas;

    // cerrojo: mientras hay una acción o animación en marcha se ignoran los toques
    private boolean ocupado = false;

    /**
     * Enseña el evento que toca y se vuelve a programar para dentro de
     * RETARDO_MENSAJE_MS. Al acabar llama a finDeAccion(). Lo hago con Handler
     * y no con Thread.sleep porque el sleep congelaría la pantalla.
     */
    private final Runnable mostrarSiguienteEvento = new Runnable() {
        @Override
        public void run() {
            if (indiceEvento >= eventosPendientes.size()) {
                finDeAccion();
                return;
            }
            Evento e = eventosPendientes.get(indiceEvento);
            DetalleGolpe detalle = detallesPendientes.get(indiceEvento);
            chat.agregar(iconoMensaje(e) + e.getTexto(), e.getBando());
            if (lector.esAnuncioAtaque(e) && detalle != null) {
                MusicaJuego.efectoAtaque();
                vistaBatalla.anunciarAtaque(detalle);
            }
            vistaBatalla.mostrarEvento(e, detalle);
            indiceEvento++;
            // el ritmo es siempre el mismo; el hit-stop del crítico lo hace
            // VistaBatalla congelando su propio fotograma, sin tocar este Handler
            handler.postDelayed(this, RETARDO_MENSAJE_MS);
        }
    };

    /**
     * Monta la pantalla y arranca la primera batalla (con el equipo del Intent o al azar).
     *
     * @param savedInstanceState estado anterior (no lo uso: la pantalla no gira y el combate no se guarda a medias)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        rng = new Random();
        botonPulsable = new boolean[NUM_BOTONES_MOV];
        sesion = new Sesion(this);
        repositorioPartidas = new RepositorioPartidas(this);
        lector = new LectorDeEventos();
        estilo = new EstiloBotonMovimiento(this);
        nombresEquipoJugador = getIntent().getStringArrayExtra(EXTRA_EQUIPO); // null si no viene
        setContentView(R.layout.activity_combate);
        Log.d(TAG, "onCreate");
        enlazarVistas();
        iniciarBatalla();
    }

    /**
     * Busca las vistas del layout y pone los listeners.
     */
    private void enlazarVistas() {
        vistaBatalla = findViewById(R.id.vista_batalla);
        ScrollView scrollLog = findViewById(R.id.scroll_log);
        LinearLayout listaMensajes = findViewById(R.id.lista_mensajes);
        chat = new ChatCombate(this, listaMensajes, scrollLog);

        botonesMov = new Button[NUM_BOTONES_MOV];
        botonesMov[0] = findViewById(R.id.boton_mov_0);
        botonesMov[1] = findViewById(R.id.boton_mov_1);
        botonesMov[2] = findViewById(R.id.boton_mov_2);
        botonesMov[3] = findViewById(R.id.boton_mov_3);
        botonEquipo = findViewById(R.id.boton_equipo);
        botonNueva = findViewById(R.id.boton_nueva);

        for (int i = 0; i < botonesMov.length; i++) {
            botonesMov[i].setOnClickListener(this);
        }
        botonEquipo.setOnClickListener(this);
        botonNueva.setOnClickListener(this);
    }

    /**
     * Clics de los seis botones: miro el id del pulsado y delego.
     *
     * @param v botón pulsado
     */
    @Override
    public void onClick(View v) {
        MusicaJuego.efectoSeleccion();
        if (v.getId() == R.id.boton_mov_0) {
            alPulsarMovimiento(0);
        } else if (v.getId() == R.id.boton_mov_1) {
            alPulsarMovimiento(1);
        } else if (v.getId() == R.id.boton_mov_2) {
            alPulsarMovimiento(2);
        } else if (v.getId() == R.id.boton_mov_3) {
            alPulsarMovimiento(3);
        } else if (v.getId() == R.id.boton_equipo) {
            alPulsarEquipo();
        } else if (v.getId() == R.id.boton_nueva) {
            alPulsarNueva();
        }
    }

    // ---- Cerrojo ----

    /**
     * @return true si he podido coger el turno; false si ya había algo en marcha
     */
    private boolean tomarAccion() {
        if (ocupado) {
            return false;
        }
        ocupado = true;
        habilitarControles(false);
        return true;
    }

    /**
     * Suelta el cerrojo y devuelve los controles al jugador.
     */
    private void liberarAccion() {
        ocupado = false;
        habilitarControles(true);
    }

    // ---- Flujo del combate ----

    /**
     * Empieza un combate nuevo. Se llama desde onCreate y desde los diálogos de repetir.
     */
    private void iniciarBatalla() {
        MusicaJuego.reproducirBatalla();
        ocupado = true; // hasta que acabe la presentación no se puede tocar nada

        ArrayList<Criatura> equipoJugador;
        if (nombresEquipoJugador != null && nombresEquipoJugador.length > 0) {
            equipoJugador = equipoDesdeNombres(nombresEquipoJugador);
        } else {
            equipoJugador = Fabrica.equipoAleatorio(rng);
        }
        ArrayList<Criatura> equipoRival = Fabrica.equipoAleatorio(rng);
        batalla = new Batalla(equipoJugador, equipoRival, rng);
        turnosJugados = 0;
        Log.d(TAG, "Nueva batalla: " + equipoJugador.get(0).getNombre() + " vs " + equipoRival.get(0).getNombre());

        vistaBatalla.setBatalla(batalla);
        chat.limpiar();
        actualizarBotones();
        habilitarControles(false);
        mostrarEventos(batalla.eventosIntro());
    }

    /**
     * @param nombres nombres que vienen del Intent
     * @return criaturas nuevas con esos nombres
     */
    private ArrayList<Criatura> equipoDesdeNombres(String[] nombres) {
        ArrayList<Criatura> equipo = new ArrayList<Criatura>();
        for (int i = 0; i < nombres.length; i++) {
            equipo.add(Fabrica.crear(nombres[i]));
        }
        return equipo;
    }

    /**
     * Ejecuta el turno con el movimiento pulsado si no hay nada en marcha.
     *
     * @param indice posición 0-3 del movimiento
     */
    private void alPulsarMovimiento(int indice) {
        if (!tomarAccion()) {
            return;
        }
        Log.d(TAG, "Movimiento " + indice);
        turnosJugados++;
        mostrarEventos(batalla.ejecutarTurno(indice));
    }

    /**
     * Abre el diálogo de cambio voluntario de criatura.
     */
    private void alPulsarEquipo() {
        if (!tomarAccion()) {
            return;
        }
        abrirDialogoEquipo(false);
    }

    /**
     * Pide confirmación antes de empezar otra batalla.
     */
    private void alPulsarNueva() {
        if (!tomarAccion()) {
            return;
        }
        confirmarNuevaBatalla();
    }

    /**
     * Empieza a enseñar una lista de eventos.
     *
     * @param eventos los eventos del turno
     */
    private void mostrarEventos(ArrayList<Evento> eventos) {
        eventosPendientes = eventos;
        detallesPendientes = lector.prepararDetalles(eventos);
        indiceEvento = 0;
        mostrarSiguienteEvento.run();
    }

    /**
     * Elige el emoji de la burbuja según lo que cuenta el evento. Los golpes
     * los sé por el efecto; el resto se lo pregunto al LectorDeEventos, que es
     * quien conoce los textos del motor. Los emojis están en strings.xml.
     *
     * @param e evento a enseñar
     * @return el emoji con su espacio, o "" si el mensaje no tiene icono
     */
    private String iconoMensaje(Evento e) {
        if (lector.esGolpe(e)) {
            return getString(R.string.combate_icono_golpe);
        }
        if (lector.esCritico(e)) {
            return getString(R.string.combate_icono_critico);
        }
        if (lector.esMuyEficaz(e)) {
            return getString(R.string.combate_icono_eficaz);
        }
        if (lector.esPocoEficaz(e)) {
            return getString(R.string.combate_icono_poco_eficaz);
        }
        if (lector.esCuracion(e)) {
            return getString(R.string.combate_icono_curacion);
        }
        if (lector.esKo(e)) {
            return getString(R.string.combate_icono_ko);
        }
        if (lector.esFallo(e)) {
            return getString(R.string.combate_icono_fallo);
        }
        if (lector.esForcejeo(e)) {
            return getString(R.string.combate_icono_forcejeo);
        }
        if (lector.esCambio(e)) {
            return getString(R.string.combate_icono_cambio);
        }
        if (lector.esReto(e)) {
            return getString(R.string.combate_icono_reto);
        }
        return "";
    }

    /**
     * Tras cada tanda de eventos: ¿fin de combate? ¿KO propio? ¿seguimos?
     * Solo suelto el cerrojo en el último caso; en los otros hay un diálogo abierto.
     */
    private void finDeAccion() {
        vistaBatalla.invalidate();
        actualizarBotones();

        if (batalla.rivalDerrotado()) {
            mostrarFinRetrasado(true);
            return;
        }
        if (batalla.jugadorDerrotado()) {
            mostrarFinRetrasado(false);
            return;
        }
        if (batalla.getActivaJugador().estaDebilitada()) {
            abrirDialogoEquipo(true); // cambio obligatorio y gratis
            return;
        }
        liberarAccion();
    }

    /**
     * Deja RETARDO_FIN_MS para ver el KO (y la desaturación si es derrota)
     * antes del diálogo de fin. El cerrojo ocupado sigue puesto mientras tanto.
     * Justo antes de abrir el diálogo congelo el escenario, que ya no cambia.
     *
     * @param victoria true si ha ganado el jugador
     */
    private void mostrarFinRetrasado(final boolean victoria) {
        vistaBatalla.mostrarFinVisual(victoria);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                vistaBatalla.congelarEscena();
                mostrarFin(victoria);
            }
        }, RETARDO_FIN_MS);
    }

    // ---- Botones de movimiento ----

    /**
     * Actualiza texto, color y estado de los 4 botones de movimiento.
     * Sin PP en ninguno, dejo solo el botón 0 con Forcejeo y escondo los demás
     * (INVISIBLE y no GONE para que el hueco se mantenga).
     */
    private void actualizarBotones() {
        Criatura activa = batalla.getActivaJugador();
        boolean sinPP = !activa.tieneAlgunPP();
        ArrayList<Movimiento> movimientos = activa.getMovimientos();

        for (int i = 0; i < botonesMov.length; i++) {
            Button b = botonesMov[i];
            if (sinPP) {
                if (i == 0 && !movimientos.isEmpty()) {
                    b.setVisibility(View.VISIBLE);
                    estilo.estilizarBoton(b, Tipo.NORMAL);
                    estilo.ponerTextoMovimiento(b, getString(R.string.combate_forcejeo_titulo),
                            getString(R.string.combate_forcejeo_detalle));
                    botonPulsable[i] = true;
                } else {
                    b.setVisibility(View.INVISIBLE);
                    botonPulsable[i] = false;
                }
            } else if (i < movimientos.size()) {
                Movimiento m = movimientos.get(i);
                b.setVisibility(View.VISIBLE);
                // los textos salen de strings.xml en dos plantillas (título y detalle)
                // para poder darles distinto tamaño en ponerTextoMovimiento
                String titulo = getString(R.string.combate_mov_titulo, m.getTipo().getIcono(), m.getNombre());
                String detalle = getString(R.string.combate_mov_detalle, m.getTipo().getNombre(), m.getPp(), m.getPpMax());
                // primero el estilo (que decide el color del texto) y luego el texto
                if (m.tienePP()) {
                    estilo.estilizarBoton(b, m.getTipo());
                } else {
                    estilo.estilizarBotonApagado(b);
                }
                estilo.ponerTextoMovimiento(b, titulo, detalle);
                botonPulsable[i] = m.tienePP();
            } else {
                b.setVisibility(View.INVISIBLE); // por si tuviera menos de 4 movimientos
                botonPulsable[i] = false;
            }
        }
    }

    /**
     * Activa o desactiva todos los controles. Los de movimiento además necesitan PP.
     *
     * @param habilitar true para activar
     */
    private void habilitarControles(boolean habilitar) {
        for (int i = 0; i < botonesMov.length; i++) {
            boolean pulsable = habilitar && botonPulsable[i];
            ponerHabilitado(botonesMov[i], pulsable);
        }
        ponerHabilitado(botonEquipo, habilitar);
        ponerHabilitado(botonNueva, habilitar);
    }

    /**
     * Activa o apaga un botón bajándole la opacidad para que se note.
     *
     * @param b         botón
     * @param habilitar true para activarlo
     */
    private void ponerHabilitado(Button b, boolean habilitar) {
        b.setEnabled(habilitar);
        if (habilitar) {
            b.setAlpha(1f);
        } else {
            b.setAlpha(ALFA_DESHABILITADO);
        }
    }

    // ---- Diálogos ----

    /**
     * Diálogo para cambiar de criatura, con una tarjeta por miembro del equipo
     * (dibujo, tipo, barra de vida y estado). Las debilitadas y la que ya está
     * en combate salen atenuadas y no se pueden pulsar.
     *
     * @param esTrasKO true = obligatorio y gratis; false = voluntario y gasta turno
     */
    private void abrirDialogoEquipo(final boolean esTrasKO) {
        View vista = getLayoutInflater().inflate(R.layout.dialogo_equipo, null);
        TextView titulo = vista.findViewById(R.id.dlg_titulo);
        TextView subtitulo = vista.findViewById(R.id.dlg_subtitulo);
        LinearLayout lista = vista.findViewById(R.id.dlg_lista);
        Button cancelar = vista.findViewById(R.id.dlg_cancelar);

        if (esTrasKO) {
            titulo.setText(R.string.dlg_equipo_ko_titulo);
            subtitulo.setText(R.string.dlg_equipo_sub_ko);
        } else {
            titulo.setText(R.string.dlg_equipo_titulo);
            subtitulo.setText(R.string.dlg_equipo_sub_voluntario);
        }

        ArrayList<Criatura> equipo = batalla.getEquipoJugador();
        int disponibles = 0;
        for (int i = 0; i < equipo.size(); i++) {
            Criatura c = equipo.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_equipo_dialogo, lista, false);
            rellenarItemEquipo(item, c);

            boolean enCombate = (c == batalla.getActivaJugador());
            boolean ko = c.estaDebilitada();
            TextView estado = item.findViewById(R.id.estado);
            if (enCombate) {
                estado.setText(R.string.dlg_equipo_estado_en_combate);
                estado.setTextColor(getColor(R.color.boton_azul));
                item.setAlpha(ALFA_EN_COMBATE);
            } else if (ko) {
                estado.setText(R.string.dlg_equipo_estado_debilitada);
                estado.setTextColor(getColor(R.color.boton_rojo));
                item.setAlpha(ALFA_DEBILITADA);
            } else {
                estado.setText(R.string.dlg_equipo_estado_lista);
                estado.setTextColor(getColor(R.color.boton_verde));
                disponibles++;
                // esta sí se puede elegir: me guardo su posición en el equipo
                final int indiceEquipo = i;
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MusicaJuego.efectoConfirmacion();
                        cerrarDialogo();
                        ArrayList<Evento> eventos;
                        if (esTrasKO) {
                            eventos = batalla.cambioTrasKO(indiceEquipo);
                        } else {
                            turnosJugados++; // el cambio voluntario gasta turno; el de tras KO no
                            eventos = batalla.cambioVoluntario(indiceEquipo);
                        }
                        mostrarEventos(eventos);
                    }
                });
            }
            lista.addView(item);
        }

        if (disponibles == 0) {
            // sin reservas: si era voluntario devuelvo el control; si era tras KO el combate ya está acabado
            chat.agregar(getString(R.string.dlg_equipo_sin_reservas), Evento.NEUTRAL);
            if (!esTrasKO) {
                liberarAccion();
            }
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(vista);
        if (esTrasKO) {
            // no se puede cerrar: si se cerrara nos quedaríamos con el cerrojo puesto
            builder.setCancelable(false);
            cancelar.setVisibility(View.GONE);
        } else {
            // si cancela hay que soltar el cerrojo; el botón atrás no pasa por el botón de cancelar,
            // por eso lo pongo en los dos sitios
            cancelar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MusicaJuego.efectoSeleccion();
                    cerrarDialogo();
                    liberarAccion();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface d) {
                    liberarAccion();
                }
            });
        }
        dialogoActual = builder.show();
    }

    /**
     * Rellena una tarjeta del diálogo de equipo: la cabecera común (dibujo,
     * nombre y tipo) la pone TarjetaCriatura; aquí añado la barra de vida y los PS.
     *
     * @param item vista de la tarjeta (item_equipo_dialogo.xml)
     * @param c    criatura a mostrar
     */
    private void rellenarItemEquipo(View item, Criatura c) {
        TarjetaCriatura.rellenarCabecera(item, c);

        // barra de vida con el color según lo que le quede
        ProgressBar barra = item.findViewById(R.id.barra_hp);
        barra.setMax(c.getHpMax());
        barra.setProgress(c.getHp());
        barra.setProgressTintList(ColorStateList.valueOf(colorVida(c.fraccionVida())));

        TextView textoHp = item.findViewById(R.id.texto_hp);
        textoHp.setText(getString(R.string.combate_panel_ps, c.getHp(), c.getHpMax()));
    }

    /**
     * Color de la barra de vida del diálogo, con los mismos cortes que el panel
     * del combate (PanelCriatura) para que las dos barras cuenten lo mismo.
     *
     * @param fraccion vida que le queda, de 0 a 1
     * @return verde por encima de UMBRAL_VIDA_ALTA, naranja hasta UMBRAL_VIDA_BAJA y rojo por debajo
     */
    private int colorVida(float fraccion) {
        if (fraccion > PanelCriatura.UMBRAL_VIDA_ALTA) {
            return getColor(R.color.boton_verde);
        }
        if (fraccion > PanelCriatura.UMBRAL_VIDA_BAJA) {
            return getColor(R.color.barra_vida_media);
        }
        return getColor(R.color.boton_rojo);
    }

    /**
     * Diálogo de fin de combate con la criatura decisiva dibujada (VistaCriatura)
     * y confeti si es victoria: repetir, cambiar de equipo o volver al menú.
     * No se puede cancelar porque el combate ha terminado y tiene que elegir algo.
     *
     * @param victoria true si ha ganado el jugador
     */
    private void mostrarFin(boolean victoria) {
        MusicaJuego.reproducirResultado(victoria);
        Log.d(TAG, "Fin del combate, victoria=" + victoria + ", turnos=" + turnosJugados);
        guardarPartida(victoria);

        View vista = getLayoutInflater().inflate(R.layout.dialogo_fin, null);
        VistaConfeti confeti = vista.findViewById(R.id.fin_confeti);
        VistaCriatura criatura = vista.findViewById(R.id.fin_criatura);
        TextView titulo = vista.findViewById(R.id.fin_titulo);
        TextView mensaje = vista.findViewById(R.id.fin_mensaje);
        criatura.setCriatura(criaturaDecisiva(victoria));
        confeti.setActivo(victoria);
        if (victoria) {
            titulo.setText(R.string.dlg_victoria_titulo);
            mensaje.setText(R.string.dlg_victoria_msg);
        } else {
            titulo.setText(R.string.dlg_derrota_titulo);
            mensaje.setText(R.string.dlg_derrota_msg);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(vista);
        builder.setCancelable(false); // tiene que elegir algo
        builder.setPositiveButton(R.string.dlg_otra_vez, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int x) {
                MusicaJuego.efectoConfirmacion();
                iniciarBatalla();
            }
        });
        builder.setNeutralButton(R.string.dlg_cambiar_equipo, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int x) {
                MusicaJuego.efectoConfirmacion();
                irASeleccion();
            }
        });
        builder.setNegativeButton(R.string.dlg_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int x) {
                MusicaJuego.efectoSeleccion();
                irAlMenu();
            }
        });
        dialogoActual = builder.show();
    }

    /**
     * @param victoria true si ha ganado el jugador
     * @return la criatura que sale en el diálogo: la mía que quedó en pie o la rival que me venció
     */
    private Criatura criaturaDecisiva(boolean victoria) {
        if (victoria) {
            return batalla.getActivaJugador();
        }
        return batalla.getActivaRival();
    }

    /**
     * Pregunta antes de tirar el combate en marcha. Si dice no o cierra, suelto el cerrojo.
     */
    private void confirmarNuevaBatalla() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dlg_nueva_titulo);
        builder.setMessage(R.string.dlg_nueva_msg);
        builder.setPositiveButton(R.string.dlg_si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int x) {
                MusicaJuego.efectoConfirmacion();
                iniciarBatalla();
            }
        });
        builder.setNegativeButton(R.string.dlg_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int x) {
                MusicaJuego.efectoSeleccion();
                liberarAccion();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                liberarAccion();
            }
        });
        dialogoActual = builder.show();
    }

    /**
     * Cierra el diálogo abierto, si lo hay.
     */
    private void cerrarDialogo() {
        if (dialogoActual != null) {
            dialogoActual.dismiss();
        }
    }

    // ---- Guardar y salir ----

    /**
     * Si hay un usuario con sesión, apunto la partida en la base de datos con
     * los nombres de sus 3 criaturas. Lo hago en un Thread para no tocar la BD
     * en el hilo principal; si falla solo lo dejo en el Log (el combate ya ha
     * terminado y no quiero molestar con un error). Como invitado no se guarda.
     *
     * @param victoria true si ganó el jugador
     */
    private void guardarPartida(final boolean victoria) {
        if (!sesion.haySesion()) {
            return;
        }
        final long idUsuario = sesion.getIdUsuario();
        final int turnos = turnosJugados;
        ArrayList<Criatura> equipo = batalla.getEquipoJugador();
        final String[] nombres = new String[equipo.size()];
        for (int i = 0; i < equipo.size(); i++) {
            nombres[i] = equipo.get(i).getNombre();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long id = repositorioPartidas.guardar(idUsuario, victoria, nombres, turnos);
                    Log.d(TAG, "Partida guardada con id " + id);
                } catch (Exception e) {
                    Log.d(TAG, "No he podido guardar la partida: " + e.getClass().getSimpleName());
                }
            }
        }).start();
    }

    /**
     * Vuelve al menú.
     */
    private void irAlMenu() {
        salirHacia(new Intent(this, MenuActivity.class));
    }

    /**
     * Vuelve a la pantalla de elegir equipo.
     */
    private void irASeleccion() {
        salirHacia(new Intent(this, SeleccionActivity.class));
    }

    /**
     * Sale del combate hacia otra pantalla. CLEAR_TOP reutiliza la pantalla
     * que ya está en la pila y finish() cierra esta para que "atrás" no vuelva
     * al combate terminado.
     *
     * @param intent destino (menú o selección)
     */
    private void salirHacia(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Transiciones.atras(this);
        finish();
    }

    /**
     * Limpieza al destruir la pantalla: cancelo lo pendiente del Handler (para que
     * no lleguen mensajes a una pantalla que ya no existe) y cierro el diálogo
     * abierto (si no, Android avisa de WindowLeaked).
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicaJuego.detenerBatalla();
        MusicaJuego.detenerResultado();
        Log.d(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);
        cerrarDialogo();
        dialogoActual = null;
    }
}
