package com.estaciones.qestaciones.red;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Lab Retrofit (HU-17) — singleton que arma la instancia de Retrofit y crea
 * el cliente de la API.
 *
 * BASE_URL apunta a JSONPlaceholder, un mock REST público que soporta GET y POST
 * sin autenticación. Si querés cambiar al backend Caso JPA local:
 *
 *   1. Asegurate que el Main.java de Caso JPA esté corriendo (puerto 8080).
 *   2. Cambia BASE_URL a: "http://10.0.2.2:8080/competitors/"
 *      (10.0.2.2 es la IP especial del emulador para alcanzar el host).
 *   3. Si el backend habla http (no https), agregá el atributo
 *      android:usesCleartextTraffic="true" al <application> del manifest, o
 *      configurá un Network Security Config que permita 10.0.2.2 en cleartext.
 *   4. Ajustá los endpoints en ApiQEstaciones para reflejar /get y /add.
 *
 * El HttpLoggingInterceptor en nivel BODY imprime los requests/responses
 * completos en Logcat (filtrar por "OkHttp"). Útil mientras desarrollás;
 * en una app productiva bajalo a NONE o BASIC para no leakear datos.
 */
public final class ClienteRetrofit {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/";

    private static volatile Retrofit instancia;

    private ClienteRetrofit() {}

    public static Retrofit obtener() {
        if (instancia == null) {
            synchronized (ClienteRetrofit.class) {
                if (instancia == null) {
                    HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
                    logger.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient http = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .addInterceptor(logger)
                            .build();

                    instancia = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(http)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return instancia;
    }

    /**
     * Atajo: devuelve directamente la implementación de la API. Equivalente a
     * {@code obtener().create(ApiQEstaciones.class)} pero más corto en los call sites.
     */
    public static ApiQEstaciones api() {
        return obtener().create(ApiQEstaciones.class);
    }
}
