package com.estaciones.qestaciones.red;

import com.google.gson.annotations.SerializedName;

/**
 * Lab Retrofit (HU-17) — DTO de un boletín normativo expedido por el regulador
 * nacional (autoridad simulada por el backend REST).
 *
 * El backend actual es JSONPlaceholder y mapeamos su entidad /posts a nuestro
 * concepto de "boletín normativo":
 *   - userId  → idRegulador
 *   - id      → id
 *   - title   → titulo
 *   - body    → contenido
 *
 * Si en el futuro apuntás a tu propio backend (por ejemplo Caso JPA o un mock
 * propio que sí use claves en español), simplemente cambia los nombres en
 * @SerializedName para que coincidan — los campos Java se quedan igual.
 */
public class BoletinNormativoDto {

    @SerializedName("id")
    private Integer id;

    @SerializedName("userId")
    private Integer idRegulador;

    @SerializedName("title")
    private String titulo;

    @SerializedName("body")
    private String contenido;

    public BoletinNormativoDto() {}

    public BoletinNormativoDto(Integer idRegulador, String titulo, String contenido) {
        this.idRegulador = idRegulador;
        this.titulo = titulo;
        this.contenido = contenido;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdRegulador() { return idRegulador; }
    public void setIdRegulador(Integer idRegulador) { this.idRegulador = idRegulador; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}
