package com.estaciones.qestaciones.hu06_zonas;

import android.content.Context;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.ZonaReguladaDao;
import com.estaciones.qestaciones.modelo.ZonaRegulada;

import java.util.List;

/**
 * Gestor de lógica de negocio para zonas reguladas.
 * HU-06: Configuración por municipio, versionamiento y aplicación automática.
 */
public class GestorZonas {

    private final ZonaReguladaDao zonaDao;

    public GestorZonas(Context contexto) {
        BaseDatosEstaciones db = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.zonaDao = db.zonaReguladaDao();
    }

    /**
     * Resultado de una operación de zona.
     */
    public static class ResultadoZona {
        private final boolean exitoso;
        private final String mensaje;

        public ResultadoZona(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
    }

    /**
     * Crea o actualiza una zona regulada.
     * Si ya existe una zona para el municipio, crea una nueva versión.
     */
    public ResultadoZona crearOActualizarZona(String municipio, String departamento,
                                               String codigoZona, String creadoPor) {
        if (municipio == null || municipio.trim().isEmpty()) {
            return new ResultadoZona(false, "El municipio es requerido");
        }
        if (departamento == null || departamento.trim().isEmpty()) {
            return new ResultadoZona(false, "El departamento es requerido");
        }
        if (codigoZona == null || codigoZona.trim().isEmpty()) {
            return new ResultadoZona(false, "El código de zona es requerido");
        }

        // Verificar si ya existe una zona activa con ese código
        ZonaRegulada existente = zonaDao.obtenerVersionActual(municipio.trim());

        ZonaRegulada zona = new ZonaRegulada();
        zona.setMunicipio(municipio.trim());
        zona.setDepartamento(departamento.trim());
        zona.setCodigoZona(codigoZona.trim());
        zona.setCreadoPor(creadoPor);

        if (existente != null) {
            // Desactivar la versión anterior
            existente.setActiva(false);
            existente.setFechaModificacion(System.currentTimeMillis());
            zonaDao.actualizar(existente);

            // Crear nueva versión
            zona.setVersion(existente.getVersion() + 1);
            zonaDao.insertar(zona);

            return new ResultadoZona(true,
                    "Zona actualizada a versión " + zona.getVersion());
        } else {
            // Primera versión
            zona.setVersion(1);
            zonaDao.insertar(zona);
            return new ResultadoZona(true, "Zona creada exitosamente (v1)");
        }
    }

    /**
     * Obtiene todas las zonas activas.
     */
    public List<ZonaRegulada> obtenerZonasActivas() {
        return zonaDao.obtenerActivas();
    }

    /**
     * Obtiene todas las zonas (activas e inactivas) para historial.
     */
    public List<ZonaRegulada> obtenerTodasLasZonas() {
        return zonaDao.obtenerTodas();
    }

    /**
     * Obtiene el historial de versiones de una zona por municipio.
     */
    public List<ZonaRegulada> obtenerVersiones(String municipio) {
        return zonaDao.obtenerPorMunicipio(municipio);
    }

    /**
     * Desactiva una zona regulada.
     */
    public ResultadoZona desactivarZona(ZonaRegulada zona) {
        zona.setActiva(false);
        zona.setFechaModificacion(System.currentTimeMillis());
        zonaDao.actualizar(zona);
        return new ResultadoZona(true, "Zona desactivada");
    }
}
