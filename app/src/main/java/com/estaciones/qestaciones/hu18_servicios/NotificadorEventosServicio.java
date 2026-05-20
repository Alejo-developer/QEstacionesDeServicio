package com.estaciones.qestaciones.hu18_servicios;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.estaciones.qestaciones.hu01_autenticacion.InicioSesionActividad;

/**
 * HU-18 — Lab Started Services.
 *
 * Servicio iniciado (started service) basado en IntentService. Una actividad
 * lo invoca con startService(intent) pasando título, mensaje y, opcionalmente,
 * un retardo y la actividad destino que se abre al tocar la notificación.
 *
 * El servicio espera el retardo indicado en un hilo en segundo plano y luego
 * emite la notificación a través de NotificationCompat. Cumple con el patrón
 * del laboratorio: extiende IntentService, implementa onHandleIntent() con
 * synchronized + wait(), registra la actividad en logcat con Log.v() y dispara
 * la notificación con un canal propio.
 *
 * Casos de uso conectados:
 *  - HU-09 (entregas): al registrar una entrega de combustible, se notifica a
 *    la estación destino que llegó la recarga.
 *  - HU-05 (subsidios): al aprobarse un subsidio, se notifica al cliente.
 *
 * Nota: IntentService está marcada como deprecated desde API 30, pero la guía
 * de la asignatura la usa explícitamente y sigue funcionando en compileSdk 34.
 */
public class NotificadorEventosServicio extends IntentService {

    // Llaves de extras que la actividad llamadora puede pasar
    public static final String EXTRA_TITULO       = "titulo";
    public static final String EXTRA_MENSAJE      = "mensaje";
    public static final String EXTRA_DELAY_MS     = "delayMs";
    public static final String EXTRA_DESTINO_FQCN = "destinoFqcn";
    public static final String EXTRA_NOTIF_ID     = "notifId";

    private static final String TAG = "NotificadorEventosSvc";
    private static final String CANAL_ID = "canal_eventos_proyecto";
    private static final long DELAY_POR_DEFECTO_MS = 5_000L;

    public NotificadorEventosServicio() {
        super("NotificadorEventosServicio");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registrarCanal();
    }

    /** Canal requerido para mostrar notificaciones en Android O+ (API 26+). */
    private void registrarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Eventos del proyecto",
                    NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription(
                    "Avisos diferidos del sistema: entregas, subsidios y otros eventos.");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    /**
     * Se ejecuta en un hilo en background por cada Intent recibido.
     * Sigue el patrón del lab: synchronized + wait() para diferir, luego notificación.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;

        String titulo  = intent.getStringExtra(EXTRA_TITULO);
        String mensaje = intent.getStringExtra(EXTRA_MENSAJE);
        long delayMs   = intent.getLongExtra(EXTRA_DELAY_MS, DELAY_POR_DEFECTO_MS);
        String destinoFqcn = intent.getStringExtra(EXTRA_DESTINO_FQCN);
        int notifId    = intent.getIntExtra(EXTRA_NOTIF_ID,
                (String.valueOf(titulo) + "|" + System.currentTimeMillis()).hashCode());

        if (titulo == null)  titulo  = "Aviso del sistema";
        if (mensaje == null) mensaje = "";

        Log.v(TAG, "Recibida solicitud: \"" + titulo + "\" — espera " + delayMs + " ms");

        synchronized (this) {
            try {
                if (delayMs > 0) wait(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Espera interrumpida", e);
            }
        }

        Log.v(TAG, "Disparando notificación: " + titulo + " — " + mensaje);
        emitirNotificacion(titulo, mensaje, destinoFqcn, notifId);
    }

    private void emitirNotificacion(String titulo, String mensaje,
                                    String destinoFqcn, int notifId) {
        Intent destinoIntent = construirIntentDestino(destinoFqcn);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notifId,
                destinoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 800})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        try {
            nm.notify(notifId, builder.build());
        } catch (SecurityException ignorado) {
            // POST_NOTIFICATIONS no concedido (Android 13+). Se omite silenciosamente.
            Log.w(TAG, "Sin permiso POST_NOTIFICATIONS, notificación omitida.");
        }
    }

    /**
     * Construye el Intent que se disparará al tocar la notificación. Si la actividad
     * llamadora no especifica destino válido, se cae al InicioSesionActividad
     * (launcher), que ya sabe redirigir si el usuario tiene sesión activa.
     */
    private Intent construirIntentDestino(@Nullable String fqcn) {
        Class<?> claseDestino = InicioSesionActividad.class;
        if (fqcn != null && !fqcn.isEmpty()) {
            try {
                claseDestino = Class.forName(fqcn);
            } catch (ClassNotFoundException ignored) {
                // fallback al launcher
            }
        }
        Intent i = new Intent(this, claseDestino);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
}
