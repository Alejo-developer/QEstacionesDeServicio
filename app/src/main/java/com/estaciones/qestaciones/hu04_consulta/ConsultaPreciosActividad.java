package com.estaciones.qestaciones.hu04_consulta;

import android.app.DatePickerDialog;
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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.hu03_precios.GestorPrecios;
import com.estaciones.qestaciones.modelo.ReglaPrecios;
import com.estaciones.qestaciones.modelo.TipoVehiculo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para consulta publica de precios (HU-04) e historico con filtros (HU-15).
 *
 * HU-15 DOING completado:
 * - Filtros avanzados por fechas (fecha desde / fecha hasta)
 * - Correccion: Todos los registros se preservan y se muestran en el historico.
 *   El problema anterior era que el historico no se actualizaba despues de consultar
 *   y solo mostraba una regla por zona+tipo. Ahora muestra TODAS las reglas
 *   ordenadas cronologicamente y se refresca automaticamente.
 */
public class ConsultaPreciosActividad extends AppCompatActivity {

    private TextInputEditText campoCiudad;
    private Spinner spinnerTipo;
    private CardView tarjetaResultado;
    private TextView textoZona, textoPrecio, textoDecreto;
    private RecyclerView listaHistorico;
    private GestorPrecios gestorPrecios;

