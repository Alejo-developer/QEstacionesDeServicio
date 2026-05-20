package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que representa un usuario de la plataforma.
 * HU-01: Autenticación segura con control de acceso por rol.
 * HU-02: 2FA con Correo Electrónico.
 */
@Entity(tableName = "usuarios")
public class Usuario {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombreUsuario;
    private String contrasena;
    private String nombreCompleto;
    private String correo;
    private String rol;
    private boolean bloqueado;
    private long fechaBanHasta; // 0 = sin ban temporal; > 0 = timestamp de expiración
    private int intentosFallidos;
    private long fechaCreacion;
    private String codigoVerificacion;
    private boolean verificado;

    // Campos de perfil (USUARIO_PARTICULAR) y de empresa (ESTACION/DISTRIBUIDOR).
    // Agregados en migración Room v8 → v9, nullable para preservar datos existentes.
    private String nit;
    private String cedula;
    private String telefono;
    private String direccion;
    private String ciudad;

    // Migración v9 → v10: usuarios OPERADOR trabajan para una empresa (nombreUsuario
    // de una estación). Cuando un operador hace una venta, el scope de inventario
    // es esta empresa, no su propio nombreUsuario.
    private String empresaAsignada;

    // Migración v10 → v11: saldo del usuario en COP (para compra/venta digital).
    // No aplica al ADMINISTRADOR (solo observa). Default 0.
    private double saldoCop;

    // Migración v11 → v12: ubicación geográfica del usuario (ESTACION = ubicación del
    // surtidor; DISTRIBUIDOR = origen de la ruta del día). Double (boxed) para soportar
    // NULL cuando el usuario aún no ha registrado coordenadas.
    private Double latitud;
    private Double longitud;

    // Constructor
    public Usuario() {
        this.bloqueado = false;
        this.intentosFallidos = 0;
        this.fechaCreacion = System.currentTimeMillis();
        this.verificado = false;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public long getFechaBanHasta() { return fechaBanHasta; }
    public void setFechaBanHasta(long fechaBanHasta) { this.fechaBanHasta = fechaBanHasta; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getCodigoVerificacion() { return codigoVerificacion; }
    public void setCodigoVerificacion(String codigoVerificacion) { this.codigoVerificacion = codigoVerificacion; }
    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getEmpresaAsignada() { return empresaAsignada; }
    public void setEmpresaAsignada(String empresaAsignada) { this.empresaAsignada = empresaAsignada; }

    public double getSaldoCop() { return saldoCop; }
    public void setSaldoCop(double saldoCop) { this.saldoCop = saldoCop; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}
