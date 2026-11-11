/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  RecuperarActivity.java
 */
package com.juego.combates.pantallas;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juego.combates.R;
import com.juego.combates.datos.RepositorioUsuarios;
import com.juego.combates.datos.RepositorioUsuarios.ResultadoCambio;
import com.juego.combates.datos.RepositorioUsuarios.ResultadoRecuperacion;
import com.juego.combates.datos.Validador;
import com.juego.combates.vistas.Animaciones;
import com.juego.combates.vistas.VistaCriatura;

import java.util.Locale;
import java.util.Random;

/**
 * Recuperar la contraseña con la pregunta de seguridad, en tres pasos dentro
 * de la misma pantalla (voy enseñando y escondiendo bloques del layout):
 *   1. escribe el usuario -> busco su pregunta. Si el usuario no existe NO lo
 *      digo: el repositorio me da una pregunta inventada y paso al paso 2 igual,
 *      para que esta pantalla no sirva para averiguar qué cuentas hay.
 *   2. responde a la pregunta -> la comparo con el hash guardado (5 fallos =
 *      15 minutos de bloqueo, igual que el login; con un usuario inventado
 *      siempre es "incorrecta" y también se bloquea). Si acierta, el
 *      repositorio me da un token de un solo uso que me guardo.
 *   3. escribe la contraseña nueva dos veces -> la guardo (el repositorio me
 *      exige el token del paso 2, que caduca a los 10 minutos) y vuelvo al
 *      login con el usuario ya escrito.
 *
 * Como en LoginActivity, lo que toca la base de datos va en un Thread y vuelvo
 * al hilo principal con un Handler.
 */
