/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  MenuActivity.java
 */
package com.juego.combates.pantallas;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.juego.combates.R;
import com.juego.combates.datos.Estadisticas;
import com.juego.combates.datos.RepositorioPartidas;
import com.juego.combates.datos.Sesion;
import com.juego.combates.modelo.Criatura;
import com.juego.combates.modelo.Fabrica;
import com.juego.combates.vistas.Animaciones;
import com.juego.combates.vistas.VistaCriatura;

import java.util.ArrayList;

/**
 * Pantalla de inicio (se llega desde LoginActivity). Tres botones: elegir
 * equipo, combate rápido y cómo jugar. El layout está en activity_menu.xml.
 *
 * Arriba lleva la fila de usuario: "Hola, ana" con sus victorias y derrotas
 * (leídas de la base de datos en onResume, para que se actualicen al volver
 * de un combate) y el botón de cerrar sesión; si juega sin cuenta pone
 * "Invitado" y el botón lleva al login.
 *
 * Las seis criaturas de adorno flotan arriba y abajo con un ObjectAnimator,
 * cada una con un pequeño retardo para que no vayan todas a la vez.
 * La Activity implementa OnClickListener y en onClick miro qué botón se pulsó.
 */
public class MenuActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MenuActivity"; // para el Log

    /** Los ids de las seis criaturas de adorno del layout, en orden. */
    private static final int[] IDS_CRIATURAS = {
            R.id.criatura_0, R.id.criatura_1, R.id.criatura_2,
            R.id.criatura_3, R.id.criatura_4, R.id.criatura_5};

    /** Cuánto sube cada criatura al flotar y lo que dura un ciclo. */
    private static final float FLOTAR_DP = 8f;
    private static final long FLOTAR_MS = 2400;
    /** Desfase entre una criatura y la siguiente, para que no floten a la vez. */
    private static final long DESFASE_CRIATURAS_MS = 250;
    /** Fundido de entrada de los botones: desde cuántos dp suben y cuánto tardan. */
    private static final float APARECER_DP = 24f;
    private static final long APARECER_MS = 350;
    /** Retardo del primer botón y desfase entre botones. */
    private static final long RETARDO_PRIMER_BOTON_MS = 100;
    private static final long DESFASE_BOTONES_MS = 120;

    private Sesion sesion;
    private RepositorioPartidas repositorioPartidas;
    private Handler handler;

    private TextView textoUsuario;
    private TextView textoEstadisticas;
    private TextView textoTitulo;
    private Button botonSesion;
    private Button botonElegir;
    private Button botonRapido;
    private Button botonComoJugar;

    // me guardo los animadores para pausarlos, reanudarlos y pararlos al cerrar la pantalla
    private ArrayList<ObjectAnimator> animadores;

    /**
     * Monta el menú: vistas, criaturas flotando, fundido de los botones y el título con degradado.
     *
     * @param savedInstanceState estado anterior (no lo uso: la pantalla no gira)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu); // antes de cualquier findViewById
        Log.d(TAG, "onCreate");
        animadores = new ArrayList<ObjectAnimator>();
        sesion = new Sesion(this);
        repositorioPartidas = new RepositorioPartidas(this);
        handler = new Handler(Looper.getMainLooper());

        enlazarVistas();
        configurarCriaturas();

        // los botones aparecen con un pequeño fundido, uno tras otro
        Animaciones.aparecer(botonElegir, APARECER_DP, RETARDO_PRIMER_BOTON_MS, APARECER_MS);
        Animaciones.aparecer(botonRapido, APARECER_DP, RETARDO_PRIMER_BOTON_MS + DESFASE_BOTONES_MS, APARECER_MS);
        Animaciones.aparecer(botonComoJugar, APARECER_DP, RETARDO_PRIMER_BOTON_MS + 2 * DESFASE_BOTONES_MS, APARECER_MS);
        prepararTitulo();
    }

    /**
     * Busca las vistas del layout y pone los listeners.
     */
    private void enlazarVistas() {
        textoUsuario = findViewById(R.id.texto_usuario);
        textoEstadisticas = findViewById(R.id.texto_estadisticas);
        textoTitulo = findViewById(R.id.texto_titulo_menu);
        botonSesion = findViewById(R.id.boton_sesion);
        botonElegir = findViewById(R.id.boton_elegir);
        botonRapido = findViewById(R.id.boton_rapido);
        botonComoJugar = findViewById(R.id.boton_como_jugar);

        botonSesion.setOnClickListener(this);
        botonElegir.setOnClickListener(this);
        botonRapido.setOnClickListener(this);
        botonComoJugar.setOnClickListener(this);
    }

    /**
     * Pone en cada vista de adorno una especie distinta y la deja flotando,
     * con un desfase creciente para que no suban todas a la vez.
     */
    private void configurarCriaturas() {
        String[] especies = Fabrica.getEspecies();
        for (int i = 0; i < IDS_CRIATURAS.length; i++) {
            VistaCriatura vista = findViewById(IDS_CRIATURAS[i]);
            Criatura criatura = Fabrica.crear(especies[i % especies.length]);
            vista.setCriatura(criatura);
            animadores.add(Animaciones.flotar(vista, FLOTAR_DP, FLOTAR_MS, DESFASE_CRIATURAS_MS * i));
        }
    }

    /**
     * Aplica un degradado de tres colores al título cuando ya se conoce su
     * anchura (en onCreate aún mide 0, por eso va en un post).
     */
    private void prepararTitulo() {
        textoTitulo.post(new Runnable() {
            @Override
            public void run() {
                if (textoTitulo.getWidth() <= 0) {
                    return;
                }
                int[] colores = new int[]{
                        getColor(R.color.titulo_grad_1),
                        getColor(R.color.titulo_grad_2),
                        getColor(R.color.titulo_grad_3)};
                Shader shader = new LinearGradient(0, 0, textoTitulo.getWidth(), 0,
                        colores, null, Shader.TileMode.CLAMP);
                textoTitulo.getPaint().setShader(shader);
                textoTitulo.invalidate();
            }
        });
    }

    /**
     * Cada vez que la pantalla vuelve al frente refresco la fila de usuario:
     * así, al volver de un combate, las estadísticas ya incluyen esa partida.
     */
    @Override
    protected void onResume() {
        super.onResume();
        MusicaJuego.reproducirMenu();
        actualizarFilaUsuario();
        // reanudo el desfile que paré en onPause
        for (int i = 0; i < animadores.size(); i++) {
            animadores.get(i).resume();
        }
    }

    /**
     * Con el menú detrás de Selección o Combate, los seis animadores infinitos
     * seguirían invalidando vistas que no se ven en cada fotograma; los pauso
     * aquí y los reanudo en onResume (pause/resume son de API 19).
     */
    @Override
    protected void onPause() {
        super.onPause();
        MusicaJuego.detenerMenu();
        for (int i = 0; i < animadores.size(); i++) {
            animadores.get(i).pause();
        }
    }

    /**
     * Saludo, estadísticas y texto del botón según haya sesión o sea invitado.
     * La consulta a la base de datos va en un Thread para no tocar la BD en el
     * hilo principal.
     */
    private void actualizarFilaUsuario() {
        if (sesion.haySesion()) {
            textoUsuario.setText(getString(R.string.menu_hola, sesion.getNombreUsuario()));
            botonSesion.setText(R.string.menu_cerrar_sesion);
            final long idUsuario = sesion.getIdUsuario();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Estadisticas e;
                    try {
                        e = repositorioPartidas.estadisticas(idUsuario);
                    } catch (Exception ex) {
                        Log.d(TAG, "No he podido leer las estadísticas: " + ex.getClass().getSimpleName());
                        e = null;
                    }
                    final Estadisticas estadisticas = e;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mostrarEstadisticas(estadisticas);
                        }
                    });
                }
            }).start();
        } else {
            textoUsuario.setText(R.string.menu_invitado);
            textoEstadisticas.setVisibility(View.GONE);
            botonSesion.setText(R.string.menu_iniciar_sesion);
        }
    }

    /**
     * Ya en el hilo principal: pinta las victorias y derrotas leídas de la BD.
     *
     * @param estadisticas lo que devolvió el repositorio (null si falló la BD)
     */
    private void mostrarEstadisticas(Estadisticas estadisticas) {
        if (isFinishing() || isDestroyed() || estadisticas == null) {
            return;
        }
        textoEstadisticas.setText(getString(R.string.menu_estadisticas,
                estadisticas.getVictorias(), estadisticas.getDerrotas()));
        textoEstadisticas.setVisibility(View.VISIBLE);
    }

    // ---- Clics ----

    /**
     * Clics de los tres botones del menú y del botón de sesión.
     *
     * @param v el botón pulsado
     */
    @Override
    public void onClick(View v) {
        MusicaJuego.efectoSeleccion();
        if (v.getId() == R.id.boton_sesion) {
            alPulsarSesion();
        } else if (v.getId() == R.id.boton_elegir) {
            alPulsarElegir();
        } else if (v.getId() == R.id.boton_rapido) {
            alPulsarRapido();
        } else if (v.getId() == R.id.boton_como_jugar) {
            alPulsarComoJugar();
        }
    }

    /**
     * Con sesión: la cierro y vuelvo al login limpiando la pila (CLEAR_TOP +
     * finish) para que "atrás" no vuelva al menú de un usuario que ya salió.
     * Como invitado: abro el login encima; si entra, LoginActivity vuelve aquí
     * con CLEAR_TOP y este menú se recrea con la sesión puesta.
     */
    private void alPulsarSesion() {
        Intent intent = new Intent(this, LoginActivity.class);
        if (sesion.haySesion()) {
            Log.d(TAG, "Cerrar sesión");
            sesion.cerrar();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Transiciones.atras(this);
            finish();
        } else {
            Log.d(TAG, "Iniciar sesión desde invitado");
            startActivity(intent);
            Transiciones.adelante(this);
        }
    }

    /**
     * Abre la pantalla de elegir equipo.
     */
    private void alPulsarElegir() {
        Log.d(TAG, "Elegir equipo");
        startActivity(new Intent(this, SeleccionActivity.class));
        Transiciones.adelante(this);
    }

    /**
     * Abre el combate directamente; sin extra en el Intent = equipo al azar.
     */
    private void alPulsarRapido() {
        Log.d(TAG, "Combate rápido");
        startActivity(new Intent(this, CombateActivity.class));
        Transiciones.adelante(this);
    }

    /**
     * Diálogo con las reglas. El texto está en strings.xml.
     * El aspecto oscuro lo pone el tema de diálogos de themes.xml.
     */
    private void alPulsarComoJugar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ayuda_titulo);
        builder.setMessage(R.string.ayuda_texto);
        builder.setPositiveButton(R.string.ayuda_entendido, null); // null = solo cerrar
        builder.show();
    }

    /**
     * Paro las animaciones para que no sigan corriendo con la pantalla cerrada.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicaJuego.detenerMenu();
        handler.removeCallbacksAndMessages(null);
        for (int i = 0; i < animadores.size(); i++) {
            animadores.get(i).cancel();
        }
        animadores.clear();
    }
}
