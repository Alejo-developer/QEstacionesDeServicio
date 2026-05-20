package com.estaciones.qestaciones.hu09_entregas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.hu18_servicios.NotificadorEventosServicio;
import com.estaciones.qestaciones.modelo.RegistroEntrega;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para registrar y consultar entregas de combustible.
 * HU-09: Registro con trazabilidad completa.
 * HU-14: Historial filtrable por estacion, combustible y rango de fechas.
 */
public class RegistroEntregaActividad extends AppCompatActivity {

    private GestorEntregas gestorEntregas;
    private TextInputEditText campoEstacion, campoVehiculo, campoPlaca, campoVolumen, campoObservaciones;
    private Spinner spinnerCombustible;
    private RecyclerView listaEntregas;
    private EntregasAdaptador adaptador;
    private TextView textoSinEntregas;

    // HU-14: filtros
    private TextInputEditText campoFiltroEstacion;
    private Spinner spinnerFiltroCombustible;
    private MaterialButton botonFiltroDesde, botonFiltroHasta;
    private TextView textoContadorEntregas;
    private long filtroDesdeMs = 0;
    private long filtroHastaMs = Long.MAX_VALUE;
    private final SimpleDateFormat fmtBoton = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_registro_entrega);

        gestorEntregas = new GestorEntregas(this);

        inicializarVistas();
        configurarEventos();
        cargarEntregas();
    }

    private void inicializarVistas() {
        campoEstacion      = findViewById(R.id.campoEstacionDestino);
        campoVehiculo      = findViewById(R.id.campoVehiculoTransporte);
        campoPlaca         = findViewById(R.id.campoPlacaEntrega);
        campoVolumen       = findViewById(R.id.campoVolumen);
        spinnerCombustible = findViewById(R.id.spinnerCombustible);
        campoObservaciones = findViewById(R.id.campoObservaciones);
        listaEntregas      = findViewById(R.id.listaEntregas);
        textoSinEntregas   = findViewById(R.id.textoSinEntregas);

        String[] tiposCombustible = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};
        spinnerCombustible.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tiposCombustible));

        // HU-14: componentes de filtro
        campoFiltroEstacion     = findViewById(R.id.campoFiltroEstacion);
        spinnerFiltroCombustible = findViewById(R.id.spinnerFiltroCombustible);
        botonFiltroDesde        = findViewById(R.id.botonFiltroDesde);
        botonFiltroHasta        = findViewById(R.id.botonFiltroHasta);
        textoContadorEntregas   = findViewById(R.id.textoContadorEntregas);

        String[] opcionesFiltro = {"Todos", "ACPM", "Gasolina Corriente", "Gasolina Extra"};
        spinnerFiltroCombustible.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opcionesFiltro));

        listaEntregas.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new EntregasAdaptador();
        listaEntregas.setAdapter(adaptador);
    }

    private void configurarEventos() {
        findViewById(R.id.botonRegistrarEntrega).setOnClickListener(v -> registrarEntrega());
        findViewById(R.id.botonVolverEntregas).setOnClickListener(v -> finish());

        // HU-14: filtros
        botonFiltroDesde.setOnClickListener(v -> mostrarDatePicker(true));
        botonFiltroHasta.setOnClickListener(v -> mostrarDatePicker(false));
        findViewById(R.id.botonAplicarFiltro).setOnClickListener(v -> aplicarFiltros());
        findViewById(R.id.botonLimpiarFiltroEntrega).setOnClickListener(v -> limpiarFiltros());
    }

    private void registrarEntrega() {
        String estacion    = texto(campoEstacion);
        String vehiculo    = texto(campoVehiculo);
        String placa       = texto(campoPlaca);
        String volumen     = texto(campoVolumen);
        String combustible = spinnerCombustible.getSelectedItem().toString();
        String obs         = texto(campoObservaciones);

        GestorEntregas.ResultadoEntrega resultado =
                gestorEntregas.registrarEntrega(estacion, vehiculo, placa, volumen,
                        combustible, obs, "distribuidor");

        if (resultado.isExitoso()) {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_SHORT).show();
            dispararNotificacionEntrega(estacion, volumen, combustible);
            limpiarFormulario();
            cargarEntregas();
        } else {
            Toast.makeText(this, resultado.getMensaje(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * HU-18: lanza el started service que, tras un retardo, muestra una notificación
     * informando que la estación destino recibió combustible. En producción esto
     * iría dirigido al dispositivo del operador de la estación; aquí, como demo
     * del lab, se emite localmente.
     */
    private void dispararNotificacionEntrega(String estacion, String volumen, String combustible) {
        Intent svc = new Intent(this, NotificadorEventosServicio.class);
        svc.putExtra(NotificadorEventosServicio.EXTRA_TITULO,
                "⛽ Entrega de combustible recibida");
        svc.putExtra(NotificadorEventosServicio.EXTRA_MENSAJE,
                "Llegaron " + volumen + " L de " + combustible
                        + " para " + estacion + ". Verifique el inventario.");
        startService(svc);
    }

    private void limpiarFormulario() {
        campoEstacion.setText("");
        campoVehiculo.setText("");
        campoPlaca.setText("");
        campoVolumen.setText("");
        campoObservaciones.setText("");
        spinnerCombustible.setSelection(0);
    }

    private void cargarEntregas() {
        mostrarLista(gestorEntregas.obtenerTodasLasEntregas());
    }

    // ── HU-14 ──

    private void mostrarDatePicker(boolean esDesde) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, day);
            if (esDesde) {
                sel.set(Calendar.HOUR_OF_DAY, 0);
                sel.set(Calendar.MINUTE, 0);
                sel.set(Calendar.SECOND, 0);
                filtroDesdeMs = sel.getTimeInMillis();
                botonFiltroDesde.setText("Desde: " + fmtBoton.format(sel.getTime()));
            } else {
                sel.set(Calendar.HOUR_OF_DAY, 23);
                sel.set(Calendar.MINUTE, 59);
                sel.set(Calendar.SECOND, 59);
                filtroHastaMs = sel.getTimeInMillis();
                botonFiltroHasta.setText("Hasta: " + fmtBoton.format(sel.getTime()));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void aplicarFiltros() {
        String estacion = campoFiltroEstacion.getText() != null ?
                campoFiltroEstacion.getText().toString().trim() : "";
        String selCombustible = spinnerFiltroCombustible.getSelectedItem().toString();
        String combustible = selCombustible.equals("Todos") ? "" : selCombustible;

        long desde = filtroDesdeMs > 0 ? filtroDesdeMs : 0;
        long hasta = filtroHastaMs < Long.MAX_VALUE ? filtroHastaMs : Long.MAX_VALUE;

        List<RegistroEntrega> resultado =
                gestorEntregas.obtenerConFiltros(estacion, combustible, desde, hasta);
        mostrarLista(resultado);
        Toast.makeText(this, resultado.size() + " entregas encontradas", Toast.LENGTH_SHORT).show();
    }

    private void limpiarFiltros() {
        campoFiltroEstacion.setText("");
        spinnerFiltroCombustible.setSelection(0);
        filtroDesdeMs = 0;
        filtroHastaMs = Long.MAX_VALUE;
        botonFiltroDesde.setText(getString(R.string.desde));
        botonFiltroHasta.setText(getString(R.string.hasta));
        cargarEntregas();
        Toast.makeText(this, "Filtros eliminados", Toast.LENGTH_SHORT).show();
    }

    private void mostrarLista(List<RegistroEntrega> entregas) {
        adaptador.actualizarDatos(entregas);
        textoSinEntregas.setVisibility(entregas.isEmpty() ? View.VISIBLE : View.GONE);
        listaEntregas.setVisibility(entregas.isEmpty() ? View.GONE : View.VISIBLE);
        if (textoContadorEntregas != null) {
            int total = gestorEntregas.obtenerTodasLasEntregas().size();
            textoContadorEntregas.setText(
                    "Mostrando " + entregas.size() + " de " + total + " entregas");
            textoContadorEntregas.setVisibility(View.VISIBLE);
        }
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString() : "";
    }

    // ── Adaptador ──

    private static class EntregasAdaptador extends RecyclerView.Adapter<EntregasAdaptador.EntregaViewHolder> {

        private List<RegistroEntrega> entregas = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public void actualizarDatos(List<RegistroEntrega> nuevas) {
            this.entregas = nuevas;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public EntregaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_entrega, parent, false);
            return new EntregaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EntregaViewHolder h, int position) {
            RegistroEntrega e = entregas.get(position);
            h.textoEstacion.setText("📍 " + e.getEstacionDestino());
            h.textoVehiculo.setText("🚛 " + e.getVehiculoTransporte() + " | " + e.getPlacaVehiculo());
            h.textoVolumen.setText(String.format(Locale.getDefault(),
                    "⛽ %.0f litros — %s", e.getVolumenLitros(), e.getTipoCombustible()));
            h.textoFecha.setText(fmt.format(new Date(e.getFechaEntrega())));
            if (e.getObservaciones() != null && !e.getObservaciones().isEmpty()) {
                h.textoObservaciones.setVisibility(View.VISIBLE);
                h.textoObservaciones.setText("📝 " + e.getObservaciones());
            } else {
                h.textoObservaciones.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return entregas.size(); }

        static class EntregaViewHolder extends RecyclerView.ViewHolder {
            TextView textoEstacion, textoVehiculo, textoVolumen, textoFecha, textoObservaciones;
            EntregaViewHolder(@NonNull View v) {
                super(v);
                textoEstacion      = v.findViewById(R.id.textoEstacionEntrega);
                textoVehiculo      = v.findViewById(R.id.textoVehiculoEntrega);
                textoVolumen       = v.findViewById(R.id.textoVolumenEntrega);
                textoFecha         = v.findViewById(R.id.textoFechaEntrega);
                textoObservaciones = v.findViewById(R.id.textoObservacionesEntrega);
            }
        }
    }
}
