package com.estaciones.qestaciones.hu09_entregas;

import android.content.Context;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.RegistroEntregaDao;
import com.estaciones.qestaciones.modelo.RegistroEntrega;

import java.util.List;

/**
 * Gestor de lógica de negocio para registro de entregas.
 * HU-09: Registro estructurado con trazabilidad completa.
 */
public class GestorEntregas {

    private final RegistroEntregaDao entregaDao;

    public GestorEntregas(Context contexto) {
        BaseDatosEstaciones db = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.entregaDao = db.registroEntregaDao();
    }

    /**
     * Resultado de una operación de entrega.
     */
    public static class ResultadoEntrega {
        private final boolean exitoso;
        private final String mensaje;

        public ResultadoEntrega(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
    }

    /**
     * Registra una nueva entrega de combustible.
     */
    public ResultadoEntrega registrarEntrega(String estacionDestino, String vehiculo,
                                              String placa, String volumenStr,
                                              String tipoCombustible, String observaciones,
                                              String registradoPor) {
        // Validaciones
        if (estacionDestino == null || estacionDestino.trim().isEmpty()) {
            return new ResultadoEntrega(false, "La estación destino es requerida");
        }
        if (vehiculo == null || vehiculo.trim().isEmpty()) {
            return new ResultadoEntrega(false, "El vehículo de transporte es requerido");
        }
        if (placa == null || placa.trim().isEmpty()) {
            return new ResultadoEntrega(false, "La placa del vehículo es requerida");
        }

        double volumen;
        try {
            volumen = Double.parseDouble(volumenStr);
            if (volumen <= 0) {
                return new ResultadoEntrega(false, "El volumen debe ser mayor a 0");
            }
        } catch (NumberFormatException e) {
            return new ResultadoEntrega(false, "Ingrese un volumen válido");
        }

        // Crear registro
        RegistroEntrega entrega = new RegistroEntrega();
        entrega.setEstacionDestino(estacionDestino.trim());
        entrega.setVehiculoTransporte(vehiculo.trim());
        entrega.setPlacaVehiculo(placa.trim().toUpperCase());
        entrega.setVolumenLitros(volumen);
        entrega.setTipoCombustible(tipoCombustible != null ? tipoCombustible : "ACPM");
        entrega.setObservaciones(observaciones != null ? observaciones.trim() : "");
        entrega.setRegistradoPor(registradoPor);

        entregaDao.insertar(entrega);

        return new ResultadoEntrega(true,
                String.format("Entrega registrada: %.0f litros a %s", volumen, estacionDestino));
    }

    /**
     * Obtiene todas las entregas registradas.
     */
    public List<RegistroEntrega> obtenerTodasLasEntregas() {
        return entregaDao.obtenerTodas();
    }

    /**
     * Obtiene entregas filtradas por estación.
     */
    public List<RegistroEntrega> obtenerPorEstacion(String estacion) {
        return entregaDao.obtenerPorEstacion(estacion);
    }

    /**
     * HU-14: Obtiene entregas aplicando filtros combinados.
     *
     * @param estacion    Texto parcial del nombre de estacion (vacio = todas)
     * @param combustible Tipo exacto de combustible (vacio = todos)
     * @param inicio      Timestamp inicio del rango (0 = sin limite inferior)
     * @param fin         Timestamp fin del rango (Long.MAX_VALUE = sin limite superior)
     */
    public List<RegistroEntrega> obtenerConFiltros(String estacion, String combustible,
                                                    long inicio, long fin) {
        String est = estacion != null ? estacion.trim() : "";
        String comb = combustible != null ? combustible : "";
        long desde = inicio > 0 ? inicio : 0;
        long hasta = fin > 0 ? fin : Long.MAX_VALUE;
        return entregaDao.obtenerConFiltros(est, comb, desde, hasta);
    }
}
