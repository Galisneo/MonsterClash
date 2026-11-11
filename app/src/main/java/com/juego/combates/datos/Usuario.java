/*
 * Proyecto: Monster Clash (juego de combates por turnos para Android)
 * Módulo:   Programación Multimedia y Dispositivos Móviles - 2º DAM
 * Archivo:  Usuario.java
 */
package com.juego.combates.datos;

/**
 * Una fila de la tabla usuarios, sin los campos secretos: aquí no viajan ni el
 * hash de la contraseña ni el de la respuesta, solo lo que la interfaz necesita.
 */
public class Usuario {

    private long id;
    private String usuario;
    private String email;      // puede ser null (es opcional al registrarse)
    private int pregunta;      // índice en el string-array preguntas_seguridad
    private long creadoEn;     // milisegundos desde 1970

    /**
     * @param id       clave primaria
     * @param usuario  nombre de usuario (siempre en minúsculas)
     * @param email    email o null
     * @param pregunta índice de la pregunta de seguridad
     * @param creadoEn fecha de alta en milisegundos
     */
    public Usuario(long id, String usuario, String email, int pregunta, long creadoEn) {
        this.id = id;
        this.usuario = usuario;
        this.email = email;
        this.pregunta = pregunta;
        this.creadoEn = creadoEn;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPregunta() {
        return pregunta;
    }

    public void setPregunta(int pregunta) {
        this.pregunta = pregunta;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}
