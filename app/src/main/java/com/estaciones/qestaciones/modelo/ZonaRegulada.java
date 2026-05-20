package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que representa una zona regulada para precios y subsidios.
 * HU-06: Definir zonas reguladas con versionamiento por municipio.
 */
@Entity(tableName = "zonas_reguladas")
public class ZonaRegulada {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String municipio;
    private String departamento;
    private String codigoZona;
    private boolean activa;
    private int version;
    private long fechaCreacion;
    private long fechaModificacion;
    private String creadoPor;

    // Constructor
    public ZonaRegulada() {
        this.activa = true;
        this.version = 1;
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaModificacion = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getCodigoZona() { return codigoZona; }
    public void setCodigoZona(String codigoZona) { this.codigoZona = codigoZona; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public long getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(long fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
}
