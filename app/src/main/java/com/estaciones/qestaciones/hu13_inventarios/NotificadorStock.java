package com.estaciones.qestaciones.hu13_inventarios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Notificador de stock bajo de combustible (HU-13 DOING).
 * Emite una notificacion local cuando el saldo cae por debajo del umbral definido.
 *
 * Umbral predeterminado: 500 litros.
 * Si el saldo queda por debajo de ese valor tras un movimiento SALIDA o AJUSTE,
 * se emite una notificacion de alta prioridad en la barra de estado del dispositivo.
 */
public class NotificadorStock {

    private static final String CANAL_ID = "canal_stock_bajo";
    private static final int ID_BASE_NOTIF = 3000;

    /** Umbral en litros: si el saldo cae por debajo se emite la alerta */
    public static final double UMBRAL_LITROS = 500.0;

    private final Context contexto;

    public NotificadorStock(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        registrarCanal();
    }

    private void registrarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Alertas de Stock",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription("Alertas de nivel bajo de combustible en estacion");
            NotificationManager nm =
                    (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    /**
     * Evalua el saldo actual y emite notificacion si esta por debajo del umbral.
     *
     * @param tipoCombustible Tipo de combustible (ACPM, Gasolina Corriente, etc.)
     * @param saldoActual     Saldo en litros tras el ultimo movimiento
     */
    public void verificarYNotificar(String tipoCombustible, double saldoActual) {
        if (saldoActual < UMBRAL_LITROS) {
            emitirAlerta(tipoCombustible, saldoActual);
        }
    }

    private void emitirAlerta(String combustible, double saldo) {
        String titulo = "Alerta: Stock Bajo - " + combustible;
        String texto = String.format(
                "Quedan solo %.0f litros de %s. Solicite recarga urgente.",
                saldo, combustible);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(contexto);
        try {
            // El ID unico evita duplicar notificaciones para el mismo combustible
            nm.notify(ID_BASE_NOTIF + Math.abs(combustible.hashCode()), builder.build());
        } catch (SecurityException ignorado) {
            // Permiso POST_NOTIFICATIONS no concedido (Android 13+). Se omite silenciosamente.
        }
    }
}
