package com.estaciones.qestaciones.hu03_precios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Notificador de cambios normativos para la Estacion de Servicio (HU-07).
 * Emite una notificacion local cada vez que el Administrador crea o actualiza
 * una regla de precios ACPM, para que la estacion pueda ajustar sus reglas.
 */
public class NotificadorCambiosNormativos {

    private static final String CANAL_ID = "canal_cambios_normativos";
    private static final int ID_NOTIF_BASE = 4000;

    private final Context contexto;

    public NotificadorCambiosNormativos(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        registrarCanal();
    }

    private void registrarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Cambios Normativos",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Alertas de actualizaciones en precios ACPM y normativa");
            NotificationManager nm =
                    (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    /**
     * Notifica que se ha creado o actualizado una regla de precios.
     *
     * @param zona          Zona afectada
     * @param tipoVehiculo  Tipo de vehiculo
     * @param decreto       Numero de decreto o norma
     */
    public void notificarCambio(String zona, String tipoVehiculo, String decreto) {
        String titulo = "Nueva normativa: " + decreto;
        String texto = String.format(
                "Se actualizaron precios ACPM para la zona %s (%s). Revise el historial normativo.",
                zona, tipoVehiculo);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(contexto);
        try {
            nm.notify(ID_NOTIF_BASE + (zona + decreto).hashCode(), builder.build());
        } catch (SecurityException ignorado) {
            // Permiso POST_NOTIFICATIONS no concedido (Android 13+)
        }
    }
}
