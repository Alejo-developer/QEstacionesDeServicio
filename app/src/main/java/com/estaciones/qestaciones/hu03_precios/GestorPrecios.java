package com.estaciones.qestaciones.hu03_precios;

import android.content.Context;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.TipoVehiculo;

import java.util.List;

/**
 * Gestor de precios ACPM.
 * HU-03: Configuración de reglas de precios por vehículo y zona.
 * Cumplimiento del Decreto 1428 de 2025.
 *
 * Funcionalidades:
 * - Crear reglas de precios diferenciados
 * - Excluir automáticamente el servicio público del subsidio
 * - Consultar historial normativo
 * - Obtener precio en tiempo real por zona y tipo de vehículo
 */
public class GestorPrecios {

    private final BaseDatosEstaciones baseDatos;
    private final NotificadorCambiosNormativos notificador;

    public GestorPrecios(Context contexto) {
        this.baseDatos = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.notificador = new NotificadorCambiosNormativos(contexto);
    }

    /**
     * Crea una nueva regla de precios.
     * Si el tipo de vehículo es SERVICIO_PUBLICO, desactiva el subsidio automáticamente.
     */
    public String crearRegla(String zona, String tipoVehiculo, double precioBase,
                              double precioSubsidiado, String decreto) {
        ReglaPrecios regla = new ReglaPrecios();
        regla.setZona(zona);
        regla.setTipoVehiculo(tipoVehiculo);
        regla.setPrecioBase(precioBase);
        regla.setDecreto(decreto);

        // HU-03: Exclusión automática de servicio público
        if (TipoVehiculo.SERVICIO_PUBLICO.name().equals(tipoVehiculo)) {
            regla.setAplicaSubsidio(false);
            regla.setPrecioSubsidiado(precioBase); // Sin subsidio
        } else {
            regla.setAplicaSubsidio(true);
            regla.setPrecioSubsidiado(precioSubsidiado);
        }

        baseDatos.reglaPreciosDao().insertarRegla(regla);

        // HU-07: Notificar a la estacion de servicio sobre el cambio normativo
        notificador.notificarCambio(zona, tipoVehiculo, decreto);

        return "Regla de precios guardada exitosamente";
    }

    /**
     * Obtiene el precio aplicable para una zona y tipo de vehículo.
     * Aplicación en tiempo real.
     */
    public ReglaPrecios obtenerPrecioActual(String zona, String tipoVehiculo) {
        return baseDatos.reglaPreciosDao().obtenerPorZonaYTipo(zona, tipoVehiculo);
    }

    /**
     * Obtiene todas las reglas de precios activas.
     */
    public List<ReglaPrecios> obtenerReglasActivas() {
        return baseDatos.reglaPreciosDao().obtenerTodas();
    }

    /**
     * Obtiene el historial normativo (todas las reglas ordenadas cronológicamente).
     */
    public List<ReglaPrecios> obtenerHistorialNormativo() {
        return baseDatos.reglaPreciosDao().obtenerHistorial();
    }

    /**
     * HU-15: Obtiene el historial filtrado por rango de fechas.
     */
    public List<ReglaPrecios> obtenerHistorialPorFechas(long fechaInicio, long fechaFin) {
        return baseDatos.reglaPreciosDao().obtenerHistorialPorFechas(fechaInicio, fechaFin);
    }

    /**
     * HU-15: Obtiene el historial filtrado por zona y rango de fechas.
     */
    public List<ReglaPrecios> obtenerHistorialPorZonaYFechas(String zona, long fechaInicio, long fechaFin) {
        return baseDatos.reglaPreciosDao().obtenerHistorialPorZonaYFechas(zona, fechaInicio, fechaFin);
    }

    /**
     * HU-15: Cuenta el total de reglas registradas.
     */
    public int contarReglas() {
        return baseDatos.reglaPreciosDao().contarReglas();
    }
}
