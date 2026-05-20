package com.estaciones.qestaciones.modelo;

/**
 * Enum que define los roles de usuario en la plataforma.
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    OPERADOR("Operador"),
    DISTRIBUIDOR("Distribuidor"),
    ESTACION("Estación"),
    USUARIO_PARTICULAR("Usuario Particular"),
    REGULADOR("Autoridad Reguladora");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
