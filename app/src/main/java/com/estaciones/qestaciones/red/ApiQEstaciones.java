package com.estaciones.qestaciones.red;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Lab Retrofit (HU-17) — interface que describe los servicios REST que la app
 * consume. Retrofit genera automáticamente una implementación de esta interface
 * cuando se la pasa a Retrofit.create().
 *
 * Convención: cada método devuelve un Call<T> tipado con el modelo esperado.
 * Las llamadas se ejecutan con call.enqueue(callback) en un hilo de fondo y
 * el callback recibe la respuesta en el hilo principal — no hay que tocar
 * ningún Thread/Executor desde el lado del consumidor.
 *
 * Si querés mirar el contrato de JSONPlaceholder:
 *   https://jsonplaceholder.typicode.com/guide/
 */
public interface ApiQEstaciones {

    /**
     * GET /posts — trae todos los boletines normativos publicados.
     * En JSONPlaceholder devuelve 100 entradas. El backend nacional real
     * normalmente devolvería los últimos N ordenados por fecha.
     */
    @GET("posts")
    Call<List<BoletinNormativoDto>> obtenerBoletines();

    /**
     * GET /posts/{id} — trae un boletín por id. No se usa en la UI inicial pero
     * queda disponible por si querés mostrar el detalle al hacer tap.
     */
    @GET("posts/{id}")
    Call<BoletinNormativoDto> obtenerBoletinPorId(@Path("id") int id);

    /**
     * POST /posts — publica un nuevo boletín normativo. El cuerpo va serializado
     * a JSON por Gson. JSONPlaceholder no persiste de verdad (devuelve un id
     * fake) pero la respuesta sí incluye el objeto enviado con su nuevo id —
     * suficiente para demostrar el ciclo completo de Retrofit.
     */
    @POST("posts")
    Call<BoletinNormativoDto> publicarBoletin(@Body BoletinNormativoDto boletin);
}
