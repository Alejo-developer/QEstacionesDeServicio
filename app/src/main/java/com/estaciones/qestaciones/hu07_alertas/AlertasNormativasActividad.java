package com.estaciones.qestaciones.hu07_alertas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.ReglaPrecios;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad de historial de alertas normativas para la Estacion de Servicio (HU-07).
 * Muestra en modo lectura todos los cambios de precios ACPM publicados por el Administrador,
 * ordenados del mas reciente al mas antiguo.
 */
public class AlertasNormativasActividad extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_alertas_normativas);

        RecyclerView lista = findViewById(R.id.listaAlertas);
        TextView textoVacio = findViewById(R.id.textoSinAlertas);

        lista.setLayoutManager(new LinearLayoutManager(this));

        List<ReglaPrecios> cambios = BaseDatosEstaciones.obtenerInstancia(this)
                .reglaPreciosDao().obtenerHistorial();

        if (cambios.isEmpty()) {
            textoVacio.setVisibility(View.VISIBLE);
            lista.setVisibility(View.GONE);
        } else {
            textoVacio.setVisibility(View.GONE);
            lista.setVisibility(View.VISIBLE);
            lista.setAdapter(new AlertasAdaptador(cambios));
        }

        findViewById(R.id.botonVolverAlertas).setOnClickListener(v -> finish());
    }

    // ── Adaptador ──

    private static class AlertasAdaptador extends RecyclerView.Adapter<AlertasAdaptador.AlertaViewHolder> {

        private final List<ReglaPrecios> lista;
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        AlertasAdaptador(List<ReglaPrecios> lista) {
            this.lista = lista != null ? lista : new ArrayList<>();
        }

        @NonNull
        @Override
        public AlertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alerta_normativa, parent, false);
            return new AlertaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AlertaViewHolder h, int position) {
            ReglaPrecios r = lista.get(position);
            h.textoDecreto.setText(r.getDecreto());
            h.textoZona.setText("Zona: " + r.getZona() + " | " + r.getTipoVehiculo());
            h.textoPrecios.setText(String.format(Locale.getDefault(),
                    "Base: $%,.0f  |  Subsidiado: $%,.0f", r.getPrecioBase(), r.getPrecioSubsidiado()));
            h.textoFecha.setText(fmt.format(new Date(r.getFechaCreacion())));
        }

        @Override
        public int getItemCount() { return lista.size(); }

        static class AlertaViewHolder extends RecyclerView.ViewHolder {
            TextView textoDecreto, textoZona, textoPrecios, textoFecha;
            AlertaViewHolder(View v) {
                super(v);
                textoDecreto = v.findViewById(R.id.textoAlertaDecreto);
                textoZona    = v.findViewById(R.id.textoAlertaZona);
                textoPrecios = v.findViewById(R.id.textoAlertaPrecios);
                textoFecha   = v.findViewById(R.id.textoAlertaFecha);
            }
        }
    }
}
