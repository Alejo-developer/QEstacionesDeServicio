package com.estaciones.qestaciones.modelo;

/**
 * Clase que representa un vehículo para validación de subsidios.
 * HU-05: Validación por placa.
 * No es entidad Room porque se obtiene de validación externa simulada.
 */
public class Vehiculo {

    private String placa;
    private String tipoVehiculo; // Valor del enum TipoVehiculo
    private String propietario;
    private boolean esServicioPublico;

    // Constructor
    public Vehiculo() {
    }

    public Vehiculo(String placa, String tipoVehiculo, String propietario, boolean esServicioPublico) {
        this.placa = placa;
        this.tipoVehiculo = tipoVehiculo;
        this.propietario = propietario;
        this.esServicioPublico = esServicioPublico;
    }

    // Getters y Setters
    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

    public boolean isEsServicioPublico() {
        return esServicioPublico;
    }

    public void setEsServicioPublico(boolean esServicioPublico) {
        this.esServicioPublico = esServicioPublico;
    }
}
