/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  LoginActivity.java
 */
package com.juego.combates.pantallas;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.juego.combates.R;
import com.juego.combates.datos.RepositorioUsuarios;
import com.juego.combates.datos.RepositorioUsuarios.ResultadoLogin;
import com.juego.combates.datos.RepositorioUsuarios.ResultadoRegistro;
import com.juego.combates.datos.Sesion;
import com.juego.combates.datos.Validador;
import com.juego.combates.vistas.Animaciones;
import com.juego.combates.vistas.VistaCriatura;

import java.util.Locale;
import java.util.Random;

/**
 * Pantalla de inicio de sesión. Es la LAUNCHER: la app arranca por aquí.
 *
 * Todo es local: las cuentas están en la base de datos SQLite del propio móvil
 * (paquete datos), sin nada remoto ni red. Tiene dos modos (entrar / crear
 * cuenta) que cambio con dos botones a modo de pestañas. Si ya hay una sesión
 * guardada paso directamente al menú sin enseñar el formulario.
 *
 * Calcular el hash de la contraseña (120000 vueltas de PBKDF2) tarda unos
 * cientos de ms, así que el trabajo con la base de datos lo hago en un Thread y
 * vuelvo al hilo principal con un Handler para tocar la pantalla. Mientras,
 * ponerCargando(true) bloquea el formulario y enseña la ruedecita.
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    /** Extra opcional con el usuario a dejar escrito (lo manda RecuperarActivity). */
    public static final String EXTRA_USUARIO = "com.juego.combates.USUARIO";

    /** Criatura de adorno: cuánto sube al flotar y lo que dura un ciclo. */
    private static final float FLOTAR_DP = 10f;
    private static final long FLOTAR_MS = 2600;
    /** Fundido de entrada del formulario y del botón de invitado. */
    private static final float APARECER_DP = 24f;
    private static final long APARECER_MS = 350;
    private static final long RETARDO_FORMULARIO_MS = 80;
    private static final long RETARDO_INVITADO_MS = 200;
    /** Opacidad del ojo cuando la contraseña va con puntos. */
    private static final float ALFA_OJO_APAGADO = 0.6f;

    private Sesion sesion;
    private RepositorioUsuarios repositorio;
    private Handler handler; // para volver al hilo principal desde el Thread

    private VistaCriatura criaturaLogin;
    private LinearLayout panelFormulario;
    private Button pestanaEntrar;
    private Button pestanaCrear;
    private EditText campoUsuario;
    private EditText campoEmail;
    private EditText campoPassword;
    private Button botonVerPassword;
    private TextView etiquetaPregunta;
    private Spinner spinnerPregunta;
    private EditText campoRespuesta;
    private TextView textoReglas;
    private TextView enlaceOlvide;
    private TextView textoError;
    private Button botonPrincipal;
    private ProgressBar progresoPrincipal;
    private Button botonInvitado;

    private boolean modoCrear = false;      // false = entrar, true = crear cuenta
    private boolean passwordVisible = false;
    private boolean cargando = false;       // cerrojo para no lanzar dos operaciones a la vez
    private ObjectAnimator animadorCriatura;

    /**
     * Monta la pantalla, o pasa directo al menú si ya hay sesión guardada.
     *
     * @param savedInstanceState estado anterior (no lo uso: la pantalla no gira)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = new Sesion(this);

        // si ya hay alguien dentro no enseño nada: directo al menú
        if (sesion.haySesion()) {
            Log.d(TAG, "Sesión guardada, voy al menú");
            irAlMenu();
            return;
        }

        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate");
        repositorio = new RepositorioUsuarios(this);
        handler = new Handler(Looper.getMainLooper());

        enlazarVistas();
        configurarCriatura();
        configurarSpinner();
        // el texto de reglas lleva los mismos límites que aplica Validador
        textoReglas.setText(getString(R.string.login_reglas,
                Validador.USUARIO_MIN, Validador.USUARIO_MAX, Validador.PASSWORD_MIN, Validador.PASSWORD_MAX));
        aplicarModo();
        Animaciones.aparecer(panelFormulario, APARECER_DP, RETARDO_FORMULARIO_MS, APARECER_MS);
        Animaciones.aparecer(botonInvitado, APARECER_DP, RETARDO_INVITADO_MS, APARECER_MS);

        // si vengo de recuperar la contraseña dejo el usuario ya escrito
        String usuarioPrevio = getIntent().getStringExtra(EXTRA_USUARIO);
        if (usuarioPrevio != null) {
            campoUsuario.setText(usuarioPrevio);
            campoPassword.requestFocus();
        }
    }

    /**
     * Busca las vistas del layout y pone los listeners.
     */
    private void enlazarVistas() {
        criaturaLogin = findViewById(R.id.criatura_login);
        panelFormulario = findViewById(R.id.panel_formulario);
        pestanaEntrar = findViewById(R.id.pestana_entrar);
        pestanaCrear = findViewById(R.id.pestana_crear);
        campoUsuario = findViewById(R.id.campo_usuario);
        campoEmail = findViewById(R.id.campo_email);
        campoPassword = findViewById(R.id.campo_password);
        botonVerPassword = findViewById(R.id.boton_ver_password);
        etiquetaPregunta = findViewById(R.id.etiqueta_pregunta);
        spinnerPregunta = findViewById(R.id.spinner_pregunta);
        campoRespuesta = findViewById(R.id.campo_respuesta);
        textoReglas = findViewById(R.id.texto_reglas);
        enlaceOlvide = findViewById(R.id.enlace_olvide);
        textoError = findViewById(R.id.texto_error);
        botonPrincipal = findViewById(R.id.boton_principal);
        progresoPrincipal = findViewById(R.id.progreso_principal);
        botonInvitado = findViewById(R.id.boton_invitado);

        pestanaEntrar.setOnClickListener(this);
        pestanaCrear.setOnClickListener(this);
        botonVerPassword.setOnClickListener(this);
        botonPrincipal.setOnClickListener(this);
        botonInvitado.setOnClickListener(this);
        enlaceOlvide.setOnClickListener(this);

        // el botón "hecho" del teclado envía el formulario (en la contraseña al
        // entrar, y en la respuesta al crear cuenta)
        EnvioConTeclado enviarConTeclado = new EnvioConTeclado(new Runnable() {
            @Override
            public void run() {
                enviarFormulario();
            }
        });
        campoPassword.setOnEditorActionListener(enviarConTeclado);
        campoRespuesta.setOnEditorActionListener(enviarConTeclado);
    }

    /**
     * Criatura de adorno: una especie al azar cada vez que se abre, flotando
     * igual que las del menú (ObjectAnimator infinito sobre translationY).
     */
    private void configurarCriatura() {
        criaturaLogin.ponerEspecieAlAzar(new Random());
        animadorCriatura = Animaciones.flotar(criaturaLogin, FLOTAR_DP, FLOTAR_MS, 0);
    }

    /**
     * Relleno el Spinner con las preguntas de res/values/arrays.xml. En la base
     * de datos guardo la posición elegida, no el texto.
     */
    private void configurarSpinner() {
        ArrayAdapter<CharSequence> adaptador = ArrayAdapter.createFromResource(
                this, R.array.preguntas_seguridad, android.R.layout.simple_spinner_item);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPregunta.setAdapter(adaptador);
    }

    // ---- Estados de la pantalla ----

    /**
     * Pinta las pestañas según el modo: la activa rellena de azul, la otra solo
     * con contorno. El email, la pregunta de seguridad, la respuesta y el texto
     * de reglas solo salen al crear cuenta; el enlace de "olvidé la contraseña"
     * solo al entrar.
     */
    private void aplicarModo() {
        pintarPestana(pestanaCrear, modoCrear);
        pintarPestana(pestanaEntrar, !modoCrear);
        if (modoCrear) {
            campoEmail.setVisibility(View.VISIBLE);
            etiquetaPregunta.setVisibility(View.VISIBLE);
            spinnerPregunta.setVisibility(View.VISIBLE);
            campoRespuesta.setVisibility(View.VISIBLE);
            textoReglas.setVisibility(View.VISIBLE);
            enlaceOlvide.setVisibility(View.GONE);
            campoPassword.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            botonPrincipal.setText(R.string.login_boton_crear);
        } else {
            campoEmail.setVisibility(View.GONE);
            etiquetaPregunta.setVisibility(View.GONE);
            spinnerPregunta.setVisibility(View.GONE);
            campoRespuesta.setVisibility(View.GONE);
            textoReglas.setVisibility(View.GONE);
            enlaceOlvide.setVisibility(View.VISIBLE);
            campoPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
            botonPrincipal.setText(R.string.login_boton_entrar);
        }
        ocultarError();
    }

    /**
     * Pinta una pestaña como activa (rellena de azul, texto blanco) o inactiva
     * (solo contorno, texto claro).
     *
     * @param pestana el botón que hace de pestaña
     * @param activa  true si es la del modo actual
     */
    private void pintarPestana(Button pestana, boolean activa) {
        if (activa) {
            pestana.setBackground(getDrawable(R.drawable.boton_redondeado));
            pestana.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.boton_azul)));
            pestana.setTextColor(getColor(R.color.texto_blanco));
        } else {
            pestana.setBackground(getDrawable(R.drawable.boton_contorno));
            pestana.setBackgroundTintList(null);
            pestana.setTextColor(getColor(R.color.texto_claro));
        }
    }

    /**
     * Bloquea el formulario y enseña la ruedecita mientras trabaja el Thread.
     * Al botón le quito el texto para que la ruedecita se vea sola encima.
     *
     * @param activo true para empezar a cargar, false para volver a la normalidad
     */
    private void ponerCargando(boolean activo) {
        cargando = activo;
        if (activo) {
            progresoPrincipal.setVisibility(View.VISIBLE);
            botonPrincipal.setText("");
        } else {
            progresoPrincipal.setVisibility(View.GONE);
            if (modoCrear) {
                botonPrincipal.setText(R.string.login_boton_crear);
            } else {
                botonPrincipal.setText(R.string.login_boton_entrar);
            }
        }
        botonPrincipal.setEnabled(!activo);
        pestanaEntrar.setEnabled(!activo);
        pestanaCrear.setEnabled(!activo);
        campoUsuario.setEnabled(!activo);
        campoEmail.setEnabled(!activo);
        campoPassword.setEnabled(!activo);
        spinnerPregunta.setEnabled(!activo);
        campoRespuesta.setEnabled(!activo);
        enlaceOlvide.setEnabled(!activo);
        botonInvitado.setEnabled(!activo);
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
     * Esconde el texto de error (al cambiar de modo o antes de volver a enviar).
     */
    private void ocultarError() {
        textoError.setVisibility(View.GONE);
    }

    // ---- Clics ----

    /**
     * Clics de las pestañas, el ojo, el botón principal, el enlace y el invitado.
     *
     * @param v la vista pulsada
     */
    @Override
    public void onClick(View v) {
        if (cargando) {
            return; // mientras trabaja el Thread no hago caso a los botones
        }
        if (v.getId() == R.id.pestana_entrar) {
            alPulsarPestana(false);
        } else if (v.getId() == R.id.pestana_crear) {
            alPulsarPestana(true);
        } else if (v.getId() == R.id.boton_ver_password) {
            alPulsarVerPassword();
        } else if (v.getId() == R.id.boton_principal) {
            enviarFormulario();
        } else if (v.getId() == R.id.enlace_olvide) {
            alPulsarOlvide();
        } else if (v.getId() == R.id.boton_invitado) {
            alPulsarInvitado();
        }
    }

    /**
     * Cambia de modo y repinta el formulario.
     *
     * @param crear true para "Crear cuenta", false para "Entrar"
     */
    private void alPulsarPestana(boolean crear) {
        modoCrear = crear;
        aplicarModo();
    }

    /**
     * Cambia entre ver la contraseña en claro y con puntos. Al cambiar el
     * inputType el cursor se va al principio, así que lo devuelvo al final.
     */
    private void alPulsarVerPassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            campoPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            botonVerPassword.setAlpha(1f);
        } else {
            campoPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            botonVerPassword.setAlpha(ALFA_OJO_APAGADO);
        }
        campoPassword.setSelection(campoPassword.getText().length());
    }

    /**
     * Abre la pantalla de recuperar contraseña con el usuario que haya escrito,
     * para que no lo teclee otra vez.
     */
    private void alPulsarOlvide() {
        Log.d(TAG, "Abro recuperar contraseña");
        Intent intent = new Intent(this, RecuperarActivity.class);
        intent.putExtra(RecuperarActivity.EXTRA_USUARIO, campoUsuario.getText().toString().trim());
        startActivity(intent);
        Transiciones.adelante(this);
    }

    /**
     * Jugar sin cuenta: borro la sesión que hubiera y paso al menú.
     */
    private void alPulsarInvitado() {
        Log.d(TAG, "Modo invitado");
        sesion.entrarComoInvitado();
        irAlMenu();
    }

    // ---- Validación y envío ----

    /**
     * Valida los campos y, si están bien, entra o crea la cuenta según el modo.
     */
    private void enviarFormulario() {
        if (cargando) {
            return;
        }
        ocultarError();
        // el repositorio guarda usuario y email en minúsculas; lo hago aquí también
        // para que el mensaje de error salga con lo mismo que se va a guardar
        final String usuario = campoUsuario.getText().toString().trim().toLowerCase(Locale.ROOT);
        final String email = campoEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        final String password = campoPassword.getText().toString();
        final String respuesta = campoRespuesta.getText().toString();
        final int pregunta = spinnerPregunta.getSelectedItemPosition();

        String error = validar(usuario, email, password, respuesta);
        if (error != null) {
            mostrarError(error);
            return;
        }

        ponerCargando(true);
        if (modoCrear) {
            hacerRegistro(usuario, email, password, pregunta, respuesta);
        } else {
            hacerLogin(usuario, password);
        }
    }

    /**
     * Aplica las reglas de Validador (las mismas que usa RecuperarActivity).
     * El email y la respuesta solo se miran al crear cuenta. Los mensajes son
     * plantillas que relleno con los límites de Validador, así el texto y la
     * regla no pueden decir cosas distintas.
     *
     * @param usuario   nombre ya recortado y en minúsculas
     * @param email     email ya recortado y en minúsculas ("" si no lo dio)
     * @param password  contraseña tal cual
     * @param respuesta respuesta de seguridad tal cual
     * @return el mensaje de error, o null si todo está bien
     */
    private String validar(String usuario, String email, String password, String respuesta) {
        if (!Validador.esUsuarioValido(usuario)) {
            return getString(R.string.login_error_usuario, Validador.USUARIO_MIN, Validador.USUARIO_MAX);
        }
        if (modoCrear && !Validador.esEmailValido(email)) {
            return getString(R.string.login_error_email);
        }
        if (!Validador.esPasswordValida(password)) {
            return getString(R.string.login_error_password, Validador.PASSWORD_MIN, Validador.PASSWORD_MAX);
        }
        if (modoCrear && !Validador.esRespuestaValida(respuesta)) {
            return getString(R.string.login_error_respuesta, Validador.RESPUESTA_MIN, Validador.RESPUESTA_MAX);
        }
        return null;
    }

    /**
     * Comprueba usuario y contraseña en un Thread aparte (el hash tarda).
     *
     * @param usuario  nombre ya normalizado
     * @param password contraseña en claro
     */
    private void hacerLogin(final String usuario, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ResultadoLogin resultado;
                try {
                    resultado = repositorio.autenticar(usuario, password);
                } catch (Exception e) {
                    Log.d(TAG, "Error al autenticar: " + e.getClass().getSimpleName());
                    resultado = null;
                }
                final ResultadoLogin r = resultado;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alTerminarLogin(r);
                    }
                });
            }
        }).start();
    }

    /**
     * Ya en el hilo principal: según el resultado entro o enseño el error.
     *
     * @param resultado lo que devolvió el repositorio (null si falló la BD)
     */
    private void alTerminarLogin(ResultadoLogin resultado) {
        if (isFinishing() || isDestroyed()) {
            return; // la pantalla se cerró mientras trabajaba el Thread
        }
        if (resultado == null) {
            ponerCargando(false);
            mostrarError(getString(R.string.login_error_generico));
            return;
        }
        if (resultado.getEstado() == ResultadoLogin.Estado.OK) {
            Log.d(TAG, "Login correcto");
            sesion.guardar(resultado.getUsuario());
            irAlMenu();
        } else if (resultado.getEstado() == ResultadoLogin.Estado.BLOQUEADO) {
            ponerCargando(false);
            int minutos = resultado.getMinutosRestantes();
            mostrarError(getResources().getQuantityString(R.plurals.login_error_bloqueado, minutos, minutos));
        } else {
            // mismo texto exista o no el usuario, para no delatar qué cuentas hay
            ponerCargando(false);
            mostrarError(getString(R.string.login_error_incorrecto));
        }
    }

    /**
     * Crea la cuenta en un Thread aparte. El repositorio me devuelve el Usuario
     * ya creado, así que entro con él directamente: no vuelvo a llamar a
     * autenticar (sería un tercer hash PBKDF2 para nada, y si fallara daría un
     * error de "no se ha podido guardar" con la cuenta ya guardada).
     *
     * @param usuario   nombre ya normalizado
     * @param email     email ya normalizado ("" si no lo dio)
     * @param password  contraseña en claro
     * @param pregunta  posición de la pregunta elegida en el Spinner
     * @param respuesta respuesta de seguridad en claro
     */
    private void hacerRegistro(final String usuario, final String email, final String password,
                               final int pregunta, final String respuesta) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ResultadoRegistro registro;
                try {
                    registro = repositorio.registrar(usuario, email, password, pregunta, respuesta);
                } catch (Exception e) {
                    Log.d(TAG, "Error al registrar: " + e.getClass().getSimpleName());
                    registro = null;
                }
                final ResultadoRegistro r = registro;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alTerminarRegistro(r);
                    }
                });
            }
        }).start();
    }

    /**
     * Ya en el hilo principal: entro con la cuenta nueva o enseño qué estaba repetido.
     *
     * @param registro resultado del alta, con el Usuario si fue OK (null si falló la BD)
     */
    private void alTerminarRegistro(ResultadoRegistro registro) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (registro == null) {
            ponerCargando(false);
            mostrarError(getString(R.string.login_error_bd));
        } else if (registro.getEstado() == ResultadoRegistro.Estado.USUARIO_EXISTE) {
            ponerCargando(false);
            mostrarError(getString(R.string.login_error_usuario_existe));
        } else if (registro.getEstado() == ResultadoRegistro.Estado.EMAIL_EXISTE) {
            ponerCargando(false);
            mostrarError(getString(R.string.login_error_email_existe));
        } else {
            Log.d(TAG, "Cuenta creada, entro con ella");
            Toast.makeText(this, R.string.login_cuenta_creada, Toast.LENGTH_SHORT).show();
            sesion.guardar(registro.getUsuario());
            irAlMenu();
        }
    }

    /**
     * Pasa al menú y cierra esta pantalla para que "atrás" no vuelva al login.
     * CLEAR_TOP por si vengo del botón "Iniciar sesión" del menú de invitado:
     * así el menú viejo se quita y se crea uno nuevo con la sesión puesta.
     */
    private void irAlMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Transiciones.adelante(this);
        finish();
    }

    /**
     * Paro la criatura y cancelo lo pendiente del Handler. Los dos pueden ser
     * null si en onCreate ya había sesión y me fui al menú sin montar la pantalla.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animadorCriatura != null) {
            animadorCriatura.cancel();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
