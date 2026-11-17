package com.estaciones.qestaciones.hu01_autenticacion;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.estaciones.qestaciones.BuildConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Servidor de correo SMTP para envio real de codigos de verificacion 2FA (HU-02).
 * Utiliza JavaMail API con Gmail SMTP.
 *
 * Configuracion:
 * - Servidor: smtp.gmail.com
 * - Puerto: 587 (TLS)
 * - Requiere "Contrasena de aplicacion" en cuenta Gmail remitente
 */
public class ServidorCorreo {

    private static final String TAG = "ServidorCorreo";

    // CONFIGURACION SMTP - El correo y la contrasena se leen de local.properties
    // (smtp.correo y smtp.contrasena) a traves de BuildConfig; nunca se escriben en el codigo.
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String CORREO_REMITENTE = BuildConfig.SMTP_CORREO;
    // Usar "Contrasena de aplicacion" de Google (16 caracteres)
    // Se genera en: Google Account > Seguridad > Verificacion en 2 pasos > Contrasenas de apps
    private static final String CONTRASENA_APP = BuildConfig.SMTP_CONTRASENA;

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    public interface ResultadoEnvio {
        void onExito();
        void onError(String mensajeError);
    }

    /**
     * Envia un codigo de verificacion 2FA al correo del usuario.
     * Se ejecuta en un hilo secundario para no bloquear la UI.
     */
    public void enviarCodigoVerificacion(String correoDestino, String codigo, ResultadoEnvio callback) {
        ejecutor.execute(() -> {
            try {
                Properties propiedades = new Properties();
                propiedades.put("mail.smtp.auth", "true");
                propiedades.put("mail.smtp.starttls.enable", "true");
                propiedades.put("mail.smtp.host", SMTP_HOST);
                propiedades.put("mail.smtp.port", SMTP_PORT);
                propiedades.put("mail.smtp.ssl.trust", SMTP_HOST);
                propiedades.put("mail.smtp.connectiontimeout", "10000");
                propiedades.put("mail.smtp.timeout", "10000");

                Session sesion = Session.getInstance(propiedades, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(CORREO_REMITENTE, CONTRASENA_APP);
                    }
                });

                MimeMessage mensaje = new MimeMessage(sesion);
                mensaje.setFrom(new InternetAddress(CORREO_REMITENTE, "Q-Estaciones Seguridad"));
                mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(correoDestino));
                mensaje.setSubject("Codigo de Verificacion - Q-Estaciones de Servicio");
                mensaje.setContent(construirPlantillaCorreo(codigo), "text/html; charset=utf-8");

                Transport.send(mensaje);

                Log.i(TAG, "Correo enviado exitosamente a: " + correoDestino);
                handlerPrincipal.post(callback::onExito);

            } catch (MessagingException e) {
                Log.e(TAG, "Error SMTP: " + e.getMessage(), e);
                handlerPrincipal.post(() -> callback.onError("Error SMTP: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error inesperado: " + e.getMessage(), e);
                handlerPrincipal.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    /**
     * Verifica si la configuracion SMTP esta lista para usarse.
     */
    public static boolean estaConfigurado() {
        return !CORREO_REMITENTE.isEmpty() && !CONTRASENA_APP.isEmpty();
    }

    /**
     * Envia una factura de venta/compra al correo del cliente.
     * El contenido plano (líneas con \n) se convierte a HTML sencillo.
     */
    public void enviarFactura(String correoDestino, String asunto, String contenidoPlano,
                               ResultadoEnvio callback) {
        ejecutor.execute(() -> {
            try {
                Properties propiedades = new Properties();
                propiedades.put("mail.smtp.auth", "true");
                propiedades.put("mail.smtp.starttls.enable", "true");
                propiedades.put("mail.smtp.host", SMTP_HOST);
                propiedades.put("mail.smtp.port", SMTP_PORT);
                propiedades.put("mail.smtp.ssl.trust", SMTP_HOST);
                propiedades.put("mail.smtp.connectiontimeout", "10000");
                propiedades.put("mail.smtp.timeout", "10000");

                Session sesion = Session.getInstance(propiedades, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(CORREO_REMITENTE, CONTRASENA_APP);
                    }
                });

                MimeMessage mensaje = new MimeMessage(sesion);
                mensaje.setFrom(new InternetAddress(CORREO_REMITENTE, "Q-Estaciones Facturación"));
                mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(correoDestino));
                mensaje.setSubject(asunto);
                mensaje.setContent(construirPlantillaFactura(contenidoPlano), "text/html; charset=utf-8");

                Transport.send(mensaje);

                Log.i(TAG, "Factura enviada a: " + correoDestino);
                handlerPrincipal.post(callback::onExito);

            } catch (MessagingException e) {
                Log.e(TAG, "Error SMTP factura: " + e.getMessage(), e);
                handlerPrincipal.post(() -> callback.onError("Error SMTP: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error inesperado factura: " + e.getMessage(), e);
                handlerPrincipal.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    private String construirPlantillaFactura(String contenidoPlano) {
        String contenidoHtml = contenidoPlano
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;'>"
                + "<div style='max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;"
                + "overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1);'>"
                + "<div style='background:#1565C0;padding:20px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:20px;'>Q-Estaciones de Servicio</h1>"
                + "<p style='color:#ffffff;margin:4px 0 0;opacity:.85;font-size:13px;'>Factura electrónica</p>"
                + "</div>"
                + "<div style='padding:24px;'>"
                + "<pre style='font-family:monospace;font-size:13px;background:#f9f9f9;"
                + "padding:16px;border-radius:8px;white-space:pre-wrap;margin:0;'>"
                + contenidoHtml
                + "</pre>"
                + "</div>"
                + "<div style='background:#f9f9f9;padding:12px;text-align:center;"
                + "font-size:11px;color:#999;border-top:1px solid #eee;'>"
                + "Este correo es una copia electrónica de la factura de tu transacción."
                + "</div></div></body></html>";
    }

    /**
     * Construye la plantilla HTML del correo de verificacion.
     */
    private String construirPlantillaCorreo(String codigo) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;'>"
                + "<div style='max-width:480px;margin:0 auto;background:#ffffff;border-radius:12px;"
                + "overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1);'>"
                + "<div style='background:#1565C0;padding:24px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:22px;'>Q-Estaciones de Servicio</h1>"
                + "</div>"
                + "<div style='padding:32px 24px;text-align:center;'>"
                + "<p style='font-size:16px;color:#333;'>Tu codigo de verificacion es:</p>"
                + "<div style='background:#E3F2FD;border-radius:8px;padding:20px;margin:16px 0;"
                + "font-size:36px;font-weight:bold;letter-spacing:8px;color:#1565C0;'>"
                + codigo
                + "</div>"
                + "<p style='font-size:13px;color:#777;margin-top:24px;'>"
                + "Este codigo expira en 10 minutos.<br>"
                + "Si no solicitaste este codigo, ignora este correo.</p>"
                + "</div>"
                + "<div style='background:#f9f9f9;padding:16px;text-align:center;"
                + "font-size:11px;color:#999;border-top:1px solid #eee;'>"
                + "Q-Estaciones de Servicio - Sistema de Seguridad 2FA"
                + "</div></div></body></html>";
    }
}
