package com.estaciones.qestaciones.hu05_subsidios;

import android.content.Context;

import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.TipoVehiculo;
import com.estaciones.qestaciones.modelo.ValidacionSubsidio;
import com.estaciones.qestaciones.modelo.Vehiculo;

import java.util.List;

/**
 * Gestor de validacion de subsidios (HU-05).
 *
 * Integra con ApiTransitoServicio para consultar vehiculos por placa.
 * Ya no usa HashMap estatico propio; toda la consulta pasa por la API de transito.
 */
public class GestorSubsidios {

    private final BaseDatosEstaciones baseDatos;
    private final ApiTransitoServicio apiTransito;

    public GestorSubsidios(Context contexto) {
        this.baseDatos = BaseDatosEstaciones.obtenerInstancia(contexto);
        this.apiTransito = new ApiTransitoServicio();
    }

    public static class ResultadoValidacion {
        private final boolean aplicaSubsidio;
        private final double precioAplicado;
        private final String tipoVehiculo;
        private final String motivoRechazo;
        private final String propietario;
        private final String fuenteDatos; // HU-05: indica de donde se obtuvo la info

        public ResultadoValidacion(boolean aplicaSubsidio, double precioAplicado,
                                    String tipoVehiculo, String motivoRechazo,
                                    String propietario, String fuenteDatos) {
            this.aplicaSubsidio = aplicaSubsidio;
            this.precioAplicado = precioAplicado;
            this.tipoVehiculo = tipoVehiculo;
            this.motivoRechazo = motivoRechazo;
            this.propietario = propietario;
            this.fuenteDatos = fuenteDatos;
        }

        public boolean isAplicaSubsidio() { return aplicaSubsidio; }
        public double getPrecioAplicado() { return precioAplicado; }
        public String getTipoVehiculo() { return tipoVehiculo; }
        public String getMotivoRechazo() { return motivoRechazo; }
        public String getPropietario() { return propietario; }
        public String getFuenteDatos() { return fuenteDatos; }
    }

    /**
     * Valida subsidio consultando la API de transito (sincrono).
     */
    public ResultadoValidacion validarSubsidio(String placa) {
        placa = placa.toUpperCase().trim();

        // ── HU-05: Consultar API de Transito ──
        ApiTransitoServicio.RespuestaApi respuestaApi = apiTransito.consultarPlacaSincrono(placa);

        if (!respuestaApi.isExitosa() || respuestaApi.getVehiculo() == null) {
            // Error en la API
            ValidacionSubsidio validacion = crearRegistroValidacion(placa, "RECHAZADO", 0,
                    "Error al consultar API de transito: " + respuestaApi.getMensaje(), "DESCONOCIDO");
            baseDatos.validacionSubsidioDao().insertar(validacion);

            return new ResultadoValidacion(false, 0, "DESCONOCIDO",
                    "Error API transito (codigo " + respuestaApi.getCodigoEstado() + "): " + respuestaApi.getMensaje(),
                    "N/A", "API RUNT - Error");
        }

        Vehiculo vehiculo = respuestaApi.getVehiculo();
        String fuente = "API RUNT (codigo " + respuestaApi.getCodigoEstado()
                + ", " + respuestaApi.getTiempoRespuestaMs() + "ms)";

        // Verificar si es servicio publico (exclusion automatica)
        if (vehiculo.isEsServicioPublico() || TipoVehiculo.SERVICIO_PUBLICO.name().equals(vehiculo.getTipoVehiculo())) {
            ReglaPrecios regla = buscarReglaPrecios(vehiculo.getTipoVehiculo());
            double precioBase = regla != null ? regla.getPrecioBase() : 0;

            ValidacionSubsidio validacion = crearRegistroValidacion(placa, "RECHAZADO", precioBase,
                    "Servicio publico excluido del subsidio", vehiculo.getTipoVehiculo());
            baseDatos.validacionSubsidioDao().insertar(validacion);

            return new ResultadoValidacion(false, precioBase, vehiculo.getTipoVehiculo(),
                    "Servicio publico excluido del subsidio", vehiculo.getPropietario(), fuente);
        }

        // Buscar regla de precios aplicable
        ReglaPrecios regla = buscarReglaPrecios(vehiculo.getTipoVehiculo());

        if (regla == null) {
            List<ReglaPrecios> todas = baseDatos.reglaPreciosDao().obtenerTodas();
            if (!todas.isEmpty()) {
                regla = todas.get(0);
            }
        }

        if (regla == null) {
            ValidacionSubsidio validacion = crearRegistroValidacion(placa, "RECHAZADO", 0,
                    "Sin reglas de precios en el sistema", vehiculo.getTipoVehiculo());
            baseDatos.validacionSubsidioDao().insertar(validacion);

            return new ResultadoValidacion(false, 0, vehiculo.getTipoVehiculo(),
                    "Sin reglas de precios configuradas", vehiculo.getPropietario(), fuente);
        }

        // Subsidio aplica
        double precioFinal = regla.getPrecioSubsidiado();
        ValidacionSubsidio validacion = crearRegistroValidacion(placa, "APROBADO", precioFinal, null, vehiculo.getTipoVehiculo());
        baseDatos.validacionSubsidioDao().insertar(validacion);

        return new ResultadoValidacion(true, precioFinal, vehiculo.getTipoVehiculo(), null,
                vehiculo.getPropietario(), fuente);
    }

