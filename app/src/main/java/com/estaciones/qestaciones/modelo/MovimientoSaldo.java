package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Historial de movimientos de saldo de un usuario.
 *
 * Tipos:
 *  - RECARGA: el usuario recargó su saldo desde la pantalla "Mi Saldo".
 *  - PAGO: el usuario pagó (saldo bajó) — compra a una empresa o pago a empleado.
 *  - INGRESO: el usuario recibió plata (saldo subió) — venta o sueldo recibido.
 *
 * Los movimientos se generan siempre en pares para transferencias entre usuarios:
 * un PAGO en el comprador + un INGRESO en el vendedor con el mismo monto.
 *
 * Los datos de tarjeta NO se guardan completos; solo método y últimos 4 dígitos.
 *
 * El @Index sobre usuarioNombre debe declararse aquí porque la migración 10→11
 * crea ese mismo índice (idx_movimientos_saldo_usuario) en la BD; Room compara
 * lo declarado con lo existente y si no coincide aborta el arranque.
 */
@Entity(
    tableName = "movimientos_saldo",
    indices = {@Index(value = "usuarioNombre", name = "idx_movimientos_saldo_usuario")}
)
public class MovimientoSaldo {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String usuarioNombre;     // nombreUsuario afectado
    private String tipo;              // RECARGA | PAGO | INGRESO
    private double monto;             // monto del movimiento (positivo siempre)
    private double saldoAnterior;
    private double saldoNuevo;
    private String concepto;          // descripción legible
    private String contraparte;       // nombreUsuario del otro lado (null para recargas)
    private String metodoPago;        // TARJETA_CREDITO | TARJETA_DEBITO | PAYPAL | TRANSFERENCIA_INTERNA
    private String tarjetaUltimos4;   // ej. "1234" — null si no aplica
    private long fecha;

    public MovimientoSaldo() {
        this.fecha = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public double getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(double saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public double getSaldoNuevo() { return saldoNuevo; }
    public void setSaldoNuevo(double saldoNuevo) { this.saldoNuevo = saldoNuevo; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getContraparte() { return contraparte; }
    public void setContraparte(String contraparte) { this.contraparte = contraparte; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getTarjetaUltimos4() { return tarjetaUltimos4; }
    public void setTarjetaUltimos4(String tarjetaUltimos4) { this.tarjetaUltimos4 = tarjetaUltimos4; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
}
