package com.estaciones.qestaciones.hu13_inventarios;

import android.content.Context;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.MovimientoInventarioDao;
import com.estaciones.qestaciones.modelo.MovimientoInventario;

import java.util.List;

/**
 * Gestor de lógica de negocio para control de inventarios.
 * HU-13: Historial de movimientos, datos auditables, reducción de pérdidas.
 * HU-13 DOING: Notificaciones de stock bajo mediante NotificadorStock.
 */
public class GestorInventarios {

    private final MovimientoInventarioDao movimientoDao;
    private final NotificadorStock notificador;

    public GestorInventarios(Context contexto) {
        BaseDatosEstaciones db = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.movimientoDao = db.movimientoInventarioDao();
        this.notificador = new NotificadorStock(contexto);
    }

    /**
     * Resultado de una operación de inventario.
     */
    public static class ResultadoInventario {
        private final boolean exitoso;
        private final String mensaje;
        private final double saldoActual;

        public ResultadoInventario(boolean exitoso, String mensaje, double saldoActual) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.saldoActual = saldoActual;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public double getSaldoActual() { return saldoActual; }
    }

    /**
     * Registra un movimiento de inventario.
     * Calcula automáticamente el saldo anterior y nuevo.
     */
    public ResultadoInventario registrarMovimiento(String tipoMovimiento,
                                                    String tipoCombustible,
                                                    String volumenStr,
                                                    String motivo,
                                                    String responsable) {
        // Validaciones
        if (tipoMovimiento == null || tipoMovimiento.trim().isEmpty()) {
            return new ResultadoInventario(false, "Seleccione el tipo de movimiento", 0);
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            return new ResultadoInventario(false, "El motivo es requerido", 0);
        }

        double volumen;
        try {
            volumen = Double.parseDouble(volumenStr);
            if (volumen <= 0) {
                return new ResultadoInventario(false, "El volumen debe ser mayor a 0", 0);
            }
        } catch (NumberFormatException e) {
            return new ResultadoInventario(false, "Ingrese un volumen válido", 0);
        }

        String combustible = tipoCombustible != null ? tipoCombustible : "ACPM";

        // Obtener saldo actual
        double saldoAnterior = movimientoDao.obtenerSaldoActual(combustible);

        // Calcular nuevo saldo
        double saldoNuevo;
        switch (tipoMovimiento) {
            case "ENTRADA":
                saldoNuevo = saldoAnterior + volumen;
                break;
            case "SALIDA":
                if (volumen > saldoAnterior) {
                    return new ResultadoInventario(false,
                            String.format("Saldo insuficiente. Disponible: %.0f litros", saldoAnterior), saldoAnterior);
                }
                saldoNuevo = saldoAnterior - volumen;
                break;
            case "AJUSTE":
                saldoNuevo = volumen; // El ajuste establece el nuevo saldo directamente
                volumen = Math.abs(saldoNuevo - saldoAnterior);
                break;
            default:
                return new ResultadoInventario(false, "Tipo de movimiento no válido", 0);
        }

        // Crear movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setTipoCombustible(combustible);
        movimiento.setVolumenLitros(volumen);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setMotivo(motivo.trim());
        movimiento.setResponsable(responsable);
        movimiento.setEsAuditable(true);

        movimientoDao.insertar(movimiento);

        // HU-13 DOING: Verificar stock bajo y notificar si es necesario
        if ("SALIDA".equals(tipoMovimiento) || "AJUSTE".equals(tipoMovimiento)) {
            notificador.verificarYNotificar(combustible, saldoNuevo);
        }

        return new ResultadoInventario(true,
                String.format("%s registrada: %.0f litros. Saldo: %.0f litros",
                        tipoMovimiento, volumen, saldoNuevo),
                saldoNuevo);
    }

    /**
     * Obtiene el saldo actual de un tipo de combustible.
     */
    public double obtenerSaldoActual(String tipoCombustible) {
        return movimientoDao.obtenerSaldoActual(tipoCombustible);
    }

    /**
     * Obtiene todos los movimientos.
     */
    public List<MovimientoInventario> obtenerTodosLosMovimientos() {
        return movimientoDao.obtenerTodos();
    }

    /**
     * Obtiene solo los movimientos auditables.
     */
    public List<MovimientoInventario> obtenerMovimientosAuditables() {
        return movimientoDao.obtenerAuditables();
    }

    /**
     * Obtiene movimientos filtrados por tipo.
     */
    public List<MovimientoInventario> obtenerPorTipo(String tipo) {
        return movimientoDao.obtenerPorTipo(tipo);
    }
}