public class RecuperarActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "RecuperarActivity";

    /** Extra opcional: usuario que ya tenía escrito en el login. */
    public static final String EXTRA_USUARIO = "com.juego.combates.USUARIO_RECUPERAR";

    private static final int PASO_USUARIO = 1;
    private static final int PASO_PREGUNTA = 2;
    private static final int PASO_PASSWORD = 3;

    /** Criatura de adorno: cuánto sube al flotar y lo que dura un ciclo. */
    private static final float FLOTAR_DP = 8f;
    private static final long FLOTAR_MS = 2600;

    private RepositorioUsuarios repositorio;
    private Handler handler;

    private VistaCriatura criatura;
    private TextView textoPaso;
    private TextView subtitulo;
    private LinearLayout paso1;
    private LinearLayout paso2;
    private LinearLayout paso3;
    private EditText campoUsuario;
    private TextView textoDeQuien;
    private TextView textoPregunta;
    private EditText campoRespuesta;
    private EditText campoPassword;
    private EditText campoPassword2;
    private TextView textoReglas;
    private TextView textoError;
    private Button botonAccion;
    private ProgressBar progreso;
    private Button botonVolver;

    private int paso = PASO_USUARIO;
    private String usuarioActual;     // el usuario que pasó el paso 1 (en minúsculas)
    private String tokenRecuperacion; // el token que dio el paso 2; sin él no hay paso 3
    private boolean cargando = false;
    private ObjectAnimator animadorCriatura;

    /**
     * Monta la pantalla en el paso 1, con el usuario ya escrito si viene del login.
     *
     * @param savedInstanceState estado anterior (no lo uso: la pantalla no gira)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar);
        Log.d(TAG, "onCreate");
        repositorio = new RepositorioUsuarios(this);
        handler = new Handler(Looper.getMainLooper());

        enlazarVistas();
        configurarCriatura();
        // el texto de reglas lleva los mismos límites que aplica Validador
        textoReglas.setText(getString(R.string.login_reglas,
                Validador.USUARIO_MIN, Validador.USUARIO_MAX, Validador.PASSWORD_MIN, Validador.PASSWORD_MAX));

        String usuarioPrevio = getIntent().getStringExtra(EXTRA_USUARIO);
        if (usuarioPrevio != null) {
            campoUsuario.setText(usuarioPrevio);
            campoUsuario.setSelection(campoUsuario.getText().length());
        }
        mostrarPaso(PASO_USUARIO);
    }

    /**
     * Busca las vistas del layout y pone los listeners.
     */
    private void enlazarVistas() {
        criatura = findViewById(R.id.criatura_recuperar);
        textoPaso = findViewById(R.id.rec_texto_paso);
        subtitulo = findViewById(R.id.rec_subtitulo);
        paso1 = findViewById(R.id.paso_1);
        paso2 = findViewById(R.id.paso_2);
        paso3 = findViewById(R.id.paso_3);
        campoUsuario = findViewById(R.id.rec_campo_usuario);
        textoDeQuien = findViewById(R.id.rec_texto_de_quien);
        textoPregunta = findViewById(R.id.rec_texto_pregunta);
        campoRespuesta = findViewById(R.id.rec_campo_respuesta);
        campoPassword = findViewById(R.id.rec_campo_password);
        campoPassword2 = findViewById(R.id.rec_campo_password2);
        textoReglas = findViewById(R.id.rec_texto_reglas);
        textoError = findViewById(R.id.rec_texto_error);
        botonAccion = findViewById(R.id.rec_boton_accion);
        progreso = findViewById(R.id.rec_progreso);
        botonVolver = findViewById(R.id.rec_boton_volver);

        botonAccion.setOnClickListener(this);
        botonVolver.setOnClickListener(this);

        // el "hecho" del teclado equivale a pulsar el botón de acción
        EnvioConTeclado enviarConTeclado = new EnvioConTeclado(new Runnable() {
            @Override
            public void run() {
                alPulsarAccion();
            }
        });
        campoUsuario.setOnEditorActionListener(enviarConTeclado);
        campoRespuesta.setOnEditorActionListener(enviarConTeclado);
        campoPassword2.setOnEditorActionListener(enviarConTeclado);
    }

    /**
     * Criatura de adorno: una especie al azar, flotando como la del login.
     */
    private void configurarCriatura() {
        criatura.ponerEspecieAlAzar(new Random());
        animadorCriatura = Animaciones.flotar(criatura, FLOTAR_DP, FLOTAR_MS, 0);
    }

    // ---- Pasos ----

    /**
     * Enseña el bloque del paso que toca y esconde los otros dos; cambia el
     * subtítulo y el texto del botón.
     *
     * @param nuevoPaso PASO_USUARIO, PASO_PREGUNTA o PASO_PASSWORD
     */
    private void mostrarPaso(int nuevoPaso) {
        paso = nuevoPaso;
        ocultarError();
        textoPaso.setText(getString(R.string.rec_paso, paso));
        if (paso == PASO_USUARIO) {
            paso1.setVisibility(View.VISIBLE);
            paso2.setVisibility(View.GONE);
            paso3.setVisibility(View.GONE);
            subtitulo.setText(R.string.rec_sub_usuario);
            botonAccion.setText(R.string.rec_continuar);
            campoUsuario.requestFocus();
        } else if (paso == PASO_PREGUNTA) {
            paso1.setVisibility(View.GONE);
            paso2.setVisibility(View.VISIBLE);
            paso3.setVisibility(View.GONE);
            subtitulo.setText(R.string.rec_sub_pregunta);
            botonAccion.setText(R.string.rec_comprobar);
            campoRespuesta.setText("");
            campoRespuesta.requestFocus();
            tokenRecuperacion = null; // al (volver a) este paso hay que ganárselo otra vez
        } else {
            paso1.setVisibility(View.GONE);
            paso2.setVisibility(View.GONE);
            paso3.setVisibility(View.VISIBLE);
            subtitulo.setText(R.string.rec_sub_password);
            botonAccion.setText(R.string.rec_cambiar);
            campoPassword.setText("");
            campoPassword2.setText("");
            campoPassword.requestFocus();
        }
    }

    /**
     * Clics del botón de acción y del de volver.
     *
     * @param v el botón pulsado
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rec_boton_accion) {
            alPulsarAccion();
        } else if (v.getId() == R.id.rec_boton_volver) {
            alPulsarVolver();
        }
    }

    /**
     * "Volver" retrocede un paso; en el primero cierra la pantalla. El botón
     * atrás del móvil hace lo mismo (ver onBackPressed). Mientras trabaja el
     * Thread no hago caso: si cambiara de paso, el resultado llegaría al paso
     * equivocado.
     */
    private void alPulsarVolver() {
        if (cargando) {
            return;
        }
        if (paso == PASO_USUARIO) {
            finish();
        } else if (paso == PASO_PREGUNTA) {
            mostrarPaso(PASO_USUARIO);
        } else {
            mostrarPaso(PASO_PREGUNTA);
        }
    }

    /**
     * El botón atrás del móvil hace lo mismo que "Volver". onBackPressed está
     * marcado como obsoleto desde la API 33, pero sin AndroidX no tengo
     * OnBackPressedDispatcher, y con targetSdk 34 el sistema sigue llamándolo
     * igual: silencio el aviso a sabiendas.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        alPulsarVolver();
    }

    /**
     * El botón de acción (o el "hecho" del teclado) hace lo que toque en el
     * paso actual. Compruebo el cerrojo aquí porque también me llaman desde el
     * teclado, que no pasa por onClick.
     */
    private void alPulsarAccion() {
        if (cargando) {
            return;
        }
        ocultarError();
        if (paso == PASO_USUARIO) {
            buscarUsuario();
        } else if (paso == PASO_PREGUNTA) {
            comprobarRespuesta();
        } else {
            cambiarPassword();
        }
    }

    // ---- Paso 1: buscar el usuario y su pregunta ----

    /**
     * Valida el nombre y pide al repositorio su pregunta en un Thread aparte.
     */
    private void buscarUsuario() {
        final String usuario = campoUsuario.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (!Validador.esUsuarioValido(usuario)) {
            mostrarError(getString(R.string.login_error_usuario, Validador.USUARIO_MIN, Validador.USUARIO_MAX));
            return;
        }
        // el repositorio necesita saber cuántas preguntas hay para inventar una válida
        final int numeroPreguntas = getResources().getStringArray(R.array.preguntas_seguridad).length;
        ponerCargando(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int pregunta;
                try {
                    pregunta = repositorio.obtenerPregunta(usuario, numeroPreguntas);
                } catch (Exception e) {
                    Log.d(TAG, "Error al buscar el usuario: " + e.getClass().getSimpleName());
                    pregunta = -1;
                }
                final int p = pregunta;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alEncontrarPregunta(usuario, p);
                    }
                });
            }
        }).start();
    }

    /**
     * Ya en el hilo principal: enseño la pregunta y paso al paso 2.
     *
     * @param usuario  el usuario buscado
     * @param pregunta índice de su pregunta (real o inventada), o -1 si falló la BD
     */
    private void alEncontrarPregunta(String usuario, int pregunta) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        ponerCargando(false);
        if (pregunta < 0) {
            mostrarError(getString(R.string.login_error_generico));
            return;
        }
        usuarioActual = usuario;
        String[] preguntas = getResources().getStringArray(R.array.preguntas_seguridad);
        String textoDeLaPregunta;
        if (pregunta < preguntas.length) {
            textoDeLaPregunta = preguntas[pregunta];
        } else {
            textoDeLaPregunta = getString(R.string.rec_pregunta_desconocida); // no debería pasar: el índice siempre sale del Spinner
        }
        textoDeQuien.setText(getString(R.string.rec_pregunta_de, usuario));
        textoPregunta.setText(textoDeLaPregunta);
        Log.d(TAG, "Paso 2: pregunta " + pregunta);
        mostrarPaso(PASO_PREGUNTA);
    }

    // ---- Paso 2: comprobar la respuesta ----

    /**
     * Valida la respuesta y la manda a comprobar en un Thread aparte (el hash tarda).
     */
    private void comprobarRespuesta() {
        final String respuesta = campoRespuesta.getText().toString();
        if (!Validador.esRespuestaValida(respuesta)) {
            mostrarError(getString(R.string.login_error_respuesta, Validador.RESPUESTA_MIN, Validador.RESPUESTA_MAX));
            return;
        }
        ponerCargando(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ResultadoRecuperacion resultado;
                try {
                    resultado = repositorio.verificarRespuesta(usuarioActual, respuesta);
                } catch (Exception e) {
                    Log.d(TAG, "Error al comprobar la respuesta: " + e.getClass().getSimpleName());
                    resultado = null;
                }
                final ResultadoRecuperacion r = resultado;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alComprobarRespuesta(r);
                    }
                });
            }
        }).start();
    }

    /**
     * Ya en el hilo principal: si acertó me guardo el token y paso al paso 3.
     *
     * @param resultado OK, INCORRECTA o BLOQUEADO (null si falló la BD)
     */
    private void alComprobarRespuesta(ResultadoRecuperacion resultado) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        ponerCargando(false);
        if (resultado == null) {
            mostrarError(getString(R.string.login_error_generico));
        } else if (resultado.getEstado() == ResultadoRecuperacion.Estado.OK) {
            Log.d(TAG, "Respuesta correcta, paso 3");
            // me guardo el token para dárselo a cambiarPassword; el valor no lo
            // escribo en el Log, que para eso es un secreto
            tokenRecuperacion = resultado.getToken();
            mostrarPaso(PASO_PASSWORD);
        } else if (resultado.getEstado() == ResultadoRecuperacion.Estado.BLOQUEADO) {
            int minutos = resultado.getMinutosRestantes();
            mostrarError(getResources().getQuantityString(R.plurals.login_error_bloqueado, minutos, minutos));
        } else {
            mostrarError(getString(R.string.rec_error_respuesta));
        }
    }

    // ---- Paso 3: contraseña nueva ----

    /**
     * Valida la contraseña nueva (y que la haya repetido bien) y la guarda en
     * un Thread aparte con el token del paso 2.
     */
    private void cambiarPassword() {
        final String password = campoPassword.getText().toString();
        String repetida = campoPassword2.getText().toString();
        if (!Validador.esPasswordValida(password)) {
            mostrarError(getString(R.string.login_error_password, Validador.PASSWORD_MIN, Validador.PASSWORD_MAX));
            return;
        }
        if (!password.equals(repetida)) {
            mostrarError(getString(R.string.rec_error_no_coinciden));
            return;
        }
        // copio el token a una final para que la clase anónima pueda usarlo
        final String token = tokenRecuperacion;
        ponerCargando(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ResultadoCambio resultado;
                try {
                    resultado = repositorio.cambiarPassword(usuarioActual, token, password);
                } catch (Exception e) {
                    Log.d(TAG, "Error al cambiar la contraseña: " + e.getClass().getSimpleName());
                    resultado = null;
                }
                final ResultadoCambio r = resultado;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alCambiarPassword(r);
                    }
                });
            }
        }).start();
    }

    /**
     * Ya en el hilo principal: si se guardó, vuelvo al login con el usuario escrito.
     *
     * @param resultado OK, TOKEN_INVALIDO o NO_GUARDADO (null si falló la BD)
     */
    private void alCambiarPassword(ResultadoCambio resultado) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        ponerCargando(false);
        if (resultado == null || resultado == ResultadoCambio.NO_GUARDADO) {
            mostrarError(getString(R.string.login_error_bd));
            return;
        }
        if (resultado == ResultadoCambio.TOKEN_INVALIDO) {
            // el permiso del paso 2 ha caducado (o ya se gastó): vuelvo a la
            // pregunta. Primero el paso, porque mostrarPaso esconde el error.
            Log.d(TAG, "Token de recuperación no válido, vuelvo al paso 2");
            mostrarPaso(PASO_PREGUNTA);
            mostrarError(getString(R.string.rec_error_caducado));
            return;
        }
        Log.d(TAG, "Contraseña cambiada, vuelvo al login");
        Toast.makeText(this, R.string.rec_cambiada, Toast.LENGTH_SHORT).show();
        // vuelvo al login con el usuario ya escrito. CLEAR_TOP reutiliza el
        // login que hay debajo en la pila si sigue vivo.
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_USUARIO, usuarioActual);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Transiciones.atras(this);
        finish();
    }

    // ---- Ayudantes de pantalla ----

    /**
     * Bloquea los controles y enseña la ruedecita encima del botón de acción.
     *
     * @param activo true mientras trabaja el Thread
     */
    private void ponerCargando(boolean activo) {
        cargando = activo;
        if (activo) {
            progreso.setVisibility(View.VISIBLE);
            botonAccion.setText("");
        } else {
            progreso.setVisibility(View.GONE);
            if (paso == PASO_USUARIO) {
                botonAccion.setText(R.string.rec_continuar);
            } else if (paso == PASO_PREGUNTA) {
                botonAccion.setText(R.string.rec_comprobar);
            } else {
                botonAccion.setText(R.string.rec_cambiar);
            }
        }
        botonAccion.setEnabled(!activo);
        botonVolver.setEnabled(!activo);
        campoUsuario.setEnabled(!activo);
        campoRespuesta.setEnabled(!activo);
        campoPassword.setEnabled(!activo);
        campoPassword2.setEnabled(!activo);
    }

    /**
     * Enseña el texto de error en rojo debajo del formulario.
     *
     * @param mensaje texto ya traducido de strings.xml
     */
    private void mostrarError(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    /**
     * Esconde el texto de error (al cambiar de paso o antes de volver a enviar).
     */
    private void ocultarError() {
        textoError.setVisibility(View.GONE);
    }

    /**
     * Paro la criatura y cancelo lo pendiente del Handler.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animadorCriatura != null) {
            animadorCriatura.cancel();
        }
        handler.removeCallbacksAndMessages(null);
    }
}
