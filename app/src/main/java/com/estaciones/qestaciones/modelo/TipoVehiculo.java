package com.estaciones.qestaciones.modelo;

/**
 * Enum que define los tipos de vehículo para las reglas de precios.
 * HU-03: Configuración de precios ACPM por tipo de vehículo.
 */
public enum TipoVehiculo {
    PARTICULAR("Particular"),
    CARGA("Carga"),
    SERVICIO_PUBLICO("Servicio Público"),
    MOTOCICLETA("Motocicleta");

    private final String descripcion;

    TipoVehiculo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
