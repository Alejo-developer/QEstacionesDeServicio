package com.estaciones.qestaciones.hu05_subsidios;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.estaciones.qestaciones.modelo.TipoVehiculo;
import com.estaciones.qestaciones.modelo.Vehiculo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio de conexion a la API oficial de transito (HU-05).
 *
 * Simula una consulta HTTP REST a la API del RUNT (Registro Unico Nacional de Transito).
 * En produccion se reemplazaria el contenido de consultarPlacaRemota() por una
 * llamada real con HttpURLConnection o Retrofit al endpoint:
 *   https://api.runt.gov.co/v1/vehiculos/{placa}
 *
 * Patron de diseno: Async con callback + hilo secundario,
 * listo para ser reemplazado por Retrofit sin cambiar la interfaz publica.
 */
public class ApiTransitoServicio {

    private static final String TAG = "ApiTransitoServicio";
    private static final String API_BASE_URL = "https://api.runt.gov.co/v1/vehiculos/";
    private static final int TIMEOUT_MS = 5000;

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    // Base de datos simulada del RUNT (en produccion se elimina)
    private static final Map<String, Vehiculo> BASE_RUNT_SIMULADA = new HashMap<>();

    static {
        BASE_RUNT_SIMULADA.put("ABC123", new Vehiculo("ABC123", TipoVehiculo.PARTICULAR.name(), "Juan Perez", false));
        BASE_RUNT_SIMULADA.put("DEF456", new Vehiculo("DEF456", TipoVehiculo.CARGA.name(), "Transportes S.A.", false));
        BASE_RUNT_SIMULADA.put("GHI789", new Vehiculo("GHI789", TipoVehiculo.SERVICIO_PUBLICO.name(), "Bus Urbano Ltda.", true));
        BASE_RUNT_SIMULADA.put("JKL012", new Vehiculo("JKL012", TipoVehiculo.MOTOCICLETA.name(), "Maria Lopez", false));
        BASE_RUNT_SIMULADA.put("MNO345", new Vehiculo("MNO345", TipoVehiculo.SERVICIO_PUBLICO.name(), "Taxi Express", true));
        BASE_RUNT_SIMULADA.put("PQR678", new Vehiculo("PQR678", TipoVehiculo.PARTICULAR.name(), "Carlos Gomez", false));
        BASE_RUNT_SIMULADA.put("ASD102", new Vehiculo("ASD102", TipoVehiculo.PARTICULAR.name(), "Usuario Demo", false));
    }

    /**
     * Resultado de la consulta a la API de transito.
     */
    public static class RespuestaApi {
        private final boolean exitosa;
        private final Vehiculo vehiculo;
        private final String codigoEstado; // "200", "404", "503"
        private final String mensaje;
        private final long tiempoRespuestaMs;

        public RespuestaApi(boolean exitosa, Vehiculo vehiculo, String codigoEstado, String mensaje, long tiempoRespuestaMs) {
            this.exitosa = exitosa;
            this.vehiculo = vehiculo;
            this.codigoEstado = codigoEstado;
            this.mensaje = mensaje;
            this.tiempoRespuestaMs = tiempoRespuestaMs;
        }

        public boolean isExitosa() { return exitosa; }
        public Vehiculo getVehiculo() { return vehiculo; }
        public String getCodigoEstado() { return codigoEstado; }
        public String getMensaje() { return mensaje; }
        public long getTiempoRespuestaMs() { return tiempoRespuestaMs; }
    }

    /**
     * Callback para respuesta asincrona.
     */
    public interface CallbackApi {
        void onRespuesta(RespuestaApi respuesta);
    }

