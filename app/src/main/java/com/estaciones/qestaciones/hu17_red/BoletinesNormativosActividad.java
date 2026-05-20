package com.estaciones.qestaciones.hu17_red;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.red.ApiQEstaciones;
import com.estaciones.qestaciones.red.BoletinNormativoDto;
import com.estaciones.qestaciones.red.ClienteRetrofit;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lab Retrofit (HU-17) — pantalla que demuestra el consumo de un servicio REST
 * usando Retrofit. Disponible para REGULADOR y ADMINISTRADOR.
 *
 * Funcionalidades:
 *  - GET: trae todos los boletines normativos publicados (al entrar y al
 *    tocar "Refrescar"). Los muestra de más nuevo a más viejo (orden inverso).
 *  - POST: publica un nuevo boletín con título y contenido. Si el servidor
 *    responde 2xx, prependeamos el boletín a la lista local.
 *
 * Las llamadas son asíncronas (Call.enqueue + Callback). Retrofit corre la
 * petición en un hilo de fondo de OkHttp y el callback regresa en el hilo
 * principal — no hace falta runOnUiThread.
 */
public class BoletinesNormativosActividad extends AppCompatActivity {

    private ApiQEstaciones api;

    private TextView textoResumen;
    private TextView textoSinBoletines;
    private ProgressBar cargando;
    private RecyclerView lista;
    private BoletinesAdaptador adaptador;

    private TextInputEditText campoTitulo;
    private TextInputEditText campoContenido;
    private MaterialButton botonPublicar;
    private MaterialButton botonRefrescar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_boletines_normativos);

        api = ClienteRetrofit.api();

        textoResumen = findViewById(R.id.textoResumenBoletines);
        textoSinBoletines = findViewById(R.id.textoSinBoletines);
        cargando = findViewById(R.id.cargandoBoletines);

        lista = findViewById(R.id.listaBoletines);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new BoletinesAdaptador();
        lista.setAdapter(adaptador);

        campoTitulo = findViewById(R.id.campoTituloBoletin);
        campoContenido = findViewById(R.id.campoContenidoBoletin);
        botonPublicar = findViewById(R.id.botonPublicarBoletin);
        botonRefrescar = findViewById(R.id.botonRefrescarBoletines);

        botonRefrescar.setOnClickListener(v -> cargarBoletines());
        botonPublicar.setOnClickListener(v -> publicarBoletin());
        findViewById(R.id.botonVolverBoletines).setOnClickListener(v -> finish());

        cargarBoletines();
    }

    /**
     * GET /posts vía Retrofit. Mientras la petición está en curso muestra
     * un ProgressBar y deshabilita el botón Refrescar para evitar reintentos
     * dobles. El response.body() ya viene como List<BoletinNormativoDto>
     * deserializado por Gson — no hay que parsear nada a mano.
     */
    private void cargarBoletines() {
        mostrarCargando(true);
        textoResumen.setText("Conectando con el servidor nacional…");

        api.obtenerBoletines().enqueue(new Callback<List<BoletinNormativoDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoletinNormativoDto>> call,
                                   @NonNull Response<List<BoletinNormativoDto>> response) {
                mostrarCargando(false);
                if (!response.isSuccessful() || response.body() == null) {
                    textoResumen.setText("Error " + response.code() + " al cargar boletines.");
                    textoSinBoletines.setVisibility(adaptador.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    return;
                }
                // Mostramos los más nuevos arriba (id mayor primero). JSONPlaceholder
                // los devuelve en orden ascendente.
                List<BoletinNormativoDto> ordenados = new ArrayList<>(response.body());
                Collections.reverse(ordenados);
                adaptador.actualizar(ordenados);
                textoResumen.setText(ordenados.size() + " boletines recibidos del servidor.");
                textoSinBoletines.setVisibility(ordenados.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<BoletinNormativoDto>> call,
                                  @NonNull Throwable t) {
                mostrarCargando(false);
                textoResumen.setText("Sin conexión: " + t.getMessage());
                Toast.makeText(BoletinesNormativosActividad.this,
                        "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * POST /posts vía Retrofit. Valida que los campos no estén vacíos, arma el
     * DTO y lo envía. Si el servidor responde 2xx, agrega el boletín al tope
     * de la lista local (UX optimista) y limpia los campos.
     */
    private void publicarBoletin() {
        String titulo = campoTitulo.getText() != null
                ? campoTitulo.getText().toString().trim() : "";
        String contenido = campoContenido.getText() != null
                ? campoContenido.getText().toString().trim() : "";

        if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(contenido)) {
            Toast.makeText(this, "Completa título y contenido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // idRegulador = 1 (usuario simulado para JSONPlaceholder; en un backend real
        // sería el id del REGULADOR logueado).
        BoletinNormativoDto nuevo = new BoletinNormativoDto(1, titulo, contenido);

        botonPublicar.setEnabled(false);
        botonPublicar.setText("Publicando…");

        api.publicarBoletin(nuevo).enqueue(new Callback<BoletinNormativoDto>() {
            @Override
            public void onResponse(@NonNull Call<BoletinNormativoDto> call,
                                   @NonNull Response<BoletinNormativoDto> response) {
                botonPublicar.setEnabled(true);
                botonPublicar.setText("📡  Publicar boletín");

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(BoletinesNormativosActividad.this,
                            "El servidor rechazó el boletín: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                BoletinNormativoDto publicado = response.body();
                adaptador.prepend(publicado);
                campoTitulo.setText("");
                campoContenido.setText("");
                textoSinBoletines.setVisibility(View.GONE);
                textoResumen.setText("✅ Boletín publicado con id " + publicado.getId()
                        + ". Total local: " + adaptador.getItemCount());
                Toast.makeText(BoletinesNormativosActividad.this,
                        "Boletín publicado.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<BoletinNormativoDto> call,
                                  @NonNull Throwable t) {
                botonPublicar.setEnabled(true);
                botonPublicar.setText("📡  Publicar boletín");
                Toast.makeText(BoletinesNormativosActividad.this,
                        "Error de red al publicar: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarCargando(boolean enCurso) {
        cargando.setVisibility(enCurso ? View.VISIBLE : View.GONE);
        botonRefrescar.setEnabled(!enCurso);
    }

    // ---------- Adaptador ----------

    private static class BoletinesAdaptador extends RecyclerView.Adapter<BoletinesAdaptador.VH> {
        private final List<BoletinNormativoDto> items = new ArrayList<>();

        void actualizar(List<BoletinNormativoDto> nuevos) {
            items.clear();
            items.addAll(nuevos);
            notifyDataSetChanged();
        }

        void prepend(BoletinNormativoDto b) {
            items.add(0, b);
            notifyItemInserted(0);
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_boletin_normativo, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            BoletinNormativoDto b = items.get(position);
            h.idTexto.setText(" #" + (b.getId() != null ? b.getId() : "—") + " ");
            h.regulador.setText("Regulador " + (b.getIdRegulador() != null ? b.getIdRegulador() : "—"));
            h.titulo.setText(b.getTitulo() != null ? b.getTitulo() : "(Sin título)");
            h.contenido.setText(b.getContenido() != null ? b.getContenido() : "");
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView idTexto, regulador, titulo, contenido;
            VH(View v) {
                super(v);
                idTexto = v.findViewById(R.id.textoIdBoletin);
                regulador = v.findViewById(R.id.textoReguladorBoletin);
                titulo = v.findViewById(R.id.textoTituloBoletin);
                contenido = v.findViewById(R.id.textoContenidoBoletin);
            }
        }
    }
}
