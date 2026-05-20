package com.estaciones.qestaciones.hu10_trazabilidad;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.estaciones.qestaciones.datos.AuditoriaDao;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.modelo.Auditoria;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HU-10: Consulta de trazabilidad de operaciones con filtros avanzados.
 * Actor: Autoridad Reguladora. Logs persistentes para auditoría legal.
 * Permite filtrar por fechas, usuario y módulo, y exportar como CSV.
 */
public class TrazabilidadActividad extends AppCompatActivity {

    private AuditoriaDao auditoriaDao;
    private TextInputEditText campoFechaDesde;
    private TextInputEditText campoFechaHasta;
    private TextInputEditText campoFiltroUsuario;
    private TextInputEditText campoFiltroModulo;
    private TextView textoContadorLogs;
    private TextView textoSinLogs;
    private RecyclerView listaLogs;
    private LogsAdaptador adaptador;

    private long fechaInicio;
    private long fechaFin;
    private List<Auditoria> logsActuales = new ArrayList<>();

    private final SimpleDateFormat fmtFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_trazabilidad);

        auditoriaDao = BaseDatosEstaciones.obtenerInstancia(this).auditoriaDao();

        // Rango por defecto: últimos 30 días
        Calendar cal = Calendar.getInstance();
        fechaFin = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        fechaInicio = cal.getTimeInMillis();

        inicializarVistas();
        configurarEventos();
        aplicarFiltros();
    }

    private void inicializarVistas() {
        campoFechaDesde = findViewById(R.id.campoFechaDesde);
        campoFechaHasta = findViewById(R.id.campoFechaHasta);
        campoFiltroUsuario = findViewById(R.id.campoFiltroUsuario);
        campoFiltroModulo = findViewById(R.id.campoFiltroModulo);
        textoContadorLogs = findViewById(R.id.textoContadorLogs);
        textoSinLogs = findViewById(R.id.textoSinLogs);
        listaLogs = findViewById(R.id.listaLogs);

        campoFechaDesde.setText(fmtFecha.format(new Date(fechaInicio)));
        campoFechaHasta.setText(fmtFecha.format(new Date(fechaFin)));

        listaLogs.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new LogsAdaptador();
        listaLogs.setAdapter(adaptador);
    }

    private void configurarEventos() {
        campoFechaDesde.setOnClickListener(v -> mostrarDatePicker(true));
        campoFechaHasta.setOnClickListener(v -> mostrarDatePicker(false));

        MaterialButton botonAplicar = findViewById(R.id.botonAplicarFiltros);
        botonAplicar.setOnClickListener(v -> aplicarFiltros());

        MaterialButton botonLimpiar = findViewById(R.id.botonLimpiarFiltros);
        botonLimpiar.setOnClickListener(v -> limpiarFiltros());

        MaterialButton botonExportar = findViewById(R.id.botonExportarCsv);
        botonExportar.setOnClickListener(v -> exportarCsv());

        findViewById(R.id.botonVolverTrazabilidad).setOnClickListener(v -> finish());
    }

    private void mostrarDatePicker(boolean esFechaInicio) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(esFechaInicio ? fechaInicio : fechaFin);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            if (esFechaInicio) {
                sel.set(Calendar.HOUR_OF_DAY, 0);
                sel.set(Calendar.MINUTE, 0);
                sel.set(Calendar.SECOND, 0);
                fechaInicio = sel.getTimeInMillis();
                campoFechaDesde.setText(String.format(Locale.getDefault(),
                        "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            } else {
                sel.set(Calendar.HOUR_OF_DAY, 23);
                sel.set(Calendar.MINUTE, 59);
                sel.set(Calendar.SECOND, 59);
                fechaFin = sel.getTimeInMillis();
                campoFechaHasta.setText(String.format(Locale.getDefault(),
                        "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void aplicarFiltros() {
        String usuarioFiltro = campoFiltroUsuario.getText() != null
                ? campoFiltroUsuario.getText().toString().trim() : "";
        String moduloFiltro = campoFiltroModulo.getText() != null
                ? campoFiltroModulo.getText().toString().trim() : "";

        logsActuales = auditoriaDao.obtenerFiltrado(usuarioFiltro, moduloFiltro, fechaInicio, fechaFin);

        textoContadorLogs.setText(String.format(Locale.getDefault(),
                "%d registros encontrados", logsActuales.size()));

        if (logsActuales.isEmpty()) {
            textoSinLogs.setVisibility(View.VISIBLE);
            listaLogs.setVisibility(View.GONE);
        } else {
            textoSinLogs.setVisibility(View.GONE);
            listaLogs.setVisibility(View.VISIBLE);
            adaptador.actualizarDatos(logsActuales);
        }
    }

    private void limpiarFiltros() {
        campoFiltroUsuario.setText("");
        campoFiltroModulo.setText("");

        Calendar cal = Calendar.getInstance();
        fechaFin = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        fechaInicio = cal.getTimeInMillis();

        campoFechaDesde.setText(fmtFecha.format(new Date(fechaInicio)));
        campoFechaHasta.setText(fmtFecha.format(new Date(fechaFin)));

        aplicarFiltros();
    }

    private void exportarCsv() {
        if (logsActuales.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat fmtCompleto = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        StringBuilder csv = new StringBuilder("\uFEFF"); // BOM para Excel
        csv.append("Fecha,Usuario,Accion,Modulo,Detalle\n");
        for (Auditoria a : logsActuales) {
            csv.append(fmtCompleto.format(new Date(a.getFecha()))).append(",")
               .append(escapar(a.getUsuario())).append(",")
               .append(escapar(a.getAccion())).append(",")
               .append(escapar(a.getModulo())).append(",")
               .append(escapar(a.getDetalle())).append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Trazabilidad de Operaciones — Q-Estaciones");
        intent.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(intent, "Exportar CSV"));
    }

    private String escapar(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    // ─── Adaptador ───────────────────────────────────────────────────────────

    private static class LogsAdaptador extends RecyclerView.Adapter<LogsAdaptador.VH> {

        private List<Auditoria> items = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());

        void actualizarDatos(List<Auditoria> nuevos) {
            this.items = nuevos;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auditoria, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Auditoria a = items.get(position);
            h.accion.setText(a.getAccion());
            h.usuario.setText("👤 " + a.getUsuario());
            h.modulo.setText(a.getModulo());
            h.detalle.setText(a.getDetalle());
            h.fecha.setText(fmt.format(new Date(a.getFecha())));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView accion, usuario, modulo, detalle, fecha;
            VH(View v) {
                super(v);
                accion = v.findViewById(R.id.textoAccion);
                usuario = v.findViewById(R.id.textoUsuario);
                modulo = v.findViewById(R.id.textoModulo);
                detalle = v.findViewById(R.id.textoDetalle);
                fecha = v.findViewById(R.id.textoFecha);
            }
        }
    }
}
