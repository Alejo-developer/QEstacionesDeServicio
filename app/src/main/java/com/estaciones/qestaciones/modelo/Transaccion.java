package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Transacción de venta de combustible en una estación.
 * Se genera cada vez que se hace una SALIDA por venta al público.
 */
@Entity(tableName = "transacciones")
public class Transaccion {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String placa;           // placa del vehículo comprador (puede ser "SIN_PLACA")
    private String tipoVehiculo;
    private String tipoCombustible;
    private double volumenLitros;
    private double precioUnitario;  // COP por litro aplicado
    private double totalCop;        // volumenLitros × precioUnitario
    private boolean subsidioAplicado;
    private String nombreEstacion;
    private String operador;
    private long fechaVenta;

    // Migración v8 → v9: scope de empresa vendedora y datos del comprador (USUARIO_PARTICULAR).
    // Nullable para preservar transacciones antiguas.
    private String empresaVendedoraUsuario;   // nombreUsuario de la estación o distribuidor que vendió
    private String tipoVendedor;              // "ESTACION" o "DISTRIBUIDOR"
    private String compradorUsuario;          // nombreUsuario del cliente USUARIO_PARTICULAR (null si venta en mostrador)
    private String correoCliente;             // correo al que se envió la factura electrónica (null si no aplica)

    public Transaccion() {
        this.fechaVenta = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String p) { this.placa = p; }

    public String getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(String t) { this.tipoVehiculo = t; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String t) { this.tipoCombustible = t; }

    public double getVolumenLitros() { return volumenLitros; }
    public void setVolumenLitros(double v) { this.volumenLitros = v; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double p) { this.precioUnitario = p; }

    public double getTotalCop() { return totalCop; }
    public void setTotalCop(double t) { this.totalCop = t; }

    public boolean isSubsidioAplicado() { return subsidioAplicado; }
    public void setSubsidioAplicado(boolean s) { this.subsidioAplicado = s; }

    public String getNombreEstacion() { return nombreEstacion; }
    public void setNombreEstacion(String n) { this.nombreEstacion = n; }

    public String getOperador() { return operador; }
    public void setOperador(String o) { this.operador = o; }

    public long getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(long f) { this.fechaVenta = f; }

    public String getEmpresaVendedoraUsuario() { return empresaVendedoraUsuario; }
    public void setEmpresaVendedoraUsuario(String e) { this.empresaVendedoraUsuario = e; }

    public String getTipoVendedor() { return tipoVendedor; }
    public void setTipoVendedor(String t) { this.tipoVendedor = t; }

    public String getCompradorUsuario() { return compradorUsuario; }
    public void setCompradorUsuario(String c) { this.compradorUsuario = c; }

    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String c) { this.correoCliente = c; }
}
