/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Validador.java
 */
package com.juego.combates.datos;

import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Reglas de los campos del formulario, en un solo sitio para que LoginActivity
 * y RecuperarActivity apliquen exactamente las mismas.
 *
 * Usuario: 3-20 caracteres, solo letras, números o guion bajo.
 * Email: opcional; si lo dan, hasta 254 caracteres y con forma de email.
 * Contraseña: 8-72 caracteres con al menos una letra y un número.
 * Respuesta de seguridad: 2-60 caracteres una vez normalizada.
 *
 * Los límites son públicos porque las pantallas los meten en los mensajes de
 * error y en el texto de reglas (plantillas de strings.xml), así el texto
 * nunca dice un número distinto del que se comprueba. Lo único que sigue
 * escrito a mano son los android:maxLength de activity_login.xml y
 * activity_recuperar.xml: si cambia un máximo de aquí hay que tocarlos también.
 */
public final class Validador {

    public static final int USUARIO_MIN = 3;
    public static final int USUARIO_MAX = 20;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;
    public static final int RESPUESTA_MIN = 2;
    public static final int RESPUESTA_MAX = 60;
    public static final int EMAIL_MAX = 254;

    /** Letras, números o guion bajo, entre USUARIO_MIN y USUARIO_MAX veces. */
    private static final Pattern PATRON_USUARIO =
            Pattern.compile("^[a-zA-Z0-9_]{" + USUARIO_MIN + "," + USUARIO_MAX + "}$");

    /**
     * Constructor privado: solo hay métodos estáticos.
     */
    private Validador() {
    }

    /**
     * @param usuario nombre ya sin espacios de los lados
     * @return true si cumple 3-20 [a-zA-Z0-9_]
     */
    public static boolean esUsuarioValido(String usuario) {
        if (usuario == null) {
            return false;
        }
        return PATRON_USUARIO.matcher(usuario).matches();
    }

    /**
     * El email es opcional, así que vacío o null cuenta como válido. Si lo dan,
     * miro la longitud (254 es el máximo que admite una dirección) y el patrón
     * de email que trae Android.
     *
     * @param email email ya sin espacios de los lados (puede ser null)
     * @return true si está vacío o si tiene forma de email y no pasa de EMAIL_MAX
     */
    public static boolean esEmailValido(String email) {
        if (email == null || email.length() == 0) {
            return true;
        }
        if (email.length() > EMAIL_MAX) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * @param password contraseña tal cual la ha escrito
     * @return true si tiene 8-72 caracteres y al menos una letra y un número
     */
    public static boolean esPasswordValida(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return false;
        }
        boolean tieneLetra = false;
        boolean tieneNumero = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isISOControl(c)) {
                return false;
            }
            if (Character.isLetter(c)) {
                tieneLetra = true;
            } else if (Character.isDigit(c)) {
                tieneNumero = true;
            }
        }
        return tieneLetra && tieneNumero;
    }

    /**
     * @param respuesta respuesta de seguridad en claro (se normaliza aquí dentro)
     * @return true si, normalizada, tiene entre 2 y 60 caracteres
     */
    public static boolean esRespuestaValida(String respuesta) {
        String normal = Hash.normalizarRespuesta(respuesta);
        if (normal.length() < RESPUESTA_MIN || normal.length() > RESPUESTA_MAX) {
            return false;
        }
        for (int i = 0; i < normal.length(); i++) {
            if (Character.isISOControl(normal.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