    /**
     * HU-05: Consulta asincrona a la API de transito (para UI con loading).
     */
    public void validarSubsidioAsync(String placa, CallbackValidacion callback) {
        final String placaFinal = placa.toUpperCase().trim();

        apiTransito.consultarPlaca(placaFinal, respuestaApi -> {
            if (!respuestaApi.isExitosa() || respuestaApi.getVehiculo() == null) {
                callback.onResultado(new ResultadoValidacion(false, 0, "DESCONOCIDO",
                        "Error API transito: " + respuestaApi.getMensaje(),
                        "N/A", "API RUNT - Error"));
                return;
            }

            // Procesar validacion en hilo principal (Room permite main thread queries)
            ResultadoValidacion resultado = procesarVehiculo(
                    placaFinal, respuestaApi.getVehiculo(), respuestaApi);
            callback.onResultado(resultado);
        });
    }

    public interface CallbackValidacion {
        void onResultado(ResultadoValidacion resultado);
    }

    private ResultadoValidacion procesarVehiculo(String placa, Vehiculo vehiculo,
                                                  ApiTransitoServicio.RespuestaApi respuestaApi) {
        String fuente = "API RUNT (codigo " + respuestaApi.getCodigoEstado()
                + ", " + respuestaApi.getTiempoRespuestaMs() + "ms)";

        if (vehiculo.isEsServicioPublico() || TipoVehiculo.SERVICIO_PUBLICO.name().equals(vehiculo.getTipoVehiculo())) {
            ReglaPrecios regla = buscarReglaPrecios(vehiculo.getTipoVehiculo());
            double precioBase = regla != null ? regla.getPrecioBase() : 0;

            ValidacionSubsidio val = crearRegistroValidacion(placa, "RECHAZADO", precioBase,
                    "Servicio publico excluido del subsidio", vehiculo.getTipoVehiculo());
            baseDatos.validacionSubsidioDao().insertar(val);

            return new ResultadoValidacion(false, precioBase, vehiculo.getTipoVehiculo(),
                    "Servicio publico excluido del subsidio", vehiculo.getPropietario(), fuente);
        }

        ReglaPrecios regla = buscarReglaPrecios(vehiculo.getTipoVehiculo());
        if (regla == null) {
            List<ReglaPrecios> todas = baseDatos.reglaPreciosDao().obtenerTodas();
            if (!todas.isEmpty()) regla = todas.get(0);
        }

        if (regla == null) {
            ValidacionSubsidio val = crearRegistroValidacion(placa, "RECHAZADO", 0,
                    "Sin reglas de precios en el sistema", vehiculo.getTipoVehiculo());
            baseDatos.validacionSubsidioDao().insertar(val);
            return new ResultadoValidacion(false, 0, vehiculo.getTipoVehiculo(),
                    "Sin reglas de precios configuradas", vehiculo.getPropietario(), fuente);
        }

        double precioFinal = regla.getPrecioSubsidiado();
        ValidacionSubsidio val = crearRegistroValidacion(placa, "APROBADO", precioFinal, null, vehiculo.getTipoVehiculo());
        baseDatos.validacionSubsidioDao().insertar(val);
        return new ResultadoValidacion(true, precioFinal, vehiculo.getTipoVehiculo(), null,
                vehiculo.getPropietario(), fuente);
    }

    private ReglaPrecios buscarReglaPrecios(String tipo) {
        ReglaPrecios r = baseDatos.reglaPreciosDao().obtenerPorZonaYTipo("Bogota", tipo);
        if (r == null) {
            List<ReglaPrecios> lista = baseDatos.reglaPreciosDao().obtenerTodas();
            for (ReglaPrecios item : lista) {
                if (item.getTipoVehiculo().equals(tipo)) return item;
            }
        }
        return r;
    }

    public List<ValidacionSubsidio> obtenerHistorialValidaciones() {
        return baseDatos.validacionSubsidioDao().obtenerTodas();
    }

    private ValidacionSubsidio crearRegistroValidacion(String placa, String resultado,
                                                         double precioAplicado, String motivoRechazo,
                                                         String tipoVehiculo) {
        ValidacionSubsidio validacion = new ValidacionSubsidio();
        validacion.setPlaca(placa);
        validacion.setResultado(resultado);
        validacion.setPrecioAplicado(precioAplicado);
        validacion.setMotivoRechazo(motivoRechazo != null ? motivoRechazo : "");
        validacion.setTipoVehiculo(tipoVehiculo);
        return validacion;
    }
}