    // HU-15: Filtros por fecha
    private MaterialButton botonFechaDesde, botonFechaHasta, botonFiltrar, botonLimpiarFiltro;
    private TextView textoContadorRegistros;
    private long fechaDesdeMs = 0;
    private long fechaHastaMs = Long.MAX_VALUE;
    private final SimpleDateFormat formatoBoton = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_consulta_precios);

        gestorPrecios = new GestorPrecios(this);
        inicializarVistas();
        cargarHistorico();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // HU-15 FIX: Recargar el historico cada vez que la pantalla vuelve a primer plano
        // para garantizar que los registros nuevos se muestren
        cargarHistorico();
    }

    private void inicializarVistas() {
        campoCiudad = findViewById(R.id.campoCiudadConsulta);
        spinnerTipo = findViewById(R.id.spinnerTipoConsulta);
        tarjetaResultado = findViewById(R.id.tarjetaPrecioActual);
        textoZona = findViewById(R.id.textoResultadoZona);
        textoPrecio = findViewById(R.id.textoPrecioActual);
        textoDecreto = findViewById(R.id.textoDecretoActual);
        listaHistorico = findViewById(R.id.listaHistoricoPrecios);

        // HU-15: Componentes de filtros
        botonFechaDesde = findViewById(R.id.botonFechaDesde);
        botonFechaHasta = findViewById(R.id.botonFechaHasta);
        botonFiltrar = findViewById(R.id.botonFiltrarHistorico);
        botonLimpiarFiltro = findViewById(R.id.botonLimpiarFiltro);
        textoContadorRegistros = findViewById(R.id.textoContadorRegistros);

        // Spinner tipos
        String[] tipos = {
            TipoVehiculo.PARTICULAR.getDescripcion(),
            TipoVehiculo.CARGA.getDescripcion(),
            TipoVehiculo.SERVICIO_PUBLICO.getDescripcion(),
            TipoVehiculo.MOTOCICLETA.getDescripcion()
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapter);

        findViewById(R.id.botonConsultarPrecio).setOnClickListener(v -> consultar());
        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());

        // HU-15: Eventos de filtros
        botonFechaDesde.setOnClickListener(v -> mostrarSelectorFecha(true));
        botonFechaHasta.setOnClickListener(v -> mostrarSelectorFecha(false));
        botonFiltrar.setOnClickListener(v -> aplicarFiltros());
        botonLimpiarFiltro.setOnClickListener(v -> limpiarFiltros());

        listaHistorico.setLayoutManager(new LinearLayoutManager(this));
    }

    private void consultar() {
        String ciudad = campoCiudad.getText() != null ? campoCiudad.getText().toString().trim() : "";
        if (ciudad.isEmpty()) {
            Toast.makeText(this, "Ingrese una ciudad o zona", Toast.LENGTH_SHORT).show();
            return;
        }

        TipoVehiculo[] tiposEnum = TipoVehiculo.values();
        String tipoStr = tiposEnum[spinnerTipo.getSelectedItemPosition()].name();

        ReglaPrecios regla = gestorPrecios.obtenerPrecioActual(ciudad, tipoStr);

        if (regla != null) {
            tarjetaResultado.setVisibility(View.VISIBLE);
            textoZona.setText("Precio en " + regla.getZona());
            double precioFinal = regla.getPrecioSubsidiado();
            textoPrecio.setText(String.format(Locale.getDefault(), "$%,.0f COP", precioFinal));
            textoDecreto.setText("Norma: " + regla.getDecreto());
        } else {
            tarjetaResultado.setVisibility(View.GONE);
            Toast.makeText(this, "No hay precios registrados para esta zona/vehiculo", Toast.LENGTH_LONG).show();
        }

        // HU-15 FIX: Recargar historico despues de cada consulta
        cargarHistorico();
    }

    /**
     * HU-15: Muestra el selector de fecha (DatePicker).
     */
    private void mostrarSelectorFecha(boolean esDesde) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar seleccionada = Calendar.getInstance();
            seleccionada.set(year, month, dayOfMonth);

            if (esDesde) {
                seleccionada.set(Calendar.HOUR_OF_DAY, 0);
                seleccionada.set(Calendar.MINUTE, 0);
                seleccionada.set(Calendar.SECOND, 0);
                fechaDesdeMs = seleccionada.getTimeInMillis();
                botonFechaDesde.setText("Desde: " + formatoBoton.format(seleccionada.getTime()));
            } else {
                seleccionada.set(Calendar.HOUR_OF_DAY, 23);
                seleccionada.set(Calendar.MINUTE, 59);
                seleccionada.set(Calendar.SECOND, 59);
                fechaHastaMs = seleccionada.getTimeInMillis();
                botonFechaHasta.setText("Hasta: " + formatoBoton.format(seleccionada.getTime()));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    /**
     * HU-15: Aplica los filtros de fecha al historico.
     */
    private void aplicarFiltros() {
        if (fechaDesdeMs == 0 && fechaHastaMs == Long.MAX_VALUE) {
            Toast.makeText(this, "Seleccione al menos una fecha para filtrar", Toast.LENGTH_SHORT).show();
            return;
        }

        long desde = fechaDesdeMs == 0 ? 0 : fechaDesdeMs;
        long hasta = fechaHastaMs == Long.MAX_VALUE ? System.currentTimeMillis() : fechaHastaMs;

        if (desde > hasta) {
            Toast.makeText(this, "La fecha 'Desde' no puede ser mayor que 'Hasta'", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ReglaPrecios> filtradas = gestorPrecios.obtenerHistorialPorFechas(desde, hasta);
        actualizarListaHistorico(filtradas);
        Toast.makeText(this, "Filtro aplicado: " + filtradas.size() + " registros encontrados", Toast.LENGTH_SHORT).show();
    }

    /**
     * HU-15: Limpia los filtros y muestra todos los registros.
     */
    private void limpiarFiltros() {
        fechaDesdeMs = 0;
        fechaHastaMs = Long.MAX_VALUE;
        botonFechaDesde.setText("Desde: --/--/----");
        botonFechaHasta.setText("Hasta: --/--/----");
        cargarHistorico();
        Toast.makeText(this, "Filtros eliminados", Toast.LENGTH_SHORT).show();
    }

    /**
     * HU-15 FIX: Carga TODOS los registros del historico.
     * Antes solo se cargaba una vez en onCreate y no se actualizaba.
     */
    private void cargarHistorico() {
        List<ReglaPrecios> historico = gestorPrecios.obtenerHistorialNormativo();
        actualizarListaHistorico(historico);
    }

    /**
     * Actualiza la lista del RecyclerView y el contador.
     */
    private void actualizarListaHistorico(List<ReglaPrecios> datos) {
        listaHistorico.setAdapter(new HistoricoAdaptador(datos));

        // HU-15: Mostrar contador de registros
        if (textoContadorRegistros != null) {
            int totalEnBD = gestorPrecios.contarReglas();
            textoContadorRegistros.setText(
                    "Mostrando " + datos.size() + " de " + totalEnBD + " registros totales");
            textoContadorRegistros.setVisibility(View.VISIBLE);
        }
    }

    // ── Adaptador del historico ──

    private static class HistoricoAdaptador extends RecyclerView.Adapter<HistoricoAdaptador.HistoricoViewHolder> {
        private final List<ReglaPrecios> lista;
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        HistoricoAdaptador(List<ReglaPrecios> lista) {
            this.lista = lista != null ? lista : new ArrayList<>();
        }

        @NonNull
        @Override
        public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
            return new HistoricoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoricoViewHolder h, int position) {
            ReglaPrecios r = lista.get(position);
            h.decreto.setText(r.getDecreto());
            h.fecha.setText(fmt.format(new Date(r.getFechaCreacion())));
            h.detalle.setText(String.format(Locale.getDefault(),
                "%s | %s\nBase: $%,.0f | Sub.: $%,.0f",
                r.getZona(), r.getTipoVehiculo(), r.getPrecioBase(), r.getPrecioSubsidiado()));
        }

        @Override
        public int getItemCount() { return lista.size(); }

        static class HistoricoViewHolder extends RecyclerView.ViewHolder {
            TextView decreto, fecha, detalle;
            HistoricoViewHolder(View v) {
                super(v);
                decreto = v.findViewById(R.id.textoDecreto);
                fecha = v.findViewById(R.id.textoFecha);
                detalle = v.findViewById(R.id.textoDetalle);
            }
        }
    }
}
