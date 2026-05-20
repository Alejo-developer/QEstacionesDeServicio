package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que registra los accesos al sistema.
 * HU-01: Trazabilidad y registro de accesos.
 */
@Entity(tableName = "registros_acceso")
public class RegistroAcceso {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int idUsuario;
    private long fechaHora;
    private String tipoAcceso; // "INGRESO", "FALLO", "BLOQUEO"
    private String nombreUsuario;

    // Constructor
    public RegistroAcceso() {
        this.fechaHora = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public long getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(long fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getTipoAcceso() {
        return tipoAcceso;
    }

    public void setTipoAcceso(String tipoAcceso) {
        this.tipoAcceso = tipoAcceso;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}
