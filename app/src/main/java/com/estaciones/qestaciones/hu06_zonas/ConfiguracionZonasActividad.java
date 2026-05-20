package com.estaciones.qestaciones.hu06_zonas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.modelo.ZonaRegulada;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para configurar zonas reguladas.
 * HU-06: Definir zonas por municipio con versionamiento.
 */
public class ConfiguracionZonasActividad extends AppCompatActivity {

    private GestorZonas gestorZonas;
    private TextInputEditText campoMunicipio;
    private TextInputEditText campoDepartamento;
    private TextInputEditText campoCodigoZona;
    private RecyclerView listaZonas;
    private ZonasAdaptador adaptador;
    private TextView textoSinZonas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_configuracion_zonas);

        gestorZonas = new GestorZonas(this);

        inicializarVistas();
        configurarEventos();
        cargarZonas();
    }

    private void inicializarVistas() {
        campoMunicipio = findViewById(R.id.campoMunicipio);
        campoDepartamento = findViewById(R.id.campoDepartamento);
        campoCodigoZona = findViewById(R.id.campoCodigoZona);
        listaZonas = findViewById(R.id.listaZonas);
        textoSinZonas = findViewById(R.id.textoSinZonas);

        listaZonas.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new ZonasAdaptador();
        listaZonas.setAdapter(adaptador);
    }

    private void configurarEventos() {
        MaterialButton botonGuardar = findViewById(R.id.botonGuardarZona);
        botonGuardar.setOnClickListener(v -> guardarZona());

        findViewById(R.id.botonVolverZonas).setOnClickListener(v -> finish());
    }

    private void guardarZona() {
        String municipio = campoMunicipio.getText() != null ?
                campoMunicipio.getText().toString() : "";
        String departamento = campoDepartamento.getText() != null ?
                campoDepartamento.getText().toString() : "";
        String codigoZona = campoCodigoZona.getText() != null ?
                campoCodigoZona.getText().toString() : "";

        GestorZonas.ResultadoZona resultado = gestorZonas.crearOActualizarZona(
                municipio, departamento, codigoZona, "admin");

        if (resultado.isExitoso()) {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_SHORT).show();
            limpiarFormulario();
            cargarZonas();
        } else {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_LONG).show();
        }
    }

    private void limpiarFormulario() {
        campoMunicipio.setText("");
        campoDepartamento.setText("");
        campoCodigoZona.setText("");
    }

    private void cargarZonas() {
        List<ZonaRegulada> zonas = gestorZonas.obtenerTodasLasZonas();
        adaptador.actualizarDatos(zonas);
        textoSinZonas.setVisibility(zonas.isEmpty() ? View.VISIBLE : View.GONE);
        listaZonas.setVisibility(zonas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ===== Adaptador RecyclerView =====

    private static class ZonasAdaptador extends RecyclerView.Adapter<ZonasAdaptador.ZonaViewHolder> {

        private List<ZonaRegulada> zonas = new ArrayList<>();
        private final SimpleDateFormat formatoFecha =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public void actualizarDatos(List<ZonaRegulada> nuevasZonas) {
            this.zonas = nuevasZonas;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ZonaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_zona, parent, false);
            return new ZonaViewHolder(vista);
        }

        @Override
        public void onBindViewHolder(@NonNull ZonaViewHolder holder, int position) {
            ZonaRegulada zona = zonas.get(position);
            holder.textoMunicipio.setText(zona.getMunicipio() + ", " + zona.getDepartamento());
            holder.textoCodigo.setText("Código: " + zona.getCodigoZona());
            holder.textoVersion.setText("Versión: " + zona.getVersion());
            holder.textoFecha.setText(formatoFecha.format(new Date(zona.getFechaCreacion())));
            holder.textoEstado.setText(zona.isActiva() ? "✅ Activa" : "⛔ Inactiva");
            holder.textoEstado.setTextColor(zona.isActiva() ?
                    holder.itemView.getContext().getResources().getColor(R.color.exito) :
                    holder.itemView.getContext().getResources().getColor(R.color.error));
        }

        @Override
        public int getItemCount() { return zonas.size(); }

        static class ZonaViewHolder extends RecyclerView.ViewHolder {
            TextView textoMunicipio, textoCodigo, textoVersion, textoFecha, textoEstado;

            ZonaViewHolder(@NonNull View itemView) {
                super(itemView);
                textoMunicipio = itemView.findViewById(R.id.textoMunicipioZona);
                textoCodigo = itemView.findViewById(R.id.textoCodigoZona);
                textoVersion = itemView.findViewById(R.id.textoVersionZona);
                textoFecha = itemView.findViewById(R.id.textoFechaZona);
                textoEstado = itemView.findViewById(R.id.textoEstadoZona);
            }
        }
    }
}