    /**
     * Consulta la API de transito para obtener informacion de un vehiculo por placa.
     * Se ejecuta en hilo secundario y devuelve resultado en hilo principal.
     *
     * @param placa    Placa del vehiculo en formato colombiano (ej: ABC123)
     * @param callback Callback con la respuesta
     */
    public void consultarPlaca(String placa, CallbackApi callback) {
        final String placaNormalizada = placa.toUpperCase().trim();

        Log.i(TAG, "Iniciando consulta API RUNT para placa: " + placaNormalizada);
        Log.i(TAG, "URL: " + API_BASE_URL + placaNormalizada);

        ejecutor.execute(() -> {
            long inicio = System.currentTimeMillis();

            try {
                // ═══════════════════════════════════════════════════════════
                // SIMULACION DE LLAMADA HTTP
                // En produccion, reemplazar este bloque por:
                //
                // URL url = new URL(API_BASE_URL + placaNormalizada);
                // HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // conn.setRequestMethod("GET");
                // conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);
                // conn.setRequestProperty("Accept", "application/json");
                // conn.setConnectTimeout(TIMEOUT_MS);
                // conn.setReadTimeout(TIMEOUT_MS);
                // int responseCode = conn.getResponseCode();
                // ... parsear JSON con Gson/Jackson ...
                // ═══════════════════════════════════════════════════════════

                // Simular latencia de red (200-800ms)
                Thread.sleep(200 + new Random().nextInt(600));

                RespuestaApi respuesta = consultarPlacaSimulada(placaNormalizada, inicio);

                long tiempoTotal = System.currentTimeMillis() - inicio;
                Log.i(TAG, "Respuesta API en " + tiempoTotal + "ms - Codigo: " + respuesta.getCodigoEstado());

                handlerPrincipal.post(() -> callback.onRespuesta(respuesta));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                long tiempo = System.currentTimeMillis() - inicio;
                handlerPrincipal.post(() -> callback.onRespuesta(
                        new RespuestaApi(false, null, "TIMEOUT",
                                "Tiempo de espera agotado al consultar API de transito", tiempo)));
            } catch (Exception e) {
                Log.e(TAG, "Error en consulta API: " + e.getMessage(), e);
                long tiempo = System.currentTimeMillis() - inicio;
                handlerPrincipal.post(() -> callback.onRespuesta(
                        new RespuestaApi(false, null, "500",
                                "Error de conexion con la API de transito: " + e.getMessage(), tiempo)));
            }
        });
    }

    /**
     * Consulta sincrona (para usar desde GestorSubsidios en el hilo principal
     * cuando se necesita resultado inmediato con la base simulada).
     */
    public RespuestaApi consultarPlacaSincrono(String placa) {
        long inicio = System.currentTimeMillis();
        return consultarPlacaSimulada(placa.toUpperCase().trim(), inicio);
    }

    /**
     * Simula la respuesta de la API RUNT.
     * En produccion este metodo se elimina y se usa la respuesta HTTP real.
     */
    private RespuestaApi consultarPlacaSimulada(String placa, long inicio) {
        long tiempo = System.currentTimeMillis() - inicio;

        // Validar formato de placa colombiana (3 letras + 3 numeros o 3 letras + 2 numeros + 1 letra)
        if (!placa.matches("^[A-Z]{3}\\d{2,3}[A-Z0-9]?$")) {
            return new RespuestaApi(false, null, "400",
                    "Formato de placa invalido. Use formato colombiano: ABC123", tiempo);
        }

        Vehiculo vehiculo = BASE_RUNT_SIMULADA.get(placa);

        if (vehiculo != null) {
            // Vehiculo encontrado en el RUNT
            return new RespuestaApi(true, vehiculo, "200",
                    "Vehiculo encontrado en el RUNT", tiempo);
        } else {
            // Placa no registrada — generar vehiculo generico (particular)
            Vehiculo nuevo = new Vehiculo(placa, TipoVehiculo.PARTICULAR.name(), "Propietario No Registrado", false);
            BASE_RUNT_SIMULADA.put(placa, nuevo);
            return new RespuestaApi(true, nuevo, "200",
                    "Vehiculo registrado dinamicamente (no encontrado en RUNT, asignado como PARTICULAR)", tiempo);
        }
    }
}
