package com.estaciones.qestaciones.hu08_inventario;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estaciones.qestaciones.R;
import com.estaciones.qestaciones.datos.BaseDatosEstaciones;
import com.estaciones.qestaciones.datos.MovimientoInventarioDao;
import com.estaciones.qestaciones.modelo.MovimientoInventario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HU-08: Inventario en tiempo real de la estación.
 * ENTRADA: registra combustible recibido de un distribuidor.
 * SALIDA: redirige a VentaActividad (flujo completo con subsidio y factura).
 */
public class InventarioTiempoRealActividad extends AppCompatActivity {

    private static final String[] COMBUSTIBLES = {"ACPM", "Gasolina Corriente", "Gasolina Extra"};

    private MovimientoInventarioDao movimientoDao;
    private Spinner spinnerTipo;
    private Spinner spinnerCombustible;
    private TextInputEditText campoVolumen;
    private TextInputEditText campoObservacion;   // usado como "Distribuidor" en ENTRADA
    private TextInputLayout layoutObservacion;
    private TextView textoRecibido;
    private TextView textoVendido;
    private TextView textoSaldo;
    private TextView textoSinMovimientos;
    private RecyclerView listaMovimientos;
    private MovimientosAdaptador adaptador;
    private String combustibleSeleccionado = "ACPM";

