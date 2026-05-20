package com.estaciones.qestaciones.hu03_precios;

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
import com.estaciones.qestaciones.modelo.ReglaPrecios;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad que muestra el historial normativo de cambios de precios.
 * HU-03: Historial normativo para trazabilidad.
 */
public class HistorialNormativoActividad extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_historial_normativo);

        // Botón volver
        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());

        cargarHistorial();
    }

    private void cargarHistorial() {
        GestorPrecios gestorPrecios = new GestorPrecios(this);
        List<ReglaPrecios> historial = gestorPrecios.obtenerHistorialNormativo();

        RecyclerView listaHistorial = findViewById(R.id.listaHistorial);
        TextView textoSinHistorial = findViewById(R.id.textoSinHistorial);

        if (historial.isEmpty()) {
            textoSinHistorial.setVisibility(View.VISIBLE);
            listaHistorial.setVisibility(View.GONE);
        } else {
            textoSinHistorial.setVisibility(View.GONE);
            listaHistorial.setVisibility(View.VISIBLE);
            listaHistorial.setLayoutManager(new LinearLayoutManager(this));
            listaHistorial.setAdapter(new AdaptadorHistorial(historial));
        }
    }

    // ==================== Adaptador RecyclerView ====================

    private static class AdaptadorHistorial extends RecyclerView.Adapter<AdaptadorHistorial.VistaHistorial> {

        private final List<ReglaPrecios> historial;
        private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        AdaptadorHistorial(List<ReglaPrecios> historial) {
            this.historial = historial;
        }

        @NonNull
        @Override
        public VistaHistorial onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_historial, parent, false);
            return new VistaHistorial(vista);
        }

        @Override
        public void onBindViewHolder(@NonNull VistaHistorial holder, int position) {
            ReglaPrecios regla = historial.get(position);
            holder.textoDecreto.setText(regla.getDecreto());
            holder.textoFecha.setText(formatoFecha.format(new Date(regla.getFechaCreacion())));

            String detalle = String.format(Locale.getDefault(),
                    "Zona: %s | Vehículo: %s | Base: $%,.0f | Subsidiado: $%,.0f | Subsidio: %s",
                    regla.getZona(),
                    regla.getTipoVehiculo(),
                    regla.getPrecioBase(),
                    regla.getPrecioSubsidiado(),
                    regla.isAplicaSubsidio() ? "Sí" : "No");
            holder.textoDetalle.setText(detalle);
        }

        @Override
        public int getItemCount() {
            return historial.size();
        }

        static class VistaHistorial extends RecyclerView.ViewHolder {
            TextView textoDecreto, textoFecha, textoDetalle;

            VistaHistorial(@NonNull View itemView) {
                super(itemView);
                textoDecreto = itemView.findViewById(R.id.textoDecreto);
                textoFecha = itemView.findViewById(R.id.textoFecha);
                textoDetalle = itemView.findViewById(R.id.textoDetalle);
            }
        }
    }
}