    // Scope por empresa: inventario aislado por estación logueada
    private String empresaUsuario = "";
    private String rolUsuario = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inventario_tiempo_real);

        movimientoDao = BaseDatosEstaciones.obtenerInstancia(this).movimientoInventarioDao();

        String usuarioIntent = getIntent().getStringExtra("nombreUsuario");
        empresaUsuario = usuarioIntent != null ? usuarioIntent : "";
        String rolIntent = getIntent().getStringExtra("rol");
        rolUsuario = rolIntent != null ? rolIntent : "";

        inicializarVistas();
        configurarEventos();
        actualizarResumen();
    }

    private boolean tieneScope() { return !empresaUsuario.isEmpty(); }

    private void inicializarVistas() {
        spinnerTipo = findViewById(R.id.spinnerTipoMov08);
        spinnerCombustible = findViewById(R.id.spinnerCombustible08);
        campoVolumen = findViewById(R.id.campoVolumen08);
        campoObservacion = findViewById(R.id.campoObservacion08);
        layoutObservacion = (TextInputLayout) campoObservacion.getParent().getParent();
        textoRecibido = findViewById(R.id.textoRecibido);
        textoVendido = findViewById(R.id.textoVendido);
        textoSaldo = findViewById(R.id.textoSaldo08);
        textoSinMovimientos = findViewById(R.id.textoSinMovimientos08);
        listaMovimientos = findViewById(R.id.listaMovimientos08);

        // Solo ENTRADA aquí; SALIDA va a VentaActividad
        String[] tipos = {"ENTRADA", "SALIDA → Registrar Venta"};
        spinnerTipo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tipos));

        // Cambiar hint de observación según tipo seleccionado
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean esEntrada = position == 0;
                layoutObservacion.setHint(esEntrada ? "Distribuidor (quién entrega)" : "Referencia interna");
                campoVolumen.setEnabled(esEntrada);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> adapterComb = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, COMBUSTIBLES);
        spinnerCombustible.setAdapter(adapterComb);
        spinnerCombustible.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                combustibleSeleccionado = COMBUSTIBLES[position];
                actualizarResumen();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        listaMovimientos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MovimientosAdaptador();
        listaMovimientos.setAdapter(adaptador);
    }

    private void configurarEventos() {
        MaterialButton botonRegistrar = findViewById(R.id.botonRegistrar08);
        botonRegistrar.setOnClickListener(v -> {
            int tipoIdx = spinnerTipo.getSelectedItemPosition();
            if (tipoIdx == 0) {
                registrarEntrada();
            } else {
                abrirVenta();
            }
        });
        findViewById(R.id.botonVolver08).setOnClickListener(v -> finish());
    }

    private void registrarEntrada() {
        String volumenStr = campoVolumen.getText() != null ? campoVolumen.getText().toString().trim() : "";
        String distribuidor = campoObservacion.getText() != null
                ? campoObservacion.getText().toString().trim() : "";

        if (volumenStr.isEmpty()) {
            Toast.makeText(this, "Ingrese el volumen en litros", Toast.LENGTH_SHORT).show();
            return;
        }

        double volumen;
        try {
            volumen = Double.parseDouble(volumenStr);
            if (volumen <= 0) {
                Toast.makeText(this, "El volumen debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Volumen inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        double saldoAnterior = tieneScope()
                ? movimientoDao.obtenerSaldoPorEmpresa(combustibleSeleccionado, empresaUsuario)
                : movimientoDao.obtenerSaldoActual(combustibleSeleccionado);
        double saldoNuevo = saldoAnterior + volumen;

        MovimientoInventario mov = new MovimientoInventario();
        mov.setTipoMovimiento("ENTRADA");
        mov.setTipoCombustible(combustibleSeleccionado);
        mov.setVolumenLitros(volumen);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setMotivo(distribuidor.isEmpty() ? "Recepción de combustible" : "Entrega de: " + distribuidor);
        mov.setDistribuidorNombre(distribuidor.isEmpty() ? "Sin especificar" : distribuidor);
        mov.setResponsable(tieneScope() ? empresaUsuario : "estacion");
        mov.setEsAuditable(true);
        mov.setEmpresaUsuario(tieneScope() ? empresaUsuario : null);
        movimientoDao.insertar(mov);

        Toast.makeText(this, String.format(Locale.getDefault(),
                "📥 Entrada registrada: %.0f L — Saldo: %.0f L", volumen, saldoNuevo),
                Toast.LENGTH_SHORT).show();

        campoVolumen.setText("");
        campoObservacion.setText("");
        actualizarResumen();
    }

    private void abrirVenta() {
        Intent i = new Intent(this, VentaActividad.class);
        // Propagar el scope para que la venta descuente del inventario de esta empresa
        if (tieneScope()) {
            i.putExtra("nombreUsuario", empresaUsuario);
            i.putExtra("rol", rolUsuario);
        }
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarResumen(); // Refrescar después de volver de VentaActividad
    }

    private void actualizarResumen() {
        // Si hay scope, usamos la lista filtrada por empresa para todos los cálculos y la lista.
        List<MovimientoInventario> movimientos = tieneScope()
                ? movimientoDao.obtenerPorEmpresa(empresaUsuario)
                : movimientoDao.obtenerTodos();

        double recibido = 0, vendido = 0;
        for (MovimientoInventario m : movimientos) {
            if (!combustibleSeleccionado.equals(m.getTipoCombustible())) continue;
            if ("ENTRADA".equals(m.getTipoMovimiento())) recibido += m.getVolumenLitros();
            else if ("SALIDA".equals(m.getTipoMovimiento())) vendido += m.getVolumenLitros();
        }
        double saldo = tieneScope()
                ? movimientoDao.obtenerSaldoPorEmpresa(combustibleSeleccionado, empresaUsuario)
                : movimientoDao.obtenerSaldoActual(combustibleSeleccionado);

        textoRecibido.setText(String.format(Locale.getDefault(), "%.0f L", recibido));
        textoVendido.setText(String.format(Locale.getDefault(), "%.0f L", vendido));
        textoSaldo.setText(String.format(Locale.getDefault(), "%.0f L", saldo));

        if (movimientos.isEmpty()) {
            textoSinMovimientos.setVisibility(View.VISIBLE);
            listaMovimientos.setVisibility(View.GONE);
        } else {
            textoSinMovimientos.setVisibility(View.GONE);
            listaMovimientos.setVisibility(View.VISIBLE);
            adaptador.actualizarDatos(movimientos);
        }
    }

    // ─── Adaptador ───────────────────────────────────────────────────────────

    private static class MovimientosAdaptador extends RecyclerView.Adapter<MovimientosAdaptador.VH> {

        private List<MovimientoInventario> items = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        void actualizarDatos(List<MovimientoInventario> nuevos) {
            this.items = nuevos;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_movimiento, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MovimientoInventario m = items.get(position);
            String icono = m.getTipoMovimiento().equals("ENTRADA") ? "📥" : "📤";
            h.tipo.setText(icono + " " + m.getTipoMovimiento());
            h.volumen.setText(String.format(Locale.getDefault(), "%.0f L — %s",
                    m.getVolumenLitros(), m.getTipoCombustible()));
            h.saldos.setText(String.format(Locale.getDefault(), "Saldo: %.0f → %.0f L",
                    m.getSaldoAnterior(), m.getSaldoNuevo()));
            String motivo = m.getMotivo() != null ? m.getMotivo() : "";
            String dist = m.getDistribuidorNombre();
            if (dist != null && !dist.isEmpty() && !dist.equals("Sin especificar")) {
                motivo = "🚛 " + dist + (motivo.isEmpty() ? "" : " — " + motivo);
            }
            h.motivo.setText(motivo.isEmpty() ? "—" : motivo);
            h.fecha.setText(fmt.format(new Date(m.getFechaMovimiento())));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tipo, volumen, saldos, motivo, fecha;
            VH(View v) {
                super(v);
                tipo = v.findViewById(R.id.textoTipoMovimiento);
                volumen = v.findViewById(R.id.textoVolumenMov);
                saldos = v.findViewById(R.id.textoSaldos);
                motivo = v.findViewById(R.id.textoMotivoMov);
                fecha = v.findViewById(R.id.textoFechaMov);
            }
        }
    }
}
